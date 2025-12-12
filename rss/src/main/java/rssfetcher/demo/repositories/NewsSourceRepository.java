package rssfetcher.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rssfetcher.demo.entities.NewsSource;

@Repository
public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
}
