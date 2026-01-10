package rssfetcher.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rssfetcher.demo.entities.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
}
