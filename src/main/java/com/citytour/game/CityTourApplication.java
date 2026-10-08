package com.citytour.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CityTourApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityTourApplication.class, args);
    }
}
