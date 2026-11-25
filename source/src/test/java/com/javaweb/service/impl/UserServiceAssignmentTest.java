package com.javaweb.service.impl;

import com.javaweb.converter.UserConverter;
import com.javaweb.entity.AssignmentBuildingEntity;
import com.javaweb.entity.BuildingEntity;
import com.javaweb.entity.CustomerEntity;
import com.javaweb.entity.RoleEntity;
import com.javaweb.entity.UserEntity;
import com.javaweb.exception.MyException;
import com.javaweb.exception.ServiceException;
import com.javaweb.model.event.ActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.javaweb.model.dto.AssignmentBuildingDTO;
import com.javaweb.model.dto.AssignmentCustomerDTO;
import com.javaweb.model.dto.PasswordDTO;
import com.javaweb.model.dto.UserDTO;
import com.javaweb.repository.AssignmentBuildingRepository;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.CustomerRepository;
import com.javaweb.repository.RoleRepository;
import com.javaweb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAssignmentTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserConverter userConverter;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AssignmentBuildingRepository assignmentBuildingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private UserEntity createStaff(Long id, String fullName) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setFullName(fullName);
        user.setStatus(1);
        return user;
    }

    @Test
    @DisplayName("1. Giao tòa nhà cho nhân viên thành công -> Xóa phân công cũ và lưu đúng danh sách nhân viên mới")
    void updateAssignmentBuilding_Success() {
        BuildingEntity building = new BuildingEntity();
        building.setId(1L);
        building.setName("ACM Tower");

        UserEntity staff1 = createStaff(2L, "Nguyen Van A");
        UserEntity staff2 = createStaff(3L, "Tran Thi B");

        AssignmentBuildingDTO dto = new AssignmentBuildingDTO();
        dto.setBuildingId(1L);
        dto.setStaffs(Arrays.asList(2L, 3L));

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(userRepository.findAllById(Arrays.asList(2L, 3L))).thenReturn(Arrays.asList(staff1, staff2));

        userService.updateAssignmentBuilding(dto);

        verify(assignmentBuildingRepository, times(1)).deleteAssignmentBuildingEntityByBuilding(building);
        ArgumentCaptor<AssignmentBuildingEntity> captor = ArgumentCaptor.forClass(AssignmentBuildingEntity.class);
        verify(assignmentBuildingRepository, times(2)).save(captor.capture());

        List<AssignmentBuildingEntity> savedAssignments = captor.getAllValues();
        assertEquals(2, savedAssignments.size());
        assertEquals(building, savedAssignments.get(0).getBuilding());
        assertEquals(staff1, savedAssignments.get(0).getStaffs());
        assertEquals(staff2, savedAssignments.get(1).getStaffs());
    }

    @Test
    @DisplayName("2. Giao tòa nhà với danh sách nhân viên rỗng -> Chỉ xóa phân công cũ, không lưu mới")
    void updateAssignmentBuilding_EmptyStaffList_ClearsAssignments() {
        BuildingEntity building = new BuildingEntity();
        building.setId(1L);

        AssignmentBuildingDTO dto = new AssignmentBuildingDTO();
        dto.setBuildingId(1L);
        dto.setStaffs(Collections.emptyList());

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

        userService.updateAssignmentBuilding(dto);

        verify(assignmentBuildingRepository, times(1)).deleteAssignmentBuildingEntityByBuilding(building);
        verify(assignmentBuildingRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Giao tòa nhà nhưng buildingId không tồn tại -> Báo lỗi và không xóa phân công cũ")
    void updateAssignmentBuilding_BuildingNotFound_ThrowsException() {
        AssignmentBuildingDTO dto = new AssignmentBuildingDTO();
        dto.setBuildingId(999L);
        dto.setStaffs(Arrays.asList(2L, 3L));

        when(buildingRepository.findById(999L)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.updateAssignmentBuilding(dto));
        assertTrue(ex.getMessage().contains("999"));
        verify(assignmentBuildingRepository, never()).deleteAssignmentBuildingEntityByBuilding(any());
        verify(assignmentBuildingRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Giao tòa nhà nhưng danh sách staffIds bị trùng lặp -> Báo lỗi trùng giao")
    void updateAssignmentBuilding_DuplicateStaffIds_ThrowsException() {
        AssignmentBuildingDTO dto = new AssignmentBuildingDTO();
        dto.setBuildingId(1L);
        dto.setStaffs(Arrays.asList(2L, 2L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.updateAssignmentBuilding(dto));
        assertTrue(ex.getMessage().contains("trùng lặp"));
        verify(assignmentBuildingRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Giao tòa nhà nhưng có nhân viên không tồn tại trong DB -> Báo lỗi")
    void updateAssignmentBuilding_StaffNotFound_ThrowsException() {
        BuildingEntity building = new BuildingEntity();
        building.setId(1L);

        AssignmentBuildingDTO dto = new AssignmentBuildingDTO();
        dto.setBuildingId(1L);
        dto.setStaffs(Arrays.asList(2L, 99L));

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(userRepository.findAllById(Arrays.asList(2L, 99L)))
                .thenReturn(Collections.singletonList(createStaff(2L, "Nguyen Van A")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.updateAssignmentBuilding(dto));
        assertTrue(ex.getMessage().contains("không tồn tại"));
        verify(assignmentBuildingRepository, never()).deleteAssignmentBuildingEntityByBuilding(any());
        verify(assignmentBuildingRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Giao khách hàng cho nhân viên thành công -> Cập nhật danh sách nhân viên phụ trách")
    void updateAssignmentCustomer_Success() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(10L);
        customer.setFullName("Khach Hang VIP");

        UserEntity staff1 = createStaff(2L, "Nguyen Van A");
        UserEntity staff2 = createStaff(3L, "Tran Thi B");

        AssignmentCustomerDTO dto = new AssignmentCustomerDTO();
        dto.setCustomerId(10L);
        dto.setStaffs(Arrays.asList(2L, 3L));

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(userRepository.findAllById(Arrays.asList(2L, 3L))).thenReturn(Arrays.asList(staff1, staff2));

        userService.updateAssignmentCustomer(dto);

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(1)).save(captor.capture());
        CustomerEntity savedCustomer = captor.getValue();
        assertEquals(2, savedCustomer.getUserEntities().size());
        assertTrue(savedCustomer.getUserEntities().contains(staff1));
        assertTrue(savedCustomer.getUserEntities().contains(staff2));
    }

    @Test
    @DisplayName("7. Giao khách hàng nhưng customerId không tồn tại -> Báo lỗi")
    void updateAssignmentCustomer_CustomerNotFound_ThrowsException() {
        AssignmentCustomerDTO dto = new AssignmentCustomerDTO();
        dto.setCustomerId(888L);
        dto.setStaffs(Collections.singletonList(2L));

        when(customerRepository.findById(888L)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> userService.updateAssignmentCustomer(dto));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Giao khách hàng nhưng danh sách staffIds bị trùng -> Báo lỗi trùng giao")
    void updateAssignmentCustomer_DuplicateStaffIds_ThrowsException() {
        AssignmentCustomerDTO dto = new AssignmentCustomerDTO();
        dto.setCustomerId(10L);
        dto.setStaffs(Arrays.asList(3L, 3L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.updateAssignmentCustomer(dto));
        assertTrue(ex.getMessage().contains("trùng lặp"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("9. Giao khách hàng nhưng nhân viên không tồn tại -> Báo lỗi")
    void updateAssignmentCustomer_StaffNotFound_ThrowsException() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(10L);

        AssignmentCustomerDTO dto = new AssignmentCustomerDTO();
        dto.setCustomerId(10L);
        dto.setStaffs(Arrays.asList(2L, 404L));

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(userRepository.findAllById(Arrays.asList(2L, 404L)))
                .thenReturn(Collections.singletonList(createStaff(2L, "Nguyen Van A")));

        assertThrows(ServiceException.class, () -> userService.updateAssignmentCustomer(dto));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Lấy danh sách nhân viên đang hoạt động (getStaffs) -> Trả về Map<Id, FullName>")
    void getStaffs_ReturnsActiveStaffMap() {
        UserEntity s1 = createStaff(2L, "Sale A");
        UserEntity s2 = createStaff(3L, "Sale B");
        when(userRepository.findByStatusAndRoles_Code(1, "STAFF")).thenReturn(Arrays.asList(s1, s2));

        Map<Long, String> result = userService.getStaffs();

        assertEquals(2, result.size());
        assertEquals("Sale A", result.get(2L));
        assertEquals("Sale B", result.get(3L));
    }

    @Test
    @DisplayName("11. Đổi mật khẩu thành công khi mật khẩu cũ đúng và mật khẩu mới khớp")
    void updatePassword_Success() throws MyException {
        UserEntity user = createStaff(2L, "Sale A");
        user.setPassword("encodedOldPass");

        PasswordDTO passwordDTO = new PasswordDTO();
        passwordDTO.setOldPassword("rawOldPass");
        passwordDTO.setNewPassword("newPass123");
        passwordDTO.setConfirmPassword("newPass123");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawOldPass", "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");

        userService.updatePassword(2L, passwordDTO);

        assertEquals("encodedNewPass", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("12. Đổi mật khẩu thất bại khi nhập sai mật khẩu cũ -> Ném MyException")
    void updatePassword_WrongOldPassword_ThrowsMyException() {
        UserEntity user = createStaff(2L, "Sale A");
        user.setPassword("encodedOldPass");

        PasswordDTO passwordDTO = new PasswordDTO();
        passwordDTO.setOldPassword("wrongPass");
        passwordDTO.setNewPassword("newPass123");
        passwordDTO.setConfirmPassword("newPass123");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encodedOldPass")).thenReturn(false);

        assertThrows(MyException.class, () -> userService.updatePassword(2L, passwordDTO));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("13. Xóa người dùng (delete) -> Chuyển status về 0 (xóa mềm)")
    void deleteUsers_SoftDeleteSetsStatusZero() {
        UserEntity u1 = createStaff(2L, "Sale A");
        when(userRepository.findById(2L)).thenReturn(Optional.of(u1));

        userService.delete(new long[]{2L});

        assertEquals(0, u1.getStatus());
        verify(userRepository, times(1)).save(u1);
    }

    @Test
    @DisplayName("14. Thêm mới và cập nhật người dùng (insert & update) -> Gán đúng role và mật khẩu mặc định")
    void insertAndUpdateUser_Success() {
        RoleEntity role = new RoleEntity();
        role.setCode("STAFF");

        UserDTO inputDto = new UserDTO();
        inputDto.setUserName("sale_new");
        inputDto.setFullName("New Sale");
        inputDto.setRoleCode("STAFF");

        UserEntity convertedEntity = new UserEntity();
        convertedEntity.setUserName("sale_new");
        convertedEntity.setFullName("New Sale");

        when(roleRepository.findOneByCode("STAFF")).thenReturn(role);
        when(userConverter.convertToEntity(inputDto)).thenReturn(convertedEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedDefault");
        when(userRepository.save(convertedEntity)).thenReturn(convertedEntity);
        when(userConverter.convertToDto(convertedEntity)).thenReturn(inputDto);

        UserDTO inserted = userService.insert(inputDto);
        assertNotNull(inserted);
        assertEquals(1, convertedEntity.getStatus());
        assertEquals("encodedDefault", convertedEntity.getPassword());

        // Test update existing user
        UserEntity existingUser = createStaff(5L, "Old Name");
        existingUser.setUserName("sale_existing");
        existingUser.setPassword("oldSecret");
        when(userRepository.findById(5L)).thenReturn(Optional.of(existingUser));

        UserDTO updated = userService.update(5L, inputDto);
        assertNotNull(updated);
        assertEquals("sale_existing", convertedEntity.getUserName());
        assertEquals("oldSecret", convertedEntity.getPassword());
    }
}
