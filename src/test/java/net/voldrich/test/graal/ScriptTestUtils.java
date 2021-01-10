package net.voldrich.test.graal;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ScriptTestUtils {
    public static String fromResource(String scriptPath) {
        try {
            File resource = new ClassPathResource(scriptPath).getFile();
            return new String(Files.readAllBytes(resource.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
