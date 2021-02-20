package com.cqsongjin.file.fly.message;

import com.alibaba.fastjson.JSON;
import com.cqsongjin.file.fly.util.MD5Util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ClientMessage extends DTMessage{
    private Client client;
    private byte[] clientBytes;

    public ClientMessage() {
    }

    public ClientMessage(Client client) {
        this.client = client;
        this.clientBytes = JSON.toJSONString(this.client).getBytes(StandardCharsets.UTF_8);
        this.type = DTMessage.BROADCAST_CLIENT;
        this.md5 = MD5Util.toMD5Bytes(this.clientBytes);
        this.length = this.clientBytes.length;
    }

    @Override
    public void parse(DTMessage dtMessage) {
        super.parse(dtMessage);
        this.clientBytes = new byte[this.length];
        System.arraycopy(this.origin, 21, this.clientBytes, 0, this.length);
        this.client = JSON.parseObject(this.clientBytes, 0, this.length, StandardCharsets.UTF_8, Client.class);
    }

    @Override
    public void readBuffer(ByteBuffer buffer) {
        super.readBuffer(buffer);
        this.clientBytes = new byte[this.length];
        System.arraycopy(this.origin, 21, this.clientBytes, 0, this.length);
        this.client = JSON.parseObject(this.clientBytes, 0, this.length, StandardCharsets.UTF_8, Client.class);
    }

    @Override
    public void writeBuffer(ByteBuffer buffer) {
        buffer.put(type);
        buffer.put(md5);
        buffer.putInt(length);
        buffer.put(clientBytes);
        buffer.put(new byte[buffer.remaining()]);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
        this.clientBytes = JSON.toJSONString(this.client).getBytes(StandardCharsets.UTF_8);
        this.length = this.clientBytes.length;
    }

    public static class Client {
        private String ipAddr;
        private String name;

        public Client(String ipAddr, String name) {
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
}
