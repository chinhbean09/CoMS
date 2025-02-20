package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "forgot_passwords")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForgotPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "otp", nullable = false)
    private Integer otp;

    @Column(name = "expiration_date", nullable = false)
    private Date expirationTime;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "reset_token")
    private String resetToken;  // Token dùng để reset mật khẩu sau khi OTP được xác thực

    @Column(name = "otp_attempts")
    private int otpAttempts;    // Đếm số lần thử OTP

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
