package com.cupk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {
    private Path root = Path.of("./uploads");

    public Path getRoot() { return root; }
    public void setRoot(Path root) { this.root = root; }
    public Path absoluteRoot() { return root.toAbsolutePath().normalize(); }
}
