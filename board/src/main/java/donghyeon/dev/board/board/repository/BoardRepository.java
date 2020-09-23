package donghyeon.dev.board.board.repository;

import donghyeon.dev.board.board.domain.Board;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends ReactiveCrudRepository<Board,Long> {
}
