package com.capstone.contractmanagement.repositories;
import com.capstone.contractmanagement.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRoleRepository extends JpaRepository<Role, Long> {

    com.capstone.contractmanagement.entities.Role findByRoleName(String roleUser);
}
