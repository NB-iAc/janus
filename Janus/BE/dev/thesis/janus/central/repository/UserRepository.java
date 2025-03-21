package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsertoken(String usertoken);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

}