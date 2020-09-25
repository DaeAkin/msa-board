package donghyeon.dev.board.board.api;

import donghyeon.dev.board.board.domain.Board;
import donghyeon.dev.board.board.dto.BoardSaveRequest;
import donghyeon.dev.board.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BoardHandler {

    private final BoardRepository boardRepository;

    public Mono<ServerResponse> getOneBoard(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(boardRepository.findById(id), Board.class);
    }

    public Mono<ServerResponse> getAllBoard(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(boardRepository.findAll(), Board.class);
    }

    public Mono<ServerResponse> createBoard(ServerRequest request) {
        Mono<BoardSaveRequest> boardSave = request.bodyToMono(BoardSaveRequest.class);
        Mono<Board> board = boardSave.map(BoardSaveRequest::toEntity);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(boardRepository.save(board), Board.class);
    }

    public Mono<ServerResponse> updateBoard(ServerRequest request) {
        Mono<BoardSaveRequest> boardSave = request.bodyToMono(BoardSaveRequest.class);
        Mono<Board> board = boardSave.map(BoardSaveRequest::toEntity);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(boardRepository.save(board), Board.class);
    }

    public Mono<ServerResponse> deleteBoard(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(boardRepository.deleteById(id), Board.class);
    }






}
