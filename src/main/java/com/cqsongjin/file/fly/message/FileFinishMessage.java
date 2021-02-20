package com.cqsongjin.file.fly.message;

import java.nio.charset.StandardCharsets;

public class FileFinishMessage extends DTMessage{
    public FileFinishMessage() {
    }

    public FileFinishMessage(byte type, byte[] md5) {
        super(type, md5, 0);
    }
}
