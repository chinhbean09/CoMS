package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Role;
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

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByRole_RoleName(String roleName);

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);

    Page<User> findByRole_RoleNameInAndActiveTrue(List<String> roleNames, Pageable pageable);

    Page<User> findByRole_RoleNameAndActiveTrue(String roleName, Pageable pageable);

    Page<User> findAll(Pageable pageable);

    // Lọc theo department có phân trang
    Page<User> findByDepartment(DepartmentList department, Pageable pageable);

    // Lọc theo department và tìm kiếm theo fullName có phân trang
    Page<User> findByDepartmentAndFullNameContainingIgnoreCase(String department, String fullName, Pageable pageable);

    Page<User> findByDepartment_Id(Long departmentId, Pageable pageable);

    Page<User> findByRole_Id(Long roleId, Pageable pageable);

    Page<User> findByDepartment_IdAndRole_Id(Long departmentId, Long roleId, Pageable pageable);

    // Search toàn bộ theo fullName hoặc staffCode
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.staffCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByFullNameOrStaffCode(@Param("search") String search, Pageable pageable);

    // Tìm theo department và tìm kiếm theo fullName hoặc staffCode
    @Query("SELECT u FROM User u WHERE u.department.id = :deptId " +
            "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.staffCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByDepartmentAndFullNameOrStaffCode(@Param("deptId") Long deptId,
                                                        @Param("search") String search,
                                                        Pageable pageable);

    // Tìm theo role và tìm kiếm theo fullName hoặc staffCode
    @Query("SELECT u FROM User u WHERE u.role.id = :roleId " +
            "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.staffCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByRoleAndFullNameOrStaffCode(@Param("roleId") Long roleId,
                                                  @Param("search") String search,
                                                  Pageable pageable);

    // Tìm theo department và role và tìm kiếm theo fullName hoặc staffCode
    @Query("SELECT u FROM User u WHERE u.department.id = :deptId AND u.role.id = :roleId " +
            "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.staffCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByDepartmentAndRoleAndFullNameOrStaffCode(@Param("deptId") Long deptId,
                                                               @Param("roleId") Long roleId,
                                                               @Param("search") String search,
                                                               Pageable pageable);
    boolean existsByRoleAndIdNot(Role role, Long id);

}
