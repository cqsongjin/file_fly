package com.cqsongjin.file.fly.server;

import com.cqsongjin.file.fly.APP;
import com.cqsongjin.file.fly.common.FileChannelHandler;
import com.cqsongjin.file.fly.constant.Config;
import com.cqsongjin.file.fly.controller.IndexController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class FileTransformServer implements Server{
    private static final Logger log = LoggerFactory.getLogger(FileTransformServer.class);
    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(1, 3, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), (r, e) -> {
        throw new RejectedExecutionException("Task " + r.toString() +
                " rejected from " +
                e.toString());
    });
    private final Map<String, FileChannelHandler> channelHandlerMap;
    private IndexController indexController;
    public FileTransformServer(IndexController indexController) {
        this.indexController = indexController;
        channelHandlerMap = new HashMap<>();
    }
    private volatile boolean isStop = false;

    @Override
    public void startServer() {
        this.isStop = false;
        System.out.println("start listener");
        EXECUTOR_SERVICE.execute(() -> {
            Selector selector = null;
            ServerSocketChannel ssc = null;
            try {
                selector = Selector.open();
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.bind(new InetSocketAddress(Config.TRANSFORM_PORT));
                ssc.register(selector, SelectionKey.OP_ACCEPT);
                while (true) {
                    System.out.println("do select");
                    if (!isStop) {
                        log.info("关闭TCP监听");
                        return;
                    }
                    selector.select();
                    final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    final Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey selectionKey = iterator.next();

                        if (selectionKey.isAcceptable()) {
                            final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                            final SocketChannel channel = serverSocketChannel.accept();
                            handleAccept(selectionKey, channel);
                        } else if (selectionKey.isReadable()) {
                            handleRead(selectionKey);
                        } else if (selectionKey.isWritable()) {
                            final SocketChannel channel = (SocketChannel) selectionKey.channel();
                            handleWrite(selectionKey, channel);
                        }
                        iterator.remove();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (selector != null) {
                    try {
                        selector.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (ssc != null) {
                    try {
                        ssc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }
    private void handleAccept(SelectionKey selectionKey, SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey selectionKey) throws IOException {
        final SocketChannel channel = (SocketChannel) selectionKey.channel();
        final String remoteAddress = channel.getRemoteAddress().toString();
        FileChannelHandler fileChannelHandler = channelHandlerMap.get(remoteAddress);
        if (fileChannelHandler != null) {
            fileChannelHandler.readChannel2File(selectionKey, channel);
        } else {
            fileChannelHandler = new FileChannelHandler(indexController.getOutput_dir_text().getText());
            fileChannelHandler.readChannel2File(selectionKey, channel);
            channelHandlerMap.put(remoteAddress, fileChannelHandler);
        }
    }

    private void handleWrite(SelectionKey selectionKey, SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
        if (buffer != null) {
            socketChannel.write(buffer);
            selectionKey.attach(null);
        }
    }

    @Override
    public void stopServer() {
        this.isStop = true;
    }
}
