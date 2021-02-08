package com.cqsongjin.file.fly.common;

import com.cqsongjin.file.fly.message.ClientMessage;

import java.util.LinkedList;
import java.util.List;

public class Global {
    private static List<ClientMessage.Client> clientList = new LinkedList<>();

    public static List<ClientMessage.Client> getClientList() {
        return clientList;
    }

    public static void setClientList(List<ClientMessage.Client> clientList) {
        Global.clientList = clientList;
    }
}
