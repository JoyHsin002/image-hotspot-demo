package com.joyhsin.hotspot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HotspotDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotspotDemoApplication.class, args);
    }
}
