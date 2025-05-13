package com.capstone.contractmanagement.services.token;

import com.capstone.contractmanagement.components.JwtTokenUtils;
import com.capstone.contractmanagement.entities.Token;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.ExpiredTokenException;
import com.capstone.contractmanagement.repositories.ITokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService implements ITokenService {
    private static final int MAX_TOKENS = 3;
    @Value("${jwt.expiration}")
    private int expiration; //save to an environment variable

    @Value("${jwt.expiration-refresh-token}")
    private int expirationRefreshToken;

    private final com.capstone.contractmanagement.repositories.ITokenRepository ITokenRepository;
    private final JwtTokenUtils jwtTokenUtils;


    @Transactional
    @Override
    public Token addToken(User user, String token, boolean isMobileDevice) {
        ITokenRepository.deleteByUser(user);

        Optional<Token> existingToken = ITokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            throw new IllegalStateException("Token cũ chưa được xóa cho user_id: " + user.getId());
        }
        long expirationInSeconds = expiration;
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(expirationInSeconds);
        Token newToken = Token.builder()
                .user(user)
                .token(token)
                .revoked(false)
                .expired(false)
                .tokenType("Bearer")
                .expirationDate(expirationDateTime)
                .isMobile(isMobileDevice)
                .build();
        ITokenRepository.save(newToken);
        return newToken;

    }

    @Override
    public void deleteToken(String token) {
        Optional<Token> tokenEntity = ITokenRepository.findByToken(token);
        if (tokenEntity.isPresent()) {
            ITokenRepository.delete(tokenEntity.get());
        } else {
            throw new RuntimeException("Không tìm thấy token.");
        }
    }
}
