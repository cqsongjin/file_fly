package com.cqsongjin.file.fly.controller;

import com.cqsongjin.file.fly.APP;
import com.cqsongjin.file.fly.common.FileChannelHandler;
import com.cqsongjin.file.fly.constant.Config;
import com.cqsongjin.file.fly.server.BroadcastServer;
import com.cqsongjin.file.fly.server.FileTransformServer;
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
public class IndexController {
    @FXML
    private HBox file_box;
    @FXML
    private TextField output_dir_text;
    @FXML
    private ComboBox<Receiver> receiver_combobox;
    @FXML
    private ProgressBar process_line_bar;
    @FXML
    private TextField target_host_text;



    private final ObservableList<Receiver> list = FXCollections.observableArrayList();

    public void init() {
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

        BroadcastServer broadcastServer = new BroadcastServer(this);
        FileTransformServer fileTransformServer = new FileTransformServer(this);
        broadcastServer.startServer();
        fileTransformServer.startServer();
        scanReceiver();
    }

    private Set<Receiver> scanReceiver() {
        return Set.of(new Receiver("111", "r111"), new Receiver("222", "r222"), new Receiver("333", "r333"));
    }

    private void doFileTransform(File targetFile) throws IOException {
        final String targetHost = target_host_text.getText();
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        boolean connect = socketChannel.connect(new InetSocketAddress(targetHost, Config.TRANSFORM_PORT));
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
            System.out.println("第 " + i++ + "次发送");
            final byte[] array = buffer.array();
            for (int i1 = 0; i1 < 21; i1++) {
                System.out.print(array[i1] + ",");
            }
            System.out.println();
            System.out.println("发送大小：" + buffer.remaining() + "数据载荷：" + length);
            write = 0;
            while (buffer.hasRemaining()) {
                write += socketChannel.write(buffer);
                System.out.println("实际发送大小：" + write);
            }
            buffer.clear();
        }
        buffer.put((byte) 2);
        buffer.put(new byte[16]);
        buffer.put(new byte[FileChannelHandler.frameSize - buffer.position()]);
        buffer.flip();
        while (buffer.hasRemaining()) {
            write += socketChannel.write(buffer);
            System.out.println("实际发送大小：" + write);
        }
        buffer.clear();
        System.out.println("send file finished");
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

    public HBox getFile_box() {
        return file_box;
    }

    public TextField getOutput_dir_text() {
        return output_dir_text;
    }

    public ComboBox<Receiver> getReceiver_combobox() {
        return receiver_combobox;
    }

    public ProgressBar getProcess_line_bar() {
        return process_line_bar;
    }

    public TextField getTarget_host_text() {
        return target_host_text;
    }
}
