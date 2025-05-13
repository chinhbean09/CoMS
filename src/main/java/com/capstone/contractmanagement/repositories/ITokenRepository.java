package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Token;
import com.capstone.contractmanagement.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByUser(User user);

    Optional<Token> findByToken(String token);

    Optional<Token> findByUserId(Long userId);

    void deleteByUser(User user);

}
