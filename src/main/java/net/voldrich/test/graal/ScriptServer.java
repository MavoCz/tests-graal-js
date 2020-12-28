package net.voldrich.test.graal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@Slf4j
@SpringBootApplication
public class ScriptServer {
    public static void main(String[] args) {
        //Hooks.onErrorDropped(throwable -> log.warn("Unhandled exception dropped", throwable));
        SpringApplication.run(ScriptServer.class, args);
    }
}
