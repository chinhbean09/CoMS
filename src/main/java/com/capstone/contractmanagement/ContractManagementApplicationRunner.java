package com.capstone.contractmanagement;

import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.template.Section;
import com.capstone.contractmanagement.repositories.IRoleRepository;
import com.capstone.contractmanagement.repositories.ISectionRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class ContractManagementApplicationRunner implements ApplicationRunner {
    @Autowired
    private com.capstone.contractmanagement.repositories.IUserRepository IUserRepository;

    @Autowired
    private ISectionRepository sectionRepository;

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

    public void initializeSections() {
        List<Section> sections = List.of(
                Section.builder().sectionName("Tiêu đề hợp đồng").order(1).isCustom(false).build(),
                Section.builder().sectionName("Căn cứ pháp lý").order(2).isCustom(false).build(),
                Section.builder().sectionName("Thông tin các bên tham gia").order(3).isCustom(false).build(),
                Section.builder().sectionName("Nội dung hợp đồng").order(4).isCustom(false).build(),
                Section.builder().sectionName("Giá trị hợp đồng và phương thức thanh toán").order(5).isCustom(false).build(),
                Section.builder().sectionName("Thời gian thực hiện hợp đồng").order(6).isCustom(false).build(),
                Section.builder().sectionName("Quyền và nghĩa vụ của các bên").order(7).isCustom(false).build(),
                Section.builder().sectionName("Điều khoản về bảo hành, bảo trì (nếu có)").order(8).isCustom(false).build(),
                Section.builder().sectionName("Điều khoản về vi phạm và bồi thường thiệt hại").order(9).isCustom(false).build(),
                Section.builder().sectionName("Điều khoản về chấm dứt hợp đồng").order(10).isCustom(false).build(),
                Section.builder().sectionName("Giải quyết tranh chấp").order(11).isCustom(false).build(),
                Section.builder().sectionName("Hiệu lực hợp đồng").order(12).isCustom(false).build(),
                Section.builder().sectionName("Cam kết chung").order(13).isCustom(false).build(),
                Section.builder().sectionName("Chữ ký, con dấu của các bên").order(14).isCustom(false).build()

        );

        sectionRepository.saveAll(sections);
        System.out.println("Sections initialized successfully!");
    }

    @Override
    public void run(ApplicationArguments args) {


        Optional<User> findAccountResult = IUserRepository.findByPhoneNumber(phoneNumber);
        Optional<Role> existRolePermission = IRoleRepository.findById((long) 1);
        Optional<User> findAccountGuest = IUserRepository.findByPhoneNumber(guestPhoneNumber);

        initializeSections();


        Role AdminRole = Role.builder()
                .id(1L)
                .roleName("ADMIN")
                .build();
        Role ManagerRole = Role.builder()
                .id(2L)
                .roleName("MANAGER")
                .build();
        Role StaffRole = Role.builder()
                .id(3L)
                .roleName("STAFF")
                .build();

        if (existRolePermission.isEmpty()) {
            System.out.println("There is no role Initialing...!");
        }

        IRoleRepository.save(AdminRole);
        IRoleRepository.save(ManagerRole);
        IRoleRepository.save(StaffRole);


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
            user.setRole(ManagerRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("Manager initialized!");
        }

        System.out.println("Hello, I'm System Manager!");
    }
}
