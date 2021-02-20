package com.cqsongjin.file.fly.util;

import java.nio.ByteBuffer;

public final class NumberUtil {
    public static byte[] long2Bytes(long src) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, src);
        return buffer.array();
    }

    public static long bytes2Long(byte[] src) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(src);
        buffer.flip();
        return buffer.getLong();
    }
}
