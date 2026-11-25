package com.javaweb.service.impl;

import com.javaweb.converter.CustomerConverter;
import com.javaweb.entity.CustomerEntity;
import com.javaweb.entity.UserEntity;
import com.javaweb.exception.ServiceException;
import com.javaweb.model.event.ActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.javaweb.model.dto.CustomerDTO;
import com.javaweb.model.request.CustomerSearchRequest;
import com.javaweb.model.response.CustomerSearchResponse;
import com.javaweb.model.response.ResponseDTO;
import com.javaweb.model.response.StaffResponseDTO;
import com.javaweb.repository.CustomerRepository;
import com.javaweb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerConverter customerConverter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Tìm kiếm danh sách khách hàng (findAll) -> Trả về danh sách CustomerSearchResponse")
    void findAll_ReturnsCustomerSearchResponses() {
        CustomerSearchRequest request = new CustomerSearchRequest();
        Pageable pageable = PageRequest.of(0, 5);

        CustomerEntity entity = new CustomerEntity();
        entity.setId(1L);
        entity.setFullName("Le Van C");

        CustomerSearchResponse response = new CustomerSearchResponse();
        response.setFullName("Le Van C");

        when(customerRepository.findAll(request, pageable)).thenReturn(Collections.singletonList(entity));
        when(customerConverter.toCustomerSearchResponse(entity)).thenReturn(response);

        List<CustomerSearchResponse> result = customerService.findAll(request, pageable);

        assertEquals(1, result.size());
        assertEquals("Le Van C", result.get(0).getFullName());
    }

    @Test
    @DisplayName("2. Tìm khách hàng theo ID hợp lệ -> Trả về CustomerDTO")
    void findCustomerById_Success() {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(10L);
        entity.setFullName("Pham Thi D");

        CustomerDTO dto = new CustomerDTO();
        dto.setId(10L);
        dto.setFullname("Pham Thi D");

        when(customerRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(customerConverter.toCustomerDTO(entity)).thenReturn(dto);

        CustomerDTO result = customerService.findCustomerById(10L);

        assertNotNull(result);
        assertEquals("Pham Thi D", result.getFullname());
    }

    @Test
    @DisplayName("3. Tìm khách hàng theo ID không tồn tại -> Ném ServiceException")
    void findCustomerById_NotFound_ThrowsException() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> customerService.findCustomerById(404L));
    }

    @Test
    @DisplayName("4. Thêm mới khách hàng hợp lệ -> Tự động bật trạng thái hoạt động (isActive = 1)")
    void addOrUpdateCustomers_CreateNew_SetsIsActiveOne() {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullname("Khach Moi");
        dto.setCustomerPhone("0909123456");

        CustomerEntity converted = new CustomerEntity();
        converted.setFullName("Khach Moi");
        converted.setPhone("0909123456");

        when(customerConverter.toCustomerEntity(dto)).thenReturn(converted);

        customerService.addOrUpdateCustomers(dto);

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(1)).save(captor.capture());
        assertEquals(1, captor.getValue().getIsActive());
    }

    @Test
    @DisplayName("5. Cập nhật thông tin khách hàng -> Giữ nguyên danh sách nhân viên sale đang chăm sóc")
    void addOrUpdateCustomers_UpdateExisting_PreservesAssignedStaffList() {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(20L);
        dto.setFullname("Khach Cu Doi SDT");
        dto.setCustomerPhone("0988777666");

        UserEntity assignedStaff = new UserEntity();
        assignedStaff.setId(2L);
        assignedStaff.setFullName("Sale Ruot");

        CustomerEntity existingInDb = new CustomerEntity();
        existingInDb.setId(20L);
        existingInDb.setUserEntities(Collections.singletonList(assignedStaff));

        CustomerEntity converted = new CustomerEntity();
        converted.setId(20L);
        converted.setFullName("Khach Cu Doi SDT");
        converted.setPhone("0988777666");

        when(customerConverter.toCustomerEntity(dto)).thenReturn(converted);
        when(customerRepository.findById(20L)).thenReturn(Optional.of(existingInDb));

        customerService.addOrUpdateCustomers(dto);

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(1)).save(captor.capture());

        CustomerEntity saved = captor.getValue();
        assertEquals(1, saved.getIsActive());
        assertNotNull(saved.getUserEntities());
        assertEquals(1, saved.getUserEntities().size());
        assertEquals(assignedStaff, saved.getUserEntities().get(0));
    }

    @Test
    @DisplayName("6. Cập nhật khách hàng nhưng ID không tồn tại -> Báo lỗi và không lưu")
    void addOrUpdateCustomers_UpdateNotFound_ThrowsException() {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(999L);
        dto.setFullname("Khach Ao");
        dto.setCustomerPhone("0909000111");

        CustomerEntity converted = new CustomerEntity();
        converted.setId(999L);

        when(customerConverter.toCustomerEntity(dto)).thenReturn(converted);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> customerService.addOrUpdateCustomers(dto));
        verify(customerRepository, never()).save(any());
    }

    @ParameterizedTest(name = "Tên=''{0}'', SĐT=''{1}''")
    @CsvSource(value = {
            "NULL, 0909123456",
            "'   ', 0909123456",
            "Nguyen Van A, NULL",
            "Nguyen Van A, '   '"
    }, nullValues = "NULL")
    @DisplayName("7-10. Thêm/sửa khách hàng nhưng bỏ trống Tên hoặc Số điện thoại -> Báo lỗi")
    void addOrUpdateCustomers_MissingRequiredFields_ThrowsException(String fullName, String phone) {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullname(fullName);
        dto.setCustomerPhone(phone);

        assertThrows(ServiceException.class, () -> customerService.addOrUpdateCustomers(dto));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("11. Xóa khách hàng (deleteCustomers) -> Chỉ ẩn đi (isActive = 0) chứ không xóa vĩnh viễn")
    void deleteCustomers_SoftDeleteSetsIsActiveZero() {
        CustomerEntity c1 = new CustomerEntity();
        c1.setId(10L);
        c1.setIsActive(1);

        when(customerRepository.findById(10L)).thenReturn(Optional.of(c1));

        customerService.deleteCustomers(Collections.singletonList(10L));

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(1)).save(captor.capture());
        assertEquals(0, captor.getValue().getIsActive());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("12-13. Xóa khách hàng với danh sách ID rỗng hoặc null -> Báo lỗi")
    void deleteCustomers_NullOrEmptyIds_ThrowsException(List<Long> emptyIds) {
        assertThrows(ServiceException.class, () -> customerService.deleteCustomers(emptyIds));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("14. Hiển thị danh sách nhân viên phụ trách khách hàng (listStaffs) -> Đánh dấu checked chuẩn xác")
    void listStaffs_MarksAssignedStaffChecked() {
        UserEntity staff1 = new UserEntity();
        staff1.setId(2L);
        staff1.setFullName("Sale A");

        UserEntity staff2 = new UserEntity();
        staff2.setId(3L);
        staff2.setFullName("Sale B");

        CustomerEntity customer = new CustomerEntity();
        customer.setId(10L);
        customer.setUserEntities(Collections.singletonList(staff2));

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(userRepository.findByStatusAndRoles_Code(1, "STAFF")).thenReturn(Arrays.asList(staff1, staff2));

        ResponseDTO response = customerService.listStaffs(10L);

        assertEquals("success", response.getMessage());
        @SuppressWarnings("unchecked")
        List<StaffResponseDTO> data = (List<StaffResponseDTO>) response.getData();
        assertEquals(2, data.size());
        assertEquals("", data.get(0).getChecked());
        assertEquals("checked", data.get(1).getChecked());
    }

    @Test
    @DisplayName("15. Đếm tổng số khách hàng đang hoạt động -> Trả về đúng số lượng")
    void countTotalItem_ReturnsCount() {
        when(customerRepository.countTotalItem()).thenReturn(42);
        assertEquals(42, customerService.countTotalItem());
    }

    @ParameterizedTest(name = "fullname = [{0}] -> bị chặn")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Tên khách hàng null / rỗng / toàn khoảng trắng -> ném lỗi, không ghi DB")
    void addOrUpdateCustomers_blankFullname_throwsAndNeverSaves(String fullname) {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullname(fullname);
        dto.setCustomerPhone("0901234567");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.addOrUpdateCustomers(dto));

        assertEquals("Tên khách hàng không được để trống", ex.getMessage());
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(customerConverter);
    }

    @ParameterizedTest(name = "phone = [{0}] -> bị chặn")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("SĐT khách hàng null / rỗng / khoảng trắng -> ném lỗi đúng message, không ghi DB")
    void addOrUpdateCustomers_blankPhone_throwsAndNeverSaves(String phone) {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullname("Trần Thị B");
        dto.setCustomerPhone(phone);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.addOrUpdateCustomers(dto));

        assertEquals("Số điện thoại khách hàng không được để trống", ex.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thêm khách mới -> object được save có isActive = 1, không tra findById")
    void addOrUpdateCustomers_newCustomer_savedWithIsActive1() {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullname("Trần Thị B");
        dto.setCustomerPhone("0901234567");
        CustomerEntity mapped = new CustomerEntity();   // id = null, isActive mặc định = 0
        when(customerConverter.toCustomerEntity(dto)).thenReturn(mapped);

        customerService.addOrUpdateCustomers(dto);

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getIsActive());
        verify(customerRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Manager xóa 2 khách -> cả 2 bị xóa MỀM (isActive = 0), không xóa cứng")
    void deleteCustomers_twoIds_softDeletesBoth() {
        CustomerEntity c1 = new CustomerEntity();
        c1.setId(1L);
        c1.setIsActive(1);
        CustomerEntity c2 = new CustomerEntity();
        c2.setId(2L);
        c2.setIsActive(1);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(c2));

        customerService.deleteCustomers(Arrays.asList(1L, 2L));

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(c -> c.getIsActive() == 0));
        verify(customerRepository, never()).deleteById(any());
        verify(customerRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa danh sách có id không tồn tại -> ném lỗi kèm đúng id")
    void deleteCustomers_idNotFound_throwsWithId() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.deleteCustomers(Collections.singletonList(99L)));

        assertEquals("Không tìm thấy khách hàng với id = 99", ex.getMessage());
        verify(customerRepository, never()).save(any());
    }
}
