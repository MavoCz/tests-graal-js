package net.voldrich.test.graal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ScriptServer {
    public static void main(String[] args) {
        SpringApplication.run(ScriptServer.class, args);
    }
}
