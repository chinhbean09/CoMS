package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.DepartmentList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phone);

    Optional<User> findByFullName(String fullName);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.role.id = ?1")
    List<User> findByRoleId(Long roleId);
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR u.fullName ILIKE %:keyword%)")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    Page<User> findByRole_RoleNameIn(List<String> roleNames, Pageable pageable);

    // Lấy tất cả người dùng có phân trang
    Page<User> findAll(Pageable pageable);

    // Lọc theo department có phân trang
    Page<User> findByDepartment(DepartmentList department, Pageable pageable);

    // Tìm kiếm theo fullName (không phân biệt hoa thường) có phân trang
    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    // Lọc theo department và tìm kiếm theo fullName có phân trang
    Page<User> findByDepartmentAndFullNameContainingIgnoreCase(DepartmentList department, String fullName, Pageable pageable);
}
