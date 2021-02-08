package com.cqsongjin.file.fly.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DTMessage {
    public static final byte FILE_CREATE = 0;
    public static final byte FILE_TRANSFORM = 1;
    public static final byte FILE_FINISH = 2;
    public static final byte BROADCAST_CLIENT = 3;

    //原始字节数组
    protected byte[] origin;
    //1个字节
    protected byte type;
    //16个字节
    protected byte[] md5;
    //4个字节
    protected int length;

    public void parse(DTMessage dtMessage) {
        this.origin = dtMessage.origin;
        this.type = dtMessage.type;
        this.md5 = dtMessage.md5;
        this.length = dtMessage.length;
    }

    public void readBuffer(ByteBuffer buffer) {
        this.origin = buffer.array();
        type = buffer.get();
        md5 = new byte[16];
        buffer.get(md5);
        length = buffer.getInt();
    }

    public void writeBuffer(ByteBuffer buffer) {
        buffer.put(type);
        buffer.put(md5);
        buffer.putInt(length);
        buffer.put(new byte[buffer.remaining()]);
    }

    public byte getType() {
        return type;
    }
}
