package com.cqsongjin.file.fly.server;

import com.alibaba.fastjson.JSON;
import com.cqsongjin.file.fly.common.Global;
import com.cqsongjin.file.fly.constant.Config;
import com.cqsongjin.file.fly.controller.IndexController;
import com.cqsongjin.file.fly.message.ClientMessage;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

public class BroadcastServer implements Server{
    private static final Logger log = LoggerFactory.getLogger(BroadcastServer.class);

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(1, 3, 60L,TimeUnit.SECONDS, new LinkedBlockingQueue<>(), (r, e) -> {
        throw new RejectedExecutionException("Task " + r.toString() +
                " rejected from " +
                e.toString());
    });

    private IndexController indexController;
    private volatile boolean isStop = false;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(Config.BROADCAST_FRAME_SIZE);
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(Config.BROADCAST_FRAME_SIZE);

    public BroadcastServer(IndexController indexController) {
        this.indexController = indexController;
    }

    @Override
    public void startServer() {
        this.isStop = false;
        EXECUTOR_SERVICE.execute(() -> {
            try {
                DatagramChannel serverDatagramChannel = DatagramChannel.open();
                serverDatagramChannel.configureBlocking(false);
                serverDatagramChannel.bind(new InetSocketAddress(Config.BROADCAST_PORT));
                Selector selector = Selector.open();
                serverDatagramChannel.register(selector, SelectionKey.OP_READ);
                serverDatagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, Boolean.TRUE);
                //更新扫描到的客户端数据
                final Service<Void> service = new Service<>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<>() {
                            @Override
                            protected Void call() throws Exception {
                                try {
                                    sendBroadcast(serverDatagramChannel);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        };
                    }
                };
                service.progressProperty();
                service.valueProperty();
                service.start();
                while (true) {
                    selector.select(TimeUnit.SECONDS.toMillis(5));
                    final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    final Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isReadable()) {
                            final DatagramChannel channel = (DatagramChannel) selectionKey.channel();
                            handleRead(channel);
                        }
                        iterator.remove();
                    }
                    if (isStop) {
                        System.out.println("停止扫描client广播监听");
                        break;
                    }
                }
            } catch (IOException e) {
                log.error("执行广播任务出错：", e);
                throw new RuntimeException(e);
            }
        });
    }

    public void handleRead(DatagramChannel channel) throws IOException {
        channel.receive(this.readBuffer);
        if (this.readBuffer.position() < this.readBuffer.capacity()) {
            return;
        }
        this.readBuffer.flip();
        ClientMessage message = new ClientMessage();
        message.readBuffer(this.readBuffer);
        final ClientMessage.Client client = message.getClient();
        log.info("发现file fly 客户端：{}，IP地址：{}", client.getName(), client.getIpAddr());
        Platform.runLater(() -> indexController.updateReceiver(client.getIpAddr(), client.getName()));
        this.readBuffer.clear();

    }

    public void sendBroadcast(DatagramChannel datagramChannel) throws IOException {
        while (!this.isStop) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final InetAddress localHost = InetAddress.getLocalHost();
            String hostIp = Config.HOST_IP.isBlank() ? localHost.getHostAddress() : Config.HOST_IP;
            String hostName = Config.HOST_NAME.isBlank() ? localHost.getHostName() : Config.HOST_NAME;
            ClientMessage.Client client = new ClientMessage.Client(hostIp, hostName);
            ClientMessage message = new ClientMessage(client);
            message.writeBuffer(this.writeBuffer);
            this.writeBuffer.flip();
            log.info("发送广播数据 ->" + JSON.toJSONString(client));
            while (this.writeBuffer.hasRemaining()) {
                datagramChannel.send(this.writeBuffer, new InetSocketAddress("192.168.81.255", Config.BROADCAST_PORT));
            }
            this.writeBuffer.clear();
        }
    }

    @Override
    public void stopServer() {
        this.isStop = true;
    }
}
