package donghyeon.dev.board.board.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Table
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    private Long id;

    private String title;
    private Long userId;
    private String content;


}
