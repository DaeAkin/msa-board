package donghyeon.dev.board.board.application;

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
        String id = request.pathVariable("id");
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
        Mono<BoardSaveRequest> boardSaveRequest = request.bodyToMono(BoardSaveRequest.class);
        Mono<Board> boardSave = boardSaveRequest.flatMap(board -> boardRepository.save(board.toEntity()));
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(boardSave, Board.class);
    }

//    public Mono<ServerResponse> updateBoard(ServerRequest request) {
//        String id = request.pathVariable("id");
//        boardRepository.findById(id)
//                .
//        Mono<BoardSaveRequest> boardSaveRequest = request.bodyToMono(BoardSaveRequest.class);
//        Mono<Board> boardUpdate = boardSaveRequest.flatMap(board -> boardRepository.save(board.toEntity()));
//        return ServerResponse.ok()
//                .contentType(MediaType.APPLICATION_STREAM_JSON)
//                .body(boardUpdate, Board.class);
//    }

    public Mono<ServerResponse> deleteBoard(ServerRequest request) {
        String id = request.pathVariable("id");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(boardRepository.deleteById(id), Board.class);
    }






}
