package com.cqsongjin.file.fly.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FileCreateMessage extends DTMessage{
    private String fileName;

    public FileCreateMessage() {
    }

    public FileCreateMessage(DTMessage dtMessage) {
        this.parse(dtMessage);
    }


    @Override
    public void parse(DTMessage dtMessage) {
        super.parse(dtMessage);
        this.fileName = new String(this.origin, 21, this.length, StandardCharsets.UTF_8);
    }

    @Override
    public void readBuffer(ByteBuffer buffer) {
        super.readBuffer(buffer);
        this.fileName = new String(this.origin, 21, this.length, StandardCharsets.UTF_8);
    }

    @Override
    public void writeBuffer(ByteBuffer buffer) {
        buffer.put(type);
        buffer.put(md5);
        buffer.putInt(fileName.getBytes(StandardCharsets.UTF_8).length);
        buffer.put(fileName.getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[buffer.remaining()]);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
