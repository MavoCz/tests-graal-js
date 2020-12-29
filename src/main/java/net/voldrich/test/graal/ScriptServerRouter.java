package net.voldrich.test.graal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ScriptServerRouter {

    @Bean
    public RouterFunction<ServerResponse> route(ScriptHandler scriptHandler) {
        return RouterFunctions
                .route(RequestPredicates
                                .POST("/script/execute")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        scriptHandler::executeScript);
    }

}
