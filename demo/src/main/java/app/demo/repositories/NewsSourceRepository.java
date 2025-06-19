package app.demo.repositories;

import app.demo.entities.NewsSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
}
