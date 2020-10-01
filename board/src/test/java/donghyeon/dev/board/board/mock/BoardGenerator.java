package donghyeon.dev.board.board.mock;

import donghyeon.dev.board.board.domain.Board;
import donghyeon.dev.board.board.repository.BoardRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BoardGenerator {

    public static Mono<Board> createBoard(BoardRepository boardRepository, Board board) {
            return boardRepository.save(board);
    }

    public static Flux<Board> createBoard(BoardRepository boardRepository, Flux<Board> board) {
        return boardRepository.saveAll(board);
    }

}
