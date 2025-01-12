package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.ForgotPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IForgotPasswordRepository extends JpaRepository<ForgotPassword, Long> {
    @Query("SELECT fp FROM ForgotPassword fp " +
            "WHERE fp.user.email = :email and fp.verified = false " +
            "AND fp.expirationTime = " +
            "( SELECT MAX(fp2.expirationTime) " +
            "FROM ForgotPassword fp2 WHERE fp2.user.email = :email AND fp2.verified = false )")
    Optional<ForgotPassword> findLatestOtpSent(@Param("email") String email);
}
