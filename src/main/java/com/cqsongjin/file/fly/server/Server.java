package com.cqsongjin.file.fly.server;

import java.io.IOException;

public interface Server {
    void startServer() throws IOException;
    void stopServer() throws IOException;
}
