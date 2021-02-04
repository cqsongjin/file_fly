package com.cqsongjin.file.fly.constant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    public static int PORT;
    public static int FRAME_SIZE;
    public static String LAST_LINKED_IP;
    public static String OUTPUT_DIR;
    public static final String USER_HOME;

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
            System.out.println("找到并加载配置文件：" + path.toString());
            properties.load(Files.newBufferedReader(path, StandardCharsets.UTF_8));
        }
        PORT = Integer.parseInt(properties.getProperty("listen.port", "28080"));
        FRAME_SIZE = Integer.parseInt(properties.getProperty("transform.frame.size", "1048576"));
        LAST_LINKED_IP = properties.getProperty("last.linked.ip", "");
        OUTPUT_DIR = properties.getProperty("output.dir", "");
        writeConfig();
    }

    public static void writeConfig() throws IOException {
        final Path path = Path.of(USER_HOME, ".file_fly", "config.properties");
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
        }
        Properties properties = new Properties();
        properties.put("listen.port", PORT + "");
        properties.put("transform.frame.size", FRAME_SIZE + "");
        properties.put("last.linked.ip", LAST_LINKED_IP);
        properties.put("output.dir", OUTPUT_DIR);
        properties.store(Files.newBufferedWriter(path, StandardCharsets.UTF_8), "the file_fly config file");
    }
}
