package com.joyhsin.hotspot;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("hotspot")
public record AppProperties(Path dataDir) { public AppProperties { if(dataDir==null) dataDir=Path.of("data"); } }
