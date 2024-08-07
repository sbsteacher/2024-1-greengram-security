package com.green.greengram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GreengramApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreengramApplication.class, args);
    }



}
