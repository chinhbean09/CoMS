package com.capstone.contractmanagement.services.user;

import com.capstone.contractmanagement.dtos.user.*;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.User.UserListCustom;
import com.capstone.contractmanagement.responses.User.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IUserService {
    com.capstone.contractmanagement.entities.User registerUser(CreateUserDTO userDTO) throws Exception;

    String login(UserLoginDTO userLoginDTO) throws Exception;

     User getUserDetailsFromToken(String token) throws DataNotFoundException;

    void blockOrEnable(Long userId, Boolean active) throws Exception;

    User getUser() throws DataNotFoundException;

    User getUserById(Long userId) throws DataNotFoundException;

    void deleteUser(Long userId);

    void updateUser(Long userId, UpdateUserDTO userDTO) throws Exception;

    User getUserDetailsFromRefreshToken(String refreshToken) throws Exception;

    Page<UserResponse> getAllUsers(int page, int size, Long departmentId, Long roleId, String search);

    void updatePassword(String email, String password) throws DataNotFoundException;

    User updateUserAvatar(long id, MultipartFile avatar);
    void changePassword(Long id, UpdatePasswordDTO changePasswordDTO) throws DataNotFoundException;

    Page<UserResponse> getAllStaffAndManager(String roleName, Pageable pageable);

    List<UserListCustom> getAll();


}
