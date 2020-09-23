package donghyeon.dev.board.board.repository;

import donghyeon.dev.board.board.domain.Board;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BoardRepository extends R2dbcRepository<Board,Long> {
    Mono<Board> save(Mono<Board> board);
}
