package com.cqsongjin.file.fly.common;

import com.cqsongjin.file.fly.constant.Config;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

//数据帧格式。type+md5+len+data，大小 1+16+4+1048576=1048597字节
//文件创建请求：type=0 + md5 + length（16位）+ filename
//文件传输请求：type=1 +md5 + len + data
//文件结束请求：type=2 + md5
//每次定长读取指定大小的字节数
public class FileChannelHandler {
    private final ByteBuffer byteBuffer;
    private final String targetDir;

    private FileChannel fileChannel;
    private boolean closed;
    public static int frameSize = Config.FRAME_SIZE;
    private int left;
    int i = 1;

    public FileChannelHandler(String targetDir) throws IOException {
        this.byteBuffer = ByteBuffer.allocate(frameSize);
        this.targetDir = targetDir;
        this.left = 0;
        init();
    }

    private void init() throws IOException {

    }

    public void readChannel2File(SelectionKey selectionKey, SocketChannel channel) throws IOException {
        System.out.println("读数据");
        int read = 0;
        do {
            if (this.closed) {
                System.out.println("当前传输已完成");
                return;
            }
            read = channel.read(byteBuffer);
            if (byteBuffer.position() < frameSize) {
                //数据不到一个包大小，不做处理
                continue;
            }
            //处理缓存中的数据
            byteBuffer.flip();
//            System.out.println("第 " + i++ + "次接收");
//            final byte[] array = byteBuffer.array();
//            for (int i1 = 0; i1 < 21; i1++) {
//                System.out.print(array[i1] + ",");
//            }
            final byte type = byteBuffer.get();
            byte[] md5 = new byte[16];
            byteBuffer.get(md5);
            final int length = byteBuffer.getInt();
//            System.out.println("读取数据载荷：" + length + " 实际剩余数据：" + byteBuffer.remaining());
            byte[] data = new byte[length];
            byteBuffer.get(data);
            handleFrame(selectionKey, channel, type, md5, length, data);
            byteBuffer.clear();
        } while (read > 0);
    }

    public void handleFrame(SelectionKey selectionKey, SocketChannel channel, byte type, byte[] md5, int length, byte[]data) throws IOException {
//        System.out.println("type -> " + type);
        if (type == 0) {
            //创建文件和文件夹
            String fileName = new String(data, StandardCharsets.UTF_8);
            final Path targetPath = Paths.get(targetDir, fileName);
            final File file = targetPath.toFile();
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            System.out.println("输出文件 -> " + file.getAbsolutePath());
            this.fileChannel = FileChannel.open(targetPath, CREATE, WRITE);
        } else if (type == 1) {
            //写文件
            fileChannel.write(ByteBuffer.wrap(data));
        } else if (type == 2) {
            System.out.println("完成文件");
            this.close(selectionKey, channel);
        } else {
            System.out.println("数据有误");
        }
    }

    public void close(SelectionKey selectionKey, SocketChannel channel) throws IOException {
        if (!this.closed) {
            selectionKey.cancel();
            channel.close();
            fileChannel.close();
            this.closed = true;
        }
    }
}
