package com.cqsongjin.file.fly.message;

import java.nio.ByteBuffer;

public class FileTransformMessage extends DTMessage{
    private byte[] data;

    public FileTransformMessage() {
    }

    public FileTransformMessage(DTMessage dtMessage) {
        this.parse(dtMessage);
    }

    @Override
    public void parse(DTMessage dtMessage) {
        super.parse(dtMessage);
        data = new byte[this.length];
        System.arraycopy(this.origin, 21, this.data, 0, this.length);
    }

    @Override
    public void readBuffer(ByteBuffer buffer) {
        super.readBuffer(buffer);
        data = new byte[this.length];
        System.arraycopy(this.origin, 21, this.data, 0, this.length);
    }

    @Override
    public void writeBuffer(ByteBuffer buffer) {
        buffer.put(type);
        buffer.put(md5);
        buffer.putInt(length);
        buffer.put(data);
        buffer.put(new byte[buffer.remaining()]);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        this.length = data.length;
    }
}
