package com.campus.library.repository;

import com.campus.library.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUniversityId(String universityId);

    boolean existsByEmail(String email);

    boolean existsByUniversityId(String universityId);
}
