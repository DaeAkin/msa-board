package donghyeon.dev.board.board.repository;

import donghyeon.dev.board.board.domain.Board;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends ReactiveMongoRepository<Board, String> {

}
