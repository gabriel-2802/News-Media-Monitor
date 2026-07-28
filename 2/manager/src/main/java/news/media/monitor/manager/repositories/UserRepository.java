package news.media.monitor.manager.repositories;

import news.media.monitor.manager.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    List<User> findAll();

    @EntityGraph(attributePaths = "roles")
    Page<User> findAll(Pageable pageable);

    boolean existsByEmail(String email);

    @Query("select u.id from User u where u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);
}