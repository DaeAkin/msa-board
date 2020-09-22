package donghyeon.dev.board.board.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class BoardRouter {
    @Bean
    public RouterFunction<ServerResponse> route(BoardHandler boardHandler) {
        return RouterFunctions.route(GET("/board"), boardHandler::helloWorld);
    }
}
