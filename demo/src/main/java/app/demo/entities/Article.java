package app.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "articles")
@Data
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id")
    private long id;

    @Column(columnDefinition = "TEXT")
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 512)
    private String source;
    @Column(columnDefinition = "TEXT", unique = true)
    private String url;

    private Date published;
    @Column(columnDefinition = "TEXT")
    private String summary;

    @ManyToOne
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "cluster_id")
    private ArticleCluster cluster;

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", source='" + source + '\'' +
                ", url='" + url + '\'' +
                ", published=" + published +
                ", summary='" + summary + '\'' +
                ", topic=" + (topic != null ? topic.getName() : "null") +
                ", cluster=" + (cluster != null ? cluster.getId() : "null") +
                '}';
    }

}
