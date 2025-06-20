package app.demo.repositories;

import app.demo.entities.Article;
import app.demo.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByTopic(Topic topic);
}
