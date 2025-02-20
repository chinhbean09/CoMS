package com.capstone.contractmanagement.services.user;

 import com.capstone.contractmanagement.components.JwtTokenUtils;
 import com.capstone.contractmanagement.components.LocalizationUtils;
 import com.capstone.contractmanagement.dtos.DataMailDTO;
 import com.capstone.contractmanagement.dtos.user.*;
 import com.capstone.contractmanagement.entities.Role;
 import com.capstone.contractmanagement.entities.Token;
 import com.capstone.contractmanagement.entities.User;
 import com.capstone.contractmanagement.exceptions.DataNotFoundException;
 import com.capstone.contractmanagement.exceptions.InvalidParamException;
 import com.capstone.contractmanagement.exceptions.PermissionDenyException;
 import com.capstone.contractmanagement.repositories.IRoleRepository;
 import com.capstone.contractmanagement.repositories.ITokenRepository;
 import com.capstone.contractmanagement.repositories.IUserRepository;
 import com.capstone.contractmanagement.responses.User.UserResponse;
 import com.capstone.contractmanagement.services.sendmails.IMailService;
 import com.capstone.contractmanagement.utils.MailTemplate;
 import com.capstone.contractmanagement.utils.MessageKeys;
 import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
 import jakarta.validation.Valid;
 import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
 import org.springframework.validation.BindingResult;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
 import java.security.SecureRandom;
 import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final IUserRepository UserRepository;
    private final LocalizationUtils localizationUtils;
    private final IRoleRepository RoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtils;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final AuthenticationManager authenticationManager;
    private final ITokenRepository TokenRepository;
    private final Cloudinary cloudinary;
    private final IMailService mailService;

    @Override
    @Transactional
    public User registerUser( @Valid @RequestBody CreateUserDTO userDTO) throws Exception {
        String phoneNumber = userDTO.getPhoneNumber();
        if (phoneNumber != null && UserRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DataIntegrityViolationException(
                    localizationUtils.getLocalizedMessage(MessageKeys.PHONE_NUMBER_ALREADY_EXISTS));
        }

        String email = userDTO.getEmail();
        if (UserRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException(
                    localizationUtils.getLocalizedMessage(MessageKeys.EMAIL_ALREADY_EXISTS));
        }

        // Use the default roleId = 2 if none is provided
        Long roleId = userDTO.getRoleId() != null ? userDTO.getRoleId() : 2L;
        Role role = RoleRepository.findById(roleId)
                .orElseThrow(() -> new DataNotFoundException(
                        localizationUtils.getLocalizedMessage(MessageKeys.ROLE_DOES_NOT_EXISTS)));

        // Prevent registration of an Admin account
        if (role.getRoleName().equalsIgnoreCase("ADMIN")) {
            throw new PermissionDenyException("Not allowed to register for an Admin account");
        }

        // Create a new user instance
        User newUser = User.builder()
                .email(userDTO.getEmail())
                .phoneNumber(userDTO.getPhoneNumber())
                .fullName(userDTO.getFullName())
                .active(true)
                .address(userDTO.getAddress())
                .isCeo(userDTO.getIsCeo())
                .build();
        newUser.setRole(role);

        // Always generate a random password to ensure a non-null value
        String generatedPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(generatedPassword);
        newUser.setPassword(encodedPassword);

        // Send the generated password via email (if needed)
        sendAccountPassword(newUser.getEmail(), generatedPassword);

        // Save the new user
        User user = UserRepository.save(newUser);
        return user;
    }

    private void sendAccountPassword(String email, String password) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(email);
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.USER_REGISTER);

            Map<String, Object> props = new HashMap<>();
            props.put("password", password);
            dataMailDTO.setProps(props); // Set props to dataMailDTO

            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.USER_REGISTER);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email", e);
        }
    }

    /**
     * Generates a random 8-character password that includes at least one uppercase letter,
     * one lowercase letter, one digit, and one special character.
     */
    private String generateRandomPassword() {
        int length = 8;
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialCharacters = "!@#$%^&*()-_+=<>?";

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(length);

        // Ensure each character type is represented at least once
        password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
        password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialCharacters.charAt(random.nextInt(specialCharacters.length())));

        // Create a pool of all allowed characters for the remaining characters
        String allAllowed = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;
        for (int i = 4; i < length; i++) {
            password.append(allAllowed.charAt(random.nextInt(allAllowed.length())));
        }

        // Shuffle the characters to ensure randomness
        List<Character> passwordChars = new ArrayList<>();
        for (char c : password.toString().toCharArray()) {
            passwordChars.add(c);
        }
        Collections.shuffle(passwordChars, random);

        StringBuilder finalPassword = new StringBuilder();
        for (char c : passwordChars) {
            finalPassword.append(c);
        }

        return finalPassword.toString();
    }


    @Override
    public User getUserDetailsFromToken(String token) throws DataNotFoundException {
        if (jwtTokenUtils.isTokenExpired(token)) {
            throw new DataNotFoundException("Token is expired");
        }

        Map<String, String> identifiers = jwtTokenUtils.extractIdentifier(token);
        if (identifiers == null || (identifiers.get("email") == null && identifiers.get("phoneNumber") == null)) {
            logger.error("Identifier extracted from token is null or empty");
            throw new DataNotFoundException("Identifier not found in token");
        }

        String emailOrPhone = identifiers.get("email") != null ? identifiers.get("email") : identifiers.get("phoneNumber");
        Optional<User> user = UserRepository.findByEmailOrPhoneNumber(emailOrPhone, emailOrPhone);

        return user.orElseThrow(() -> {
            logger.error("User not found for identifier: {}", emailOrPhone);
            return new DataNotFoundException("User not found");
        });
    }

