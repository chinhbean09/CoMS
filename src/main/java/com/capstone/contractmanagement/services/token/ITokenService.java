package com.capstone.contractmanagement.services.token;

import com.capstone.contractmanagement.entities.Token;
import com.capstone.contractmanagement.entities.User;
import org.springframework.stereotype.Service;

@Service

public interface ITokenService {
    com.capstone.contractmanagement.entities.Token addToken(User user, String token, boolean isMobileDevice);

    Token refreshToken(String refreshToken, User user) throws Exception;

    void deleteToken(String token);
}
