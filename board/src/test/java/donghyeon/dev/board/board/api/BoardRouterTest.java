package donghyeon.dev.board.board.api;

import donghyeon.dev.board.board.domain.Board;
import donghyeon.dev.board.board.dto.BoardSaveRequest;
import donghyeon.dev.board.board.mock.BoardGenerator;
import donghyeon.dev.board.board.repository.BoardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureWebTestClient

public class BoardRouterTest {
    private final static String BOARD_API_URL = "/board";

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private BoardRepository boardRepository;

    @AfterEach
    public void cleanUpData() {
        boardRepository.deleteAll().block();
    }

    @Test
    public void board_get_Test() {
        Board board = new Board("boardTitle",1L,"boardContent");
        Board boardEntity = BoardGenerator.createBoard(boardRepository, board).block();

        webClient.get()
                .uri(BOARD_API_URL+"/"+boardEntity.getObjectId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Board.class)
                .consumeWith(boardEntityExchangeResult -> System.out.println(boardEntityExchangeResult.getResponseBody()));
    }

    @Test
    public void board_getAll_Test() {
        Board board1 = new Board("boardTitle1",1L,"boardContent2");
        Board board2 = new Board("boardTitle2",1L,"boardContent2");
        BoardGenerator.createBoard(boardRepository,Flux.just(board1,board2).

        webClient.get()
                .uri(BOARD_API_URL)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Board.class)
                .consumeWith(boardEntityExchangeResult -> System.out.println(boardEntityExchangeResult.getResponseBody()));

    }

    @Test
    public void board_create_Test() {
        BoardSaveRequest boardSaveRequest = new BoardSaveRequest("title","content");

        webClient.post()
                .uri(BOARD_API_URL)
                .body(Mono.just(boardSaveRequest),BoardSaveRequest.class)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void board_update_Test() {

    }



}