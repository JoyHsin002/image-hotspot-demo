package com.joyhsin.hotspot;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("hotspot")
public record AppProperties(Path dataDirectory) { public AppProperties { if(dataDirectory==null) dataDirectory=Path.of("data"); } }
