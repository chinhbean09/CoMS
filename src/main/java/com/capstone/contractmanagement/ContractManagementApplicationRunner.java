package com.capstone.contractmanagement;

import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.repositories.IRoleRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Component
public class ContractManagementApplicationRunner implements ApplicationRunner {
    @Autowired
    private com.capstone.contractmanagement.repositories.IUserRepository IUserRepository;

    @Autowired
    private IRoleRepository IRoleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${contract.admin.email}")
    private String email;

    @Value("${contract.guest.email}")
    private String guestEmail;

    @Value("${contract.admin.fullName}")
    private String fullName;

    @Value("${contract.guest.fullName}")
    private String guestFullName;

    @Value("${contract.admin.address}")
    private String address;

    @Value("${contract.guest.address}")
    private String guestAddress;

    @Value("${contract.admin.phoneNumber}")
    private String phoneNumber;

    @Value("${contract.guest.phoneNumber}")
    private String guestPhoneNumber;


    @Value("${contract.admin.gender}")
    private String gender;

    @Value("${contract.admin.password}")
    private String password;

    @Value("${contract.admin.active}")
    private Boolean active;

    @Override
    public void run(ApplicationArguments args) {
        Optional<User> findAccountResult = IUserRepository.findByPhoneNumber(phoneNumber);
        Optional<Role> existRolePermission = IRoleRepository.findById((long) 1);
        Optional<User> findAccountGuest = IUserRepository.findByPhoneNumber(guestPhoneNumber);


        Role AdminRole = Role.builder()
                .id(1L)
                .roleName("ADMIN")
                .build();
        Role CustomerRole = Role.builder()
                .id(2L)
                .roleName("CUSTOMER")
                .build();

        if (existRolePermission.isEmpty()) {
            System.out.println("There is no role Initialing...!");
        }

        IRoleRepository.save(AdminRole);
        IRoleRepository.save(CustomerRole);


        if (findAccountResult.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);

            User user = new User();
            user.setEmail(email);
            user.setAddress(address);
            user.setPassword(encodedPassword);
            user.setActive(active);
            user.setFullName(fullName);
            user.setPhoneNumber(phoneNumber);
            user.setRole(AdminRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("Admin initialized!");
        }

        if (findAccountGuest.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);

            User user = new User();
            user.setEmail(guestEmail);
            user.setAddress(guestAddress);
            user.setPassword(encodedPassword);
            user.setActive(active);
            user.setFullName(guestFullName);
            user.setPhoneNumber(guestPhoneNumber);
            user.setRole(CustomerRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("CUSTOMER initialized!");
        }

        System.out.println("Hello, I'm System Manager!");
    }
}
