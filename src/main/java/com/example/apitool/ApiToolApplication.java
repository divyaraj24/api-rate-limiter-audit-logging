package com.example.apitool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiToolApplication.class, args);
    }
}
