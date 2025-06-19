package app.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Data
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id")
    private long id;

    private String title;
    private String content;

    private String source;
    private String url;

    private LocalDateTime published;
    private String summary;

    private String sha256Hash;
    private long simHash;


    @ManyToOne
    private Topic topic;

    @ManyToOne
    private ArticleCluster cluster;

}
