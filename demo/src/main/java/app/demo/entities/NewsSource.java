package app.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "news_sources")
@Data
public class NewsSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String baseUrl;
    @Column(unique = true)
    private String rssUrl;
}
