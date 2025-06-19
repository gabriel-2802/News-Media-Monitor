package app.demo.repositories;

import app.demo.entities.Role;
import app.demo.entities.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByAuthority(RoleName authority);
    Optional<Role> findByAuthority(RoleName authority);
}
