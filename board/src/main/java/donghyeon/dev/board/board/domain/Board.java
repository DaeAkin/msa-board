package donghyeon.dev.board.board.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import org.springframework.data.annotation.Id;



@Document
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String objectId;

    private String title;
    private Long userId;
    private String content;

    public Board(String title, Long userId, String content) {
        this.title = title;
        this.userId = userId;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Board{" +
                "objectId='" + objectId + '\'' +
                ", title='" + title + '\'' +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                '}';
    }
}
