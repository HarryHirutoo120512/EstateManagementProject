package com.javaweb.service.impl;

import com.javaweb.converter.BuildingDTOConverter;
import com.javaweb.converter.CustomerConverter;
import com.javaweb.converter.RoleConverter;
import com.javaweb.converter.TransactionConverter;
import com.javaweb.entity.CustomerEntity;
import com.javaweb.entity.RoleEntity;
import com.javaweb.entity.TransactionEntity;
import com.javaweb.model.event.ActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.javaweb.model.dto.AssignmentBuildingDTO;
import com.javaweb.model.dto.AssignmentCustomerDTO;
import com.javaweb.model.dto.MyUserDetail;
import com.javaweb.model.dto.RoleDTO;
import com.javaweb.model.dto.TransactionTypeDTO;
import com.javaweb.model.dto.UserDTO;
import com.javaweb.model.request.BuildingSearchRequest;
import com.javaweb.model.request.CustomerSearchRequest;
import com.javaweb.model.response.TransactionResponseDTO;
import com.javaweb.repository.AssignmentBuildingRepository;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.CustomerRepository;
import com.javaweb.repository.RoleRepository;
import com.javaweb.repository.TransactionRepository;
import com.javaweb.repository.UserRepository;
import com.javaweb.service.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceAuthorizationTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssignmentBuildingRepository assignmentBuildingRepository;
    @Mock
    private BuildingDTOConverter buildingDTOConverter;
    @Mock
    private CustomerConverter customerConverter;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionConverter transactionConverter;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleConverter roleConverter;
    @Mock
    private IUserService iUserService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BuildingServiceImpl buildingService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @InjectMocks
    private UserService userService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @InjectMocks
    private RoleService roleService;

    @InjectMocks
    private CustomUserDetailService customUserDetailService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockLoggedInUser(Long userId, String username, String roleCode) {
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode));
        MyUserDetail userDetail = new MyUserDetail(username, "pass", true, true, true, true, authorities);
        userDetail.setId(userId);
        userDetail.setFullName(username);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetail, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("1. Nhân viên (ROLE_STAFF) tìm kiếm tòa nhà -> Tự động khóa staffId theo ID của chính nhân viên đó")
    void buildingFindAll_WhenStaffRole_ForcesOwnStaffId() {
        mockLoggedInUser(7L, "staff_a", "STAFF");

        BuildingSearchRequest request = new BuildingSearchRequest();
        request.setStaffId(99L); // Cố tình truyền staffId của người khác

        when(buildingRepository.findAll(any(BuildingSearchRequest.class), any(Pageable.class))).thenReturn(Collections.emptyList());

        buildingService.findAll(request, PageRequest.of(0, 5));

        assertEquals(7L, request.getStaffId());
    }

    @Test
    @DisplayName("2. Nhân viên (ROLE_STAFF) tìm kiếm khách hàng -> Tự động khóa staffId theo ID của chính nhân viên đó")
    void customerFindAll_WhenStaffRole_ForcesOwnStaffId() {
        mockLoggedInUser(7L, "staff_a", "STAFF");

        CustomerSearchRequest request = new CustomerSearchRequest();
        request.setStaffId(null);

        when(customerRepository.findAll(any(CustomerSearchRequest.class), any(Pageable.class))).thenReturn(Collections.emptyList());

        customerService.findAll(request, PageRequest.of(0, 5));

        assertEquals(7L, request.getStaffId());
    }

    @Test
    @DisplayName("3. Quản lý (ROLE_MANAGER) tìm kiếm tòa nhà -> Giữ nguyên bộ lọc staffId tùy chọn")
    void buildingFindAll_WhenManagerRole_KeepsFilterStaffId() {
        mockLoggedInUser(1L, "manager", "MANAGER");

        BuildingSearchRequest request = new BuildingSearchRequest();
        request.setStaffId(7L);

        when(buildingRepository.findAll(any(BuildingSearchRequest.class), any(Pageable.class))).thenReturn(Collections.emptyList());

        buildingService.findAll(request, PageRequest.of(0, 5));

        assertEquals(7L, request.getStaffId());
    }

    @Test
    @DisplayName("4. Nhân viên (ROLE_STAFF) cố tình gọi xóa tòa nhà -> Bị chặn với AccessDeniedException")
    void deleteBuildings_WhenStaffRole_ThrowsAccessDeniedException() {
        mockLoggedInUser(7L, "staff_a", "STAFF");

        assertThrows(AccessDeniedException.class,
                () -> buildingService.deleteBuildings(Collections.singletonList(1L)));
        verify(buildingRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("5. Nhân viên (ROLE_STAFF) cố tình gọi xóa khách hàng -> Bị chặn với AccessDeniedException")
    void deleteCustomers_WhenStaffRole_ThrowsAccessDeniedException() {
        mockLoggedInUser(7L, "staff_a", "STAFF");

        assertThrows(AccessDeniedException.class,
                () -> customerService.deleteCustomers(Collections.singletonList(10L)));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Nhân viên (ROLE_STAFF) cố tình phân công tòa nhà hoặc khách hàng -> Bị chặn với AccessDeniedException")
    void updateAssignment_WhenStaffRole_ThrowsAccessDeniedException() {
        mockLoggedInUser(7L, "staff_a", "STAFF");

        AssignmentBuildingDTO buildingDTO = new AssignmentBuildingDTO();
        buildingDTO.setBuildingId(1L);
        assertThrows(AccessDeniedException.class,
                () -> userService.updateAssignmentBuilding(buildingDTO));

        AssignmentCustomerDTO customerDTO = new AssignmentCustomerDTO();
        customerDTO.setCustomerId(10L);
        assertThrows(AccessDeniedException.class,
                () -> userService.updateAssignmentCustomer(customerDTO));
    }

    @Test
    @DisplayName("7. Kiểm thử CustomUserDetailService, RoleService và TransactionServiceImpl")
    void additionalServices_SuccessAndCoverage() {
        // 1. CustomUserDetailService
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setCode("MANAGER");
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUserName("admin");
        userDTO.setPassword("hashed");
        userDTO.setRoles(Collections.singletonList(roleDTO));

        when(iUserService.findOneByUserNameAndStatus("admin", 1)).thenReturn(userDTO);
        UserDetails details = customUserDetailService.loadUserByUsername("admin");
        assertEquals("admin", details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));

        when(iUserService.findOneByUserNameAndStatus("unknown", 1)).thenReturn(null);
        assertThrows(UsernameNotFoundException.class, () -> customUserDetailService.loadUserByUsername("unknown"));

        // 2. RoleService
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setCode("STAFF");
        roleEntity.setName("Nhân viên");
        RoleDTO mappedRole = new RoleDTO();
        mappedRole.setCode("STAFF");
        mappedRole.setName("Nhân viên");

        when(roleRepository.findAll()).thenReturn(Collections.singletonList(roleEntity));
        when(roleConverter.convertToDto(roleEntity)).thenReturn(mappedRole);

        assertEquals(1, roleService.findAll().size());
        Map<String, String> roleMap = roleService.getRoles();
        assertEquals("Nhân viên", roleMap.get("STAFF"));

        // 3. TransactionServiceImpl
        TransactionTypeDTO txDto = new TransactionTypeDTO();
        txDto.setCustomerId(10L);
        txDto.setNote("Dẫn khách đi xem tầng 3");

        TransactionEntity txEntity = new TransactionEntity();
        txEntity.setNote("Dẫn khách đi xem tầng 3");

        CustomerEntity customer = new CustomerEntity();
        customer.setId(10L);

        when(transactionConverter.transactionEntity(txDto)).thenReturn(txEntity);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        transactionService.addOrUpdateTransactions(txDto);
        verify(transactionRepository, times(1)).save(txEntity);

        when(transactionRepository.findByIdAndCustomerId(5L, 10L)).thenReturn(txEntity);
        TransactionResponseDTO loaded = transactionService.loadTransaction(5L, 10L);
        assertEquals("Dẫn khách đi xem tầng 3", loaded.getNote());
    }
}
