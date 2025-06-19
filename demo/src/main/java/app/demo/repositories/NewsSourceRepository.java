package app.demo.repositories;

import app.demo.entities.NewsSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
    Optional<NewsSource> findByRssUrl(String rssUrl);
    boolean existsByRssUrl(String rssUrl);
}
