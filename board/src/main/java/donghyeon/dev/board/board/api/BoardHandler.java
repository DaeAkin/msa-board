package donghyeon.dev.board.board.api;

import donghyeon.dev.board.board.domain.Board;
import donghyeon.dev.board.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class BoardHandler {

    private final BoardRepository boardRepository = null;

    public Mono<ServerResponse> getOneBoard(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return ServerResponse.ok().body(boardRepository.findById(id), Board.class);
    }

    public Mono<ServerResponse> getAllBoard(ServerRequest request) {
        return ServerResponse.ok().body(Mono.just(new Board()), Board.class);
    }

}
