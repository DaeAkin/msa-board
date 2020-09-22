package donghyeon.dev.board.board.api;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class BoardHandler {
    public Mono<ServerResponse> helloWorld(ServerRequest request) {
        Mono<Integer> just = Mono.just(1);
        return ServerResponse.ok().body(just, Integer.class);
    }
}
