package donghyeon.dev.board.board.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFlux
public class BoardRouter {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(BoardHandler boardHandler) {

        return nest(path("/board"),
                route(GET("/"),boardHandler::getAllBoard))
                .andRoute(GET("/{id}"),boardHandler::getOneBoard)
                .andRoute(POST("/"),boardHandler::createBoard)
                .andRoute(PATCH("/"),boardHandler::updateBoard)
                .andRoute(DELETE("/"),boardHandler::deleteBoard);
    }
}
