package com.cqsongjin.file.fly.controller;

import com.cqsongjin.file.fly.APP;
import com.cqsongjin.file.fly.common.FileChannelHandler;
import com.cqsongjin.file.fly.constant.Config;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 自定义协议内容
 * 16+2+*，md5+length+data,length为-1则表示传输完成
 */
public class DemoController {
    @FXML
    public HBox file_box;
    @FXML
    public TextField output_dir_text;
    @FXML
    public ComboBox<Receiver> receiver_combobox;
    @FXML
    public ProgressBar process_line_bar;
    @FXML
    public TextField target_host_text;

    private Map<String, FileChannelHandler> channelHandlerMap;

    private final ObservableList<Receiver> list = FXCollections.observableArrayList();

    public void init() {
        channelHandlerMap = new HashMap<>();
        output_dir_text.setText(Config.OUTPUT_DIR);
        target_host_text.setText(Config.LAST_LINKED_IP);
        receiver_combobox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Receiver object) {
                return object.getName();
            }

            @Override
            public Receiver fromString(String string) {
                return null;
            }
        });
        receiver_combobox.setItems(list);
        file_box.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });
        file_box.setOnDragDropped(event -> {
            final Dragboard dragboard = event.getDragboard();
            final File targetFile = dragboard.getFiles().get(0);
            System.out.println(targetFile.getAbsolutePath());
            System.out.println(targetFile.getName());
            event.setDropCompleted(true);
            event.consume();
            try {
                doFileTransform(targetFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        startListenBroadcast();
        scanReceiver();
    }

    private Set<Receiver> scanReceiver() {
        return Set.of(new Receiver("111", "r111"), new Receiver("222", "r222"), new Receiver("333", "r333"));
    }

    private void startListenBroadcast() {
        System.out.println("start listener");
        APP.EXECUTOR_SERVICE.execute(() -> {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.bind(new InetSocketAddress(Config.PORT));
                ssc.register(selector, SelectionKey.OP_ACCEPT);
                while (true) {
                    System.out.println("do select");
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
            fileChannelHandler = new FileChannelHandler(output_dir_text.getText());
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

    private void doFileTransform(File targetFile) throws IOException {
        final String output_dirText = output_dir_text.getText();
        final String targetHost = target_host_text.getText();
        final Path path = Path.of(output_dirText, targetFile.getName());
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        boolean connect = socketChannel.connect(new InetSocketAddress(targetHost, Config.PORT));
        if (connect) {
            System.out.println("connected");
            sendFile(targetFile.toPath(), socketChannel);
            return;
        } else {
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                SocketChannel socketChannel1 = (SocketChannel) selectionKey.channel();
                if (selectionKey.isConnectable()) {
                    if (socketChannel1.finishConnect()) {
                        System.out.println("connected");
                        sendFile(targetFile.toPath(), socketChannel);
                        return;
                    } else {
                        System.out.println("connect failed");
                        System.exit(-1);
                    }
                }
                iterator.remove();
            }
        }
    }

    private void sendFile(Path path, SocketChannel socketChannel) throws IOException {
        FileChannel fileChannel = FileChannel.open(path);
        final String fileName = path.getFileName().toString();
        ByteBuffer buffer = ByteBuffer.allocate(FileChannelHandler.frameSize);
        buffer.put((byte) 0);
        buffer.put(new byte[16]);
        final byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(fileNameBytes.length);
        buffer.put(fileNameBytes);
        buffer.put(new byte[FileChannelHandler.frameSize - buffer.position()]);
        buffer.flip();

//        System.out.println("发送大小：" + buffer.remaining());
        int write = 0;
        while (buffer.hasRemaining()) {
            write += socketChannel.write(buffer);
            System.out.println("实际发送大小：" + write);
        }
        buffer.clear();
        int read = 0;
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        int i = 2;
        while (read > -1) {
            buffer.put((byte) 1);
            buffer.put(new byte[16]);
            buffer.putInt(0);
            int length = 0;
            while (buffer.position() < FileChannelHandler.frameSize) {
                read = fileChannel.read(buffer);
                if (read == -1) {
                    break;
                }
                length += read;
            }
            buffer.putInt(17, length);
            if (buffer.position() < FileChannelHandler.frameSize) {
                buffer.put(new byte[FileChannelHandler.frameSize - buffer.position()]);
            }
            buffer.flip();
            //降低一下传输速率，否则在本地测试过程中有奇怪问题
//            try {
//                TimeUnit.MILLISECONDS.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            System.out.println("第 " + i++ + "次发送");
            final byte[] array = buffer.array();
            for (int i1 = 0; i1 < 21; i1++) {
                System.out.print(array[i1] + ",");
            }
            System.out.println();
            System.out.println("发送大小：" + buffer.remaining() + "数据载荷：" + length);
            write = 0;
            while (buffer.hasRemaining()) {
//                try {
//                    TimeUnit.MILLISECONDS.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                write += socketChannel.write(buffer);
                System.out.println("实际发送大小：" + write);
            }
            buffer.clear();
        }
        buffer.put((byte) 2);
        buffer.put(new byte[16]);
        buffer.put(new byte[FileChannelHandler.frameSize - buffer.position()]);
        buffer.flip();
//        System.out.println("发送大小：" + buffer.remaining());
        while (buffer.hasRemaining()) {
            write += socketChannel.write(buffer);
            System.out.println("实际发送大小：" + write);
        }
        buffer.clear();
        System.out.println("send file finished");
//        socketChannel.close();
    }

    private static class Receiver {
        private String ipAddr;
        private String name;

        public Receiver(String ipAddr, String name) {
            this.ipAddr = ipAddr;
            this.name = name;
        }

        public String getIpAddr() {
            return ipAddr;
        }

        public void setIpAddr(String ipAddr) {
            this.ipAddr = ipAddr;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
