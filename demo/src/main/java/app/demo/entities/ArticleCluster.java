package app.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "article_clusters")
@Data
public class ArticleCluster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cluster_id")
    private Long id;

    @ManyToOne
    private Topic topic;

    @OneToOne
    private Article originalSource;

    @OneToMany(mappedBy = "cluster")
    private Set<Article> articles = new HashSet<>();


}
