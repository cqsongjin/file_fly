package com.cqsongjin.file.fly.message;

import java.nio.charset.StandardCharsets;

public class FileCreateMessage extends DTMessage{
    public String getFileName() {
        return new String(this.getData(), StandardCharsets.UTF_8);
    }
}
