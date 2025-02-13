package com.capstone.contractmanagement;

import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.TypeTerm;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.repositories.IRoleRepository;
import com.capstone.contractmanagement.repositories.ITypeTermRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ContractManagementApplicationRunner implements ApplicationRunner {
    @Autowired
    private IUserRepository IUserRepository;

    @Autowired
    private IRoleRepository IRoleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ITypeTermRepository typeTermRepository;

    @Value("${contract.admin.email}")
    private String email;

    @Value("${contract.manager.email}")
    private String managerEmail;

    @Value("${contract.staff.email}")
    private String staffEmail;

    @Value("${contract.admin.fullName}")
    private String fullName;

    @Value("${contract.manager.fullName}")
    private String managerFullName;

    @Value("${contract.staff.fullName}")
    private String staffFullName;

    @Value("${contract.admin.address}")
    private String address;

    @Value("${contract.manager.address}")
    private String managerAddress;

    @Value("${contract.manager.address}")
    private String staffAddress;

    @Value("${contract.admin.phoneNumber}")
    private String phoneNumber;

    @Value("${contract.manager.phoneNumber}")
    private String managerPhoneNumber;

    @Value("${contract.staff.phoneNumber}")
    private String staffPhoneNumber;

    @Value("${contract.admin.password}")
    private String password;

    @Value("${contract.admin.active}")
    private Boolean active;

//    public void initializeSections() {
//
//        if (sectionRepository.count() > 0) {
//            System.out.println("Sections already initialized!");
//            return;
//        }
//
//        List<Section> sections = List.of(
//                Section.builder().sectionName("Tiêu đề hợp đồng").order(1).isCustom(false).build(),
//                Section.builder().sectionName("Căn cứ pháp lý").order(2).isCustom(false).build(),
//                Section.builder().sectionName("Thông tin các bên tham gia").order(3).isCustom(false).build(),
//                Section.builder().sectionName("Nội dung hợp đồng").order(4).isCustom(false).build(),
//                Section.builder().sectionName("Giá trị hợp đồng và phương thức thanh toán").order(5).isCustom(false).build(),
//                Section.builder().sectionName("Thời gian thực hiện hợp đồng").order(6).isCustom(false).build(),
//                Section.builder().sectionName("Quyền và nghĩa vụ của các bên").order(7).isCustom(false).build(),
//                Section.builder().sectionName("Điều khoản về bảo hành, bảo trì (nếu có)").order(8).isCustom(false).build(),
//                Section.builder().sectionName("Điều khoản về vi phạm và bồi thường thiệt hại").order(9).isCustom(false).build(),
//                Section.builder().sectionName("Điều khoản về chấm dứt hợp đồng").order(10).isCustom(false).build(),
//                Section.builder().sectionName("Giải quyết tranh chấp").order(11).isCustom(false).build(),
//                Section.builder().sectionName("Hiệu lực hợp đồng").order(12).isCustom(false).build(),
//                Section.builder().sectionName("Cam kết chung").order(13).isCustom(false).build(),
//                Section.builder().sectionName("Chữ ký, con dấu của các bên").order(14).isCustom(false).build()
//
//        );
//
//        sectionRepository.saveAll(sections);
//        System.out.println("Sections initialized successfully!");
//    }

    public void initializeTypeTerms() {
        if (typeTermRepository.count() > 0) {
            System.out.println("Type terms already initialized!");
            return;
        }
        //generalTerms, additionalTerms, otherTerms, legalbasis
        List<TypeTerm> typeTerms = List.of(
                // Additional Terms
                TypeTerm.builder().name("Điều khoản thêm").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Quyền và nghĩa vụ").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Bảo hành và bảo trì").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Vi phạm và thiệt hại").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Chấm dứt hợp đồng").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản giải quyết tranh chấp").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Chính sách bảo mật").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),

                // Legal basis
                TypeTerm.builder().name("Căn cứ pháp lí").identifier(TypeTermIdentifier.LEGAL_BASIS).build(),

                // General terms
                TypeTerm.builder().name("Điều khoản chung").identifier(TypeTermIdentifier.GENERAL_TERMS).build(),

                // Other terms
                TypeTerm.builder().name("Các điều khoản khác").identifier(TypeTermIdentifier.OTHER_TERMS).build()
        );

        typeTermRepository.saveAll(typeTerms);
        System.out.println("Type terms initialized successfully!");
    }

    @Override
    public void run(ApplicationArguments args) {


        Optional<User> findAccountResult = IUserRepository.findByPhoneNumber(phoneNumber);
        Optional<Role> existRolePermission = IRoleRepository.findById((long) 1);
        Optional<User> findAccountManager = IUserRepository.findByPhoneNumber(managerPhoneNumber);
        Optional<User> findAccountStaff = IUserRepository.findByPhoneNumber(staffPhoneNumber);

        // Initialize type terms
        initializeTypeTerms();


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

        if (findAccountManager.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);

            User user = new User();
            user.setEmail(managerEmail);
            user.setAddress(managerAddress);
            user.setPassword(encodedPassword);
            user.setActive(active);
            user.setFullName(managerFullName);
            user.setPhoneNumber(managerPhoneNumber);
            user.setRole(ManagerRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("Manager initialized!");
        }

        if (findAccountStaff.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);

            User user = new User();
            user.setEmail(staffEmail);
            user.setAddress(staffAddress);
            user.setPassword(encodedPassword);
            user.setActive(active);
            user.setFullName(staffFullName);
            user.setPhoneNumber(staffPhoneNumber);
            user.setRole(StaffRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("Staff initialized!");
        }

        System.out.println("Hello, I'm System Manager!");
    }
}
