package com.xazktx;

import com.xazktx.utils.FileTransferServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PulldataclientApplication {


    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(PulldataclientApplication.class, args);
        FileTransferServer bean = run.getBean(FileTransferServer.class);
        try {
            bean.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
