package com.cqsongjin.file.fly.message;

import com.cqsongjin.file.fly.util.NumberUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FileCreateMessage extends DTMessage{
    private long fileSize;
    private String fileName;

    public FileCreateMessage() {
    }

    public FileCreateMessage(byte[] md5, int length, long fileSize, String fileName) {
        super(DTMessage.FILE_CREATE, md5, length);
        this.fileSize = fileSize;
        this.fileName = fileName;
    }

    public FileCreateMessage(DTMessage dtMessage) {
        this.parse(dtMessage);
    }


    @Override
    public void parse(DTMessage dtMessage) {
        super.parse(dtMessage);
        byte[] fileSizeBytes = new byte[8];
        System.arraycopy(this.origin, 21, fileSizeBytes, 0, 8);
        this.fileSize = NumberUtil.bytes2Long(fileSizeBytes);
        this.fileName = new String(this.origin, 29, this.length, StandardCharsets.UTF_8);
    }

    @Override
    public void readBuffer(ByteBuffer buffer) {
        super.readBuffer(buffer);
//        byte[] fileSizeBytes = new byte[8];
//        System.arraycopy(this.origin, 21, fileSizeBytes, 0, 8);
//        this.fileSize = NumberUtil.bytes2Long(fileSizeBytes);
        this.fileSize = buffer.getLong();
        this.fileName = new String(this.origin, 29, this.length, StandardCharsets.UTF_8);
    }

    @Override
    public void writeBuffer(ByteBuffer buffer) {
        buffer.put(type);
        buffer.put(md5);
        buffer.putInt(fileName.getBytes(StandardCharsets.UTF_8).length);
        buffer.putLong(this.fileSize);
        buffer.put(fileName.getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[buffer.remaining()]);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
