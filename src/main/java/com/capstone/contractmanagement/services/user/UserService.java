package com.capstone.contractmanagement.services.user;

 import com.capstone.contractmanagement.components.JwtTokenUtils;
 import com.capstone.contractmanagement.components.LocalizationUtils;
 import com.capstone.contractmanagement.dtos.user.*;
 import com.capstone.contractmanagement.entities.Department;
 import com.capstone.contractmanagement.entities.Role;
 import com.capstone.contractmanagement.entities.Token;
 import com.capstone.contractmanagement.entities.User;
 import com.capstone.contractmanagement.exceptions.DataNotFoundException;
 import com.capstone.contractmanagement.exceptions.InvalidParamException;
 import com.capstone.contractmanagement.exceptions.PermissionDenyException;
 import com.capstone.contractmanagement.repositories.IDepartmentRepository;
 import com.capstone.contractmanagement.repositories.IRoleRepository;
 import com.capstone.contractmanagement.repositories.ITokenRepository;
 import com.capstone.contractmanagement.repositories.IUserRepository;
 import com.capstone.contractmanagement.responses.User.UserListCustom;
 import com.capstone.contractmanagement.responses.User.UserResponse;
 import com.capstone.contractmanagement.services.sendmails.IMailService;
 import com.capstone.contractmanagement.utils.MessageKeys;
 import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
 import jakarta.transaction.Transactional;
 import jakarta.validation.Valid;
 import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
 import org.springframework.data.domain.Pageable;
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
    private final IDepartmentRepository departmentRepository;

    @Override
    @Transactional
    public User registerUser( @Valid @RequestBody CreateUserDTO userDTO) throws Exception {
        String phoneNumber = userDTO.getPhoneNumber();
        if (phoneNumber != null && UserRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DataIntegrityViolationException(
                    (MessageKeys.PHONE_NUMBER_ALREADY_EXISTS));
        }

        String email = userDTO.getEmail();
        if (UserRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException(
                    (MessageKeys.EMAIL_ALREADY_EXISTS));
        }

        // Use the default roleId = 2 if none is provided
        Long roleId = userDTO.getRoleId();
        Role role = RoleRepository.findById(roleId)
                .orElseThrow(() -> new DataNotFoundException(
                        (MessageKeys.ROLE_DOES_NOT_EXISTS)));

        // Prevent registration of an Admin account
        if (role.getRoleName().equalsIgnoreCase("ADMIN")) {
            throw new PermissionDenyException("Khoon");
        }

        // **MỚI**: Không cho tạo thêm Director nếu đã có sẵn
        if (Role.DIRECTOR.equalsIgnoreCase(role.getRoleName())
                && UserRepository.existsByRole_RoleName(Role.DIRECTOR)) {
            throw new PermissionDenyException(
                    ("Đã có Giám đốc trong hệ thống."));
        }

        Department department = departmentRepository.findById(userDTO.getDepartmentId())
                .orElseThrow(() -> new DataNotFoundException(
                        (MessageKeys.DEPARTMENT_NOT_FOUND)));

        // Create a new user instance
        User newUser = User.builder()
                .email(userDTO.getEmail())
                .phoneNumber(userDTO.getPhoneNumber())
                .fullName(userDTO.getFullName())
                .active(true)
                .address(userDTO.getAddress())
                .department(department)
                .DateOfBirth(userDTO.getDateOfBirth())
                .build();
        newUser.setRole(role);

        newUser.setStaffCode(generateStaffCode(department));
        // Always generate a random password to ensure a non-null value
        String generatedPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(generatedPassword);
        newUser.setPassword(encodedPassword);

        // Send the generated password via email (if needed)
        mailService.sendAccountPassword(newUser.getEmail(), generatedPassword);

        // Save the new user
        User user = UserRepository.save(newUser);
        return user;
    }

    /**
     * Generates a unique staff code based on the department and a random 6-digit number.
     */
    private String generateStaffCode(Department department) {
        // Extract the first two letters of the department name
        String departmentCode = department != null ? department.getDepartmentName().substring(0, 2).toUpperCase() : "XX";

        // Generate a 6-digit random number
        String randomDigits = String.format("%06d", new Random().nextInt(999999));

        // Combine the department code and the random 6-digit number
        return departmentCode + randomDigits;
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

            if (existingUser.getFailedLoginAttempts() == null) {
                existingUser.setFailedLoginAttempts(0);
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
    public User getUser() throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return UserRepository.findById(currentUser.getId()).orElseThrow(() -> new DataNotFoundException("User not found"));
    }

    @Override
    public void deleteUser(Long userId) {
        Optional<User> optionalUser = UserRepository.findById(userId);
        List<Token> tokens = TokenRepository.findByUserId(userId);
        TokenRepository.deleteAll(tokens);
        optionalUser.ifPresent(UserRepository::delete);
    }
    @org.springframework.transaction.annotation.Transactional(rollbackFor = {DataNotFoundException.class, DataIntegrityViolationException.class})
    @Override
    public void updateUser(Long userId, UpdateUserDTO userDTO) throws DataNotFoundException {
        // Kiểm tra xem user có tồn tại không
        User user = UserRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));

        // Kiểm tra số điện thoại: chỉ khi số điện thoại mới khác với số hiện tại mới thực hiện kiểm tra trùng lặp
        String phoneNumber = userDTO.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.equals(user.getPhoneNumber()) && UserRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DataIntegrityViolationException(
                    (MessageKeys.PHONE_NUMBER_ALREADY_EXISTS));
        }

        // Kiểm tra email: chỉ khi email mới khác với email hiện tại mới thực hiện kiểm tra trùng lặp
        String email = userDTO.getEmail();
        if (email != null && !email.equals(user.getEmail()) && UserRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException(
                    (MessageKeys.EMAIL_ALREADY_EXISTS));
        }

        // Nếu có departmentId thì kiểm tra và cập nhật
        if (userDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userDTO.getDepartmentId())
                    .orElseThrow(() -> new DataNotFoundException(MessageKeys.DEPARTMENT_NOT_FOUND));
            user.setDepartment(department);
        }

        // Cập nhật thông tin của user
        user.setFullName(userDTO.getFullName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setEmail(userDTO.getEmail());
        user.setGender(userDTO.getGender());
        user.setAddress(userDTO.getAddress());
        user.setDateOfBirth(userDTO.getDateOfBirth());
        user.setRole(RoleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.ROLE_DOES_NOT_EXISTS)));

        // Lưu thông tin cập nhật của user
        UserRepository.save(user);
    }

    @Override
    public User getUserDetailsFromRefreshToken(String refreshToken) throws Exception {
        Token existingToken = TokenRepository.findByRefreshToken(refreshToken);
        return getUserDetailsFromToken(existingToken.getToken());
    }

    @Override
    public Page<UserResponse> getAllUsers(int page, int size, Long departmentId, Long roleId, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasDepartment = departmentId != null;
        boolean hasRole = roleId != null;

        if (hasSearch) {
            if (hasDepartment && hasRole) {
                userPage = UserRepository.searchByDepartmentAndRoleAndFullNameOrStaffCode(departmentId, roleId, search, pageable);
            } else if (hasDepartment) {
                userPage = UserRepository.searchByDepartmentAndFullNameOrStaffCode(departmentId, search, pageable);
            } else if (hasRole) {
                userPage = UserRepository.searchByRoleAndFullNameOrStaffCode(roleId, search, pageable);
            } else {
                userPage = UserRepository.searchByFullNameOrStaffCode(search, pageable);
            }
        } else {
            if (hasDepartment && hasRole) {
                userPage = UserRepository.findByDepartment_IdAndRole_Id(departmentId, roleId, pageable);
            } else if (hasDepartment) {
                userPage = UserRepository.findByDepartment_Id(departmentId, pageable);
            } else if (hasRole) {
                userPage = UserRepository.findByRole_Id(roleId, pageable);
            } else {
                userPage = UserRepository.findAll(pageable);
            }
        }
        return userPage.map(UserResponse::fromUser);
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
                // Kiểm tra định dạng ảnh
                MediaType mediaType = MediaType.parseMediaType(Objects.requireNonNull(avatar.getContentType()));
                if (!mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) &&
                        !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
                    throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_IMAGES_FILE_MUST_BE_IMAGE));
                }

                // Tạo tên duy nhất cho avatar theo userId
                String publicId = "avatar_" + id;

                // Upload avatar lên Cloudinary, ghi đè nếu đã có
                Map uploadResult = cloudinary.uploader().upload(
                        avatar.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "user_avatar/" + id,
                                "public_id", publicId,
                                "overwrite", true,
                                "resource_type", "image"
                        )
                );

                // Lấy URL ảnh mới
                String avatarUrl = uploadResult.get("secure_url").toString();
                user.setAvatar(avatarUrl);

                // Cập nhật user
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

    @Override
    public Page<UserResponse> getAllStaffAndManager(String roleName, Pageable pageable) {
        Page<User> usersPage;

        if (roleName == null || roleName.isBlank()) {
            List<String> roleNames = List.of(Role.STAFF, Role.MANAGER);
            usersPage = UserRepository.findByRole_RoleNameInAndActiveTrue(roleNames, pageable);
        } else {
            usersPage = UserRepository.findByRole_RoleNameAndActiveTrue(roleName.toUpperCase(), pageable);
        }

        return usersPage.map(UserResponse::fromUser);
    }

    @Override
    public List<UserListCustom> getAll() {
        List<User> userList = UserRepository.findAll();
        return userList.stream()
                .map(user -> UserListCustom.builder()
                        .fullName(user.getFullName())
                        .userId(user.getId())
                        .department(user.getDepartment())
                        .build())
                .collect(Collectors.toList());
    }
}
