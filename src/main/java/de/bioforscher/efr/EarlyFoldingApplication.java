package de.bioforscher.efr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class EarlyFoldingApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(EarlyFoldingApplication.class, args);
    }
}
