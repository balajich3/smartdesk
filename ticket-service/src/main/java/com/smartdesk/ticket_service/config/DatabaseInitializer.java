package com.smartdesk.ticket_service.config;

import com.smartdesk.ticket_service.entity.Department;
import com.smartdesk.ticket_service.entity.Role;
import com.smartdesk.ticket_service.repository.DepartmentRepository;
import com.smartdesk.ticket_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
public class DatabaseInitializer implements ApplicationRunner  {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        initializeRoles();
        initializeDepartments();
    }

    private void initializeRoles() {

        List<String> roles = List.of(
                "ADMIN",
                "EMPLOYEE",
                "SUPPORT_AGENT",
                "MANAGER"
        );

        for (String roleName : roles) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(
                        Role.builder()
                                .name(roleName)
                                .build()
                );
            }
        }
    }

    private void initializeDepartments() {

        List<Department> departments = List.of(
                Department.builder()
                        .name("Information Technology")
                        .code("IT")
                        .active(true)
                        .build(),

                Department.builder()
                        .name("Human Resources")
                        .code("HR")
                        .active(true)
                        .build(),

                Department.builder()
                        .name("Facilities")
                        .code("FACILITIES")
                        .active(true)
                        .build()
        );

        for (Department department : departments) {
            if (!departmentRepository.existsByCode(department.getCode())) {
                departmentRepository.save(department);
            }
        }
    }
}