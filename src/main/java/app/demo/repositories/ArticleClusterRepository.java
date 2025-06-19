package app.demo.repositories;

import app.demo.entities.ArticleCluster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleClusterRepository extends JpaRepository<ArticleCluster, Long> {
}