//    @Override
//    public void sendMailForRegisterSuccess(String fullName, String email, String password) {
//        try {
//            DataMailDTO dataMail = new DataMailDTO();
//            dataMail.setTo(email);
//            dataMail.setSubject(MailTemplate.SEND_MAIL_SUBJECT.USER_REGISTER);
//
//            Map<String, Object> props = new HashMap<>();
//            props.put("fulName", fullName);
//            props.put("email", email);
//            props.put("password", password);
//
//            dataMail.setProps(props);
//
//            mailService.sendHtmlMail(dataMail, MailTemplate.SEND_MAIL_TEMPLATE.USER_REGISTER);
//        } catch (MessagingException exp) {
//            logger.error("Failed to send registration success email", exp);
//        }
//    }

//    @Override
//    public void changePassword(Long id, ChangePasswordDTO changePasswordDTO) throws DataNotFoundException {
//        User exsistingUser = UserRepository.findById(id)
//                .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
//        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), exsistingUser.getPassword())) {
//            throw new DataNotFoundException(MessageKeys.OLD_PASSWORD_WRONG);
//        }
//        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
//            throw new DataNotFoundException(MessageKeys.CONFIRM_PASSWORD_NOT_MATCH);
//        }
//        exsistingUser.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
//        UserRepository.save(exsistingUser);
//    }
//
//    @Override
//    public void updatePassword(String email, String password) throws DataNotFoundException {
//        User user = UserRepository.findByEmail(email)
//                .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
//        user.setPassword(passwordEncoder.encode(password));
//        UserRepository.save(user);
//    }

    @Override
    public String login(UserLoginDTO userLoginDTO) throws Exception {
        String loginIdentifier = userLoginDTO.getLoginIdentifier();
        try {
            User existingUser = UserRepository.findByEmailOrPhoneNumber(loginIdentifier, loginIdentifier)
                    .orElseThrow(() -> new UsernameNotFoundException(MessageKeys.USER_NOT_FOUND));

            // check if account is locked
            if (!existingUser.isActive()) {
                throw new LockedException(MessageKeys.USER_IS_LOCKED);
            }

            // check failed login attempts
            if (existingUser.getFailedLoginAttempts() >= 5) {
                existingUser.setActive(false);
                UserRepository.save(existingUser);
                throw new LockedException(MessageKeys.USER_IS_LOCKED);
            }

            if (!passwordEncoder.matches(userLoginDTO.getPassword(), existingUser.getPassword())) {
                existingUser.setFailedLoginAttempts(existingUser.getFailedLoginAttempts() + 1);
                UserRepository.save(existingUser);
                throw new BadCredentialsException(MessageKeys.PASSWORD_NOT_MATCH);
            }

            // login success reset failed login attempts
            existingUser.setFailedLoginAttempts(0);
            UserRepository.save(existingUser);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    loginIdentifier, userLoginDTO.getPassword(), existingUser.getAuthorities());

            authenticationManager.authenticate(authenticationToken);
            return jwtTokenUtils.generateToken(existingUser);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new DataIntegrityViolationException("Multiple users found with the same identifier: " + loginIdentifier);
        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for user: {}", loginIdentifier, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void blockOrEnable(Long userId, Boolean active) throws DataNotFoundException, PermissionDenyException {
        User existingUser = UserRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
        existingUser.setActive(active);
        existingUser.setFailedLoginAttempts(0);

        if(existingUser.getRole().getRoleName().equals(Role.ADMIN)) {
            throw new PermissionDenyException("Not allowed to block Admin account");
        }
        UserRepository.save(existingUser);
        }
    @Override
    public Page<UserResponse> getAllUsers(String keyword, PageRequest pageRequest) {
        Page<User> usersPage;
        usersPage = UserRepository.searchUsers(keyword, pageRequest);
        return usersPage.map(UserResponse::fromUser);
    }

    @Override
    public User getUser(Long id) throws DataNotFoundException {
        return UserRepository.findById(id).orElseThrow(() -> new DataNotFoundException("User not found"));
    }

    @Override
    public void deleteUser(Long userId) {
        Optional<User> optionalUser = UserRepository.findById(userId);
        List<Token> tokens = TokenRepository.findByUserId(userId);
        TokenRepository.deleteAll(tokens);
        optionalUser.ifPresent(UserRepository::delete);
    }
    @Override
    public void updateUser(Long userId, UpdateUserDTO userDTO) throws DataNotFoundException {
        // Check if the user exists
        User user = UserRepository.findById(userId).orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));

        // Update the user's information
        user.setFullName(userDTO.getFullName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setEmail(userDTO.getEmail());
        user.setAddress(userDTO.getAddress());
        user.setIsCeo(userDTO.getIsCeo());
        user.setRole(RoleRepository.findById(userDTO.getRoleId()).orElseThrow(() -> new DataNotFoundException(MessageKeys.ROLE_DOES_NOT_EXISTS)));

        // Save the updated user entity
        UserRepository.save(user);
    }

    @Override
    public User getUserDetailsFromRefreshToken(String refreshToken) throws Exception {
        Token existingToken = TokenRepository.findByRefreshToken(refreshToken);
        return getUserDetailsFromToken(existingToken.getToken());
    }

    @Override
    public List<UserResponse> getAllUsers(Long roleId) {
        List<User> users = UserRepository.findByRoleId(roleId);
        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    public void updatePassword(String email, String password) throws DataNotFoundException {
        User user = UserRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
        user.setPassword(passwordEncoder.encode(password));
        UserRepository.save(user);
    }

    @Override
    public User updateUserAvatar(long id, MultipartFile avatar) {
        User user = UserRepository.findById(id).orElse(null);
        if (user != null && avatar != null && !avatar.isEmpty()) {
            try {
                // Check if the uploaded file is an image
                MediaType mediaType = MediaType.parseMediaType(Objects.requireNonNull(avatar.getContentType()));
                if (!mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) &&
                        !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
                    throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_IMAGES_FILE_MUST_BE_IMAGE));
                }

                // Upload the avatar to Cloudinary
                Map uploadResult = cloudinary.uploader().upload(avatar.getBytes(),
                        ObjectUtils.asMap("folder", "user_avatar/" + id, "public_id", avatar.getOriginalFilename()));

                // Get the URL of the uploaded avatar
                String avatarUrl = uploadResult.get("secure_url").toString();
                user.setAvatar(avatarUrl);

                // Save the updated user entity
                UserRepository.save(user);
                return user;
            } catch (IOException e) {
                logger.error("Failed to upload avatar for user with ID {}", id, e);
            }
        }
        return null;
    }

    @Override
    public void changePassword(Long id, UpdatePasswordDTO changePasswordDTO) throws DataNotFoundException {
        User exsistingUser = UserRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), exsistingUser.getPassword())) {
            throw new DataNotFoundException(MessageKeys.OLD_PASSWORD_WRONG);
        }
        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            throw new DataNotFoundException(MessageKeys.CONFIRM_PASSWORD_NOT_MATCH);
        }
        exsistingUser.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        UserRepository.save(exsistingUser);
    }
}
