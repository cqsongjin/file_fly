package com.cqsongjin.file.fly.message;

import java.nio.ByteBuffer;

public class DTMessage {
    private static final byte FILE_CREATE = 0;
    private static final byte FILE_TRANSFORM = 1;
    private static final byte FILE_FINISH = 2;
    private static final byte BROADCAST_REQ = 3;
    private static final byte BROADCAST_REPLY = 4;

    //1个字节
    private byte type;
    //16个字节
    private byte[] md5;
    //32个字节
    private int length;
    //取决于length长度
    private byte[] data;
    //原始字节数组
    private byte[] origin;

    public void readBuffer(ByteBuffer buffer) {
        this.origin = buffer.array();
        type = buffer.get();
        md5 = new byte[16];
        buffer.get(md5);
        length = buffer.getInt();
        data = new byte[length];
        buffer.get(data);
    }

    public void writeBuffer(ByteBuffer buffer) {
        buffer.put(type);
        buffer.put(md5);
        buffer.putInt(data.length);
        buffer.put(data);
    }

    public byte getType() {
        return type;
    }

    public byte[] getMd5() {
        return md5;
    }

    public int getLength() {
        return length;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getOrigin() {
        return origin;
    }
}
