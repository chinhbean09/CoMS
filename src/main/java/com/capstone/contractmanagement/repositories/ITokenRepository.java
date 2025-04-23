package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Token;
import com.capstone.contractmanagement.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ITokenRepository extends JpaRepository<Token, Long> {

    List<Token> findByUser(User user);

    Token findByToken(String token);

    List<Token> findByUserId(Long userId);
}
