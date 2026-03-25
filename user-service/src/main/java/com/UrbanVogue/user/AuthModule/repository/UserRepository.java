package com.UrbanVogue.user.AuthModule.repository;

import com.UrbanVogue.user.AuthModule.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // email can be there or not be there
    Optional<User> findByEmail(String email);

}