package com.cqsongjin.file.fly.constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    public static int TRANSFORM_PORT;
    public static int BROADCAST_PORT;
    public static int TRANSFORM_FRAME_SIZE;
    public static int BROADCAST_FRAME_SIZE;
    public static String LAST_LINKED_IP;
    public static String OUTPUT_DIR;
    public static final String USER_HOME;
    public static String HOST_IP;
    public static String HOST_NAME;


    static {
        USER_HOME = System.getProperty("user.home", "/tmp");
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void loadConfig() throws IOException {
        final Path path = Path.of(USER_HOME, ".file_fly", "config.properties");
        Properties properties = new Properties();
        if (Files.exists(path)) {
            log.info("找到并加载配置文件：" + path.toString());
            properties.load(Files.newBufferedReader(path, StandardCharsets.UTF_8));
        }
        TRANSFORM_PORT = Integer.parseInt(properties.getProperty("listen.transform.port", "28080"));
        BROADCAST_PORT = Integer.parseInt(properties.getProperty("listen.broadcast.port", "28082"));

        TRANSFORM_FRAME_SIZE = Integer.parseInt(properties.getProperty("transform.frame.size", "1048576"));
        BROADCAST_FRAME_SIZE = Integer.parseInt(properties.getProperty("broadcast.frame.size", "1024"));

        LAST_LINKED_IP = properties.getProperty("last.linked.ip", "");
        OUTPUT_DIR = properties.getProperty("output.dir", "");

        HOST_IP = properties.getProperty("host.ip", "");
        HOST_NAME = properties.getProperty("host.name", "");

        writeConfig();
    }

    public static void writeConfig() throws IOException {
        final Path path = Path.of(USER_HOME, ".file_fly", "config.properties");
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
        }
        Properties properties = new Properties();
        properties.put("listen.transform.port", TRANSFORM_PORT + "");
        properties.put("listen.broadcast.port", BROADCAST_PORT + "");
        properties.put("transform.frame.size", TRANSFORM_FRAME_SIZE + "");
        properties.put("broadcast.frame.size", BROADCAST_FRAME_SIZE + "");
        properties.put("last.linked.ip", LAST_LINKED_IP);
        properties.put("output.dir", OUTPUT_DIR);
        properties.put("host.ip", HOST_IP);
        properties.put("host.name", HOST_NAME);
        properties.store(Files.newBufferedWriter(path, StandardCharsets.UTF_8), "the file_fly config file");
    }
}
