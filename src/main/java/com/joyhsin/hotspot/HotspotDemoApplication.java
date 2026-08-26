package com.joyhsin.hotspot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
@SpringBootApplication @EnableConfigurationProperties(AppProperties.class)
public class HotspotDemoApplication { public static void main(String[] args){ SpringApplication.run(HotspotDemoApplication.class,args); } }
