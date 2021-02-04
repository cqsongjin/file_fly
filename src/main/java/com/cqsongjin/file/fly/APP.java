package com.cqsongjin.file.fly;

import com.cqsongjin.file.fly.constant.Config;
import com.cqsongjin.file.fly.controller.DemoController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.*;

public class APP extends Application {

    public static final ExecutorService EXECUTOR_SERVICE;
    private DemoController controller;

    static {
        EXECUTOR_SERVICE = new ThreadPoolExecutor(5, 5, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), (r, e) -> {
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        });
    }

    @Override
    public void start(Stage stage) throws IOException {
        URL location = getClass().getResource("/index.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
//        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        final Parent parent = fxmlLoader.load();
        Scene scene = new Scene(parent);
        stage.setScene(scene);
        stage.setTitle("file fly app");
        this.controller = fxmlLoader.getController();
        this.controller.init();
        stage.show();
        stage.setOnCloseRequest(event -> exit());
    }

    public static void main(String[] args) {
        System.out.println("调整缓冲区大小：" + Config.FRAME_SIZE);
        launch();
    }

    public void exit() {
        try {
            Config.LAST_LINKED_IP = this.controller.target_host_text.getText();
            Config.OUTPUT_DIR =this.controller.output_dir_text.getText();
            Config.writeConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        EXECUTOR_SERVICE.shutdown();
        Platform.exit();
        System.exit(0);
    }

}