package com.marat.taskmanager.securetaskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SecureTaskManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureTaskManagerApplication.class, args);
    }

}
