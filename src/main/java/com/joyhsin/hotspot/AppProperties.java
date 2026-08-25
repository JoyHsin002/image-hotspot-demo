package com.joyhsin.hotspot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "hotspot")
public record AppProperties(Path dataDirectory, long maxImageBytes) {

    public AppProperties {
        if (dataDirectory == null) {
            dataDirectory = Path.of("./data");
        }
        if (maxImageBytes <= 0) {
            maxImageBytes = 10L * 1024 * 1024;
        }
    }

    public Path sceneStoreFile() {
        return dataDirectory.resolve("scenes.json");
    }

    public Path uploadDirectory() {
        return dataDirectory.resolve("uploads");
    }
}
