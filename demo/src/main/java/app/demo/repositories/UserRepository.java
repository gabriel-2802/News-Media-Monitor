package app.demo.repositories;

import app.demo.entities.Topic;
import app.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.subscribedTopics WHERE u.username = :username")
    Optional<User> findByUsernameWithTopics(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.subscribedTopics")
    List<User> findAllWithTopics();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.subscribedTopics WHERE :topic MEMBER OF u.subscribedTopics")
    List<User> findAllSubcribedToTopic(Topic topic);

}
