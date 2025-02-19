package com.capstone.contractmanagement.user;

import com.capstone.contractmanagement.components.JwtTokenUtils;
import com.capstone.contractmanagement.components.LocalizationUtils;
import com.capstone.contractmanagement.dtos.user.UserDTO;
import com.capstone.contractmanagement.dtos.user.UserLoginDTO;
import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.Token;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.InvalidParamException;
import com.capstone.contractmanagement.exceptions.PermissionDenyException;
import com.capstone.contractmanagement.repositories.IRoleRepository;
import com.capstone.contractmanagement.repositories.ITokenRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.services.user.UserService;
import com.capstone.contractmanagement.utils.MessageKeys;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {
    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private ITokenRepository tokenRepository;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LocalizationUtils localizationUtils;

    @Mock
    private Cloudinary cloudinary;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO userDTO;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(2L);
        role.setRoleName("USER");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPhoneNumber("123456789");
        user.setPassword("encodedPassword");
        user.setRole(role);
        user.setActive(true);

        userDTO = new UserDTO();
        userDTO.setEmail("test@example.com");
        userDTO.setPhoneNumber("123456789");
        userDTO.setPassword("password123");
        userDTO.setRoleId(2L);
    }

//    @Test
//    void registerUser_Success() throws Exception {
//        when(userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())).thenReturn(false);
//        when(userRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
//        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
//        when(passwordEncoder.encode(userDTO.getPassword())).thenReturn("encodedPassword");
//        when(userRepository.save(any(User.class))).thenReturn(user);
//
//        User registeredUser = userService.registerUser(userDTO);
//        assertNotNull(registeredUser);
//        assertEquals("test@example.com", registeredUser.getEmail());
//    }
//
//    @Test
//    void registerUser_EmailAlreadyExists() {
//        when(userRepository.existsByEmail(userDTO.getEmail())).thenReturn(true);
//        assertThrows(Exception.class, () -> userService.registerUser(userDTO));
//    }

    @Test
    void getUserDetailsFromToken_UserExists() throws Exception {
        when(jwtTokenUtils.isTokenExpired(anyString())).thenReturn(false);
        when(jwtTokenUtils.extractIdentifier(anyString())).thenReturn(Map.of("email", "test@example.com"));
        when(userRepository.findByEmailOrPhoneNumber(anyString(), anyString())).thenReturn(Optional.of(user));

        User foundUser = userService.getUserDetailsFromToken("validToken");
        assertNotNull(foundUser);
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void getUserDetailsFromToken_UserNotFound() {
        when(jwtTokenUtils.isTokenExpired(anyString())).thenReturn(false);
        when(jwtTokenUtils.extractIdentifier(anyString())).thenReturn(Map.of("email", "test@example.com"));
        when(userRepository.findByEmailOrPhoneNumber(anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class, () -> userService.getUserDetailsFromToken("validToken"));
    }

    @Test
    void login_Success() throws Exception {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setLoginIdentifier("test@example.com");
        loginDTO.setPassword("password123");

        when(userRepository.findByEmailOrPhoneNumber(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenUtils.generateToken(any(User.class))).thenReturn("jwtToken");

        String token = userService.login(loginDTO);
        assertNotNull(token);
        assertEquals("jwtToken", token);
    }

    @Test
    void login_WrongPassword() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setLoginIdentifier("test@example.com");
        loginDTO.setPassword("wrongPassword");

        when(userRepository.findByEmailOrPhoneNumber(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(Exception.class, () -> userService.login(loginDTO));
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);
        doNothing().when(tokenRepository).deleteAll(anyList());

        assertDoesNotThrow(() -> userService.deleteUser(1L));
    }

//    @Test
//    void testUploadAvatar_Success() throws IOException {
//        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.jpg", "image/jpeg", "test image content".getBytes());
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//        when(cloudinary.uploader().upload(any(byte[].class), any(Map.class)))
//                .thenReturn(Map.of("secure_url", "https://cloudinary.com/avatar.jpg"));
//
//        User updatedUser = userService.updateUserAvatar(1L, file);
//
//        assertNotNull(updatedUser);
//        assertEquals("https://cloudinary.com/avatar.jpg", updatedUser.getAvatar());
//        verify(userRepository).save(user);
//    }
//
//    @Test
//    void testUploadAvatar_InvalidFileType() {
//        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.txt", "text/plain", "invalid file content".getBytes());
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//
//        Exception exception = assertThrows(InvalidParamException.class, () -> {
//            userService.updateUserAvatar(1L, file);
//        });
//
//        assertTrue(exception.getMessage().contains("UPLOAD_IMAGES_FILE_MUST_BE_IMAGE"));
//        verify(userRepository, never()).save(any(User.class));
//    }

    @Test
    void testUploadAvatar_UserNotFound() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.jpg", "image/jpeg", "test image content".getBytes());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        User updatedUser = userService.updateUserAvatar(1L, file);

        assertNull(updatedUser);
        verify(userRepository, never()).save(any(User.class));
    }
}
