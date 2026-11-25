package com.javaweb.service.impl;

import com.javaweb.converter.TransactionConverter;
import com.javaweb.entity.CustomerEntity;
import com.javaweb.entity.TransactionEntity;
import com.javaweb.exception.ServiceException;
import com.javaweb.model.dto.TransactionTypeDTO;
import com.javaweb.model.response.TransactionResponseDTO;
import com.javaweb.repository.CustomerRepository;
import com.javaweb.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionConverter transactionConverter;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    // ---------- Helper tạo dữ liệu mẫu ----------
    private TransactionTypeDTO dto(Long id, Long customerId, String code, String note) {
        TransactionTypeDTO dto = new TransactionTypeDTO();
        dto.setId(id);
        dto.setCustomerId(customerId);
        dto.setCode(code);
        dto.setNote(note);
        return dto;
    }

    private TransactionEntity entity(Long id, String code, String note) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setNote(note);
        return entity;
    }

    @Test
    @DisplayName("Thêm giao dịch cho khách hàng tồn tại -> save đúng 1 lần và gắn đúng khách hàng")
    void addOrUpdateTransactions_newTransactionCustomerExists_savesOnceWithCustomer() {
        // Arrange
        TransactionTypeDTO input = dto(null, 10L, "CSKH", "Gọi điện tư vấn");
        TransactionEntity mapped = entity(null, "CSKH", "Gọi điện tư vấn");
        CustomerEntity customer = new CustomerEntity();
        customer.setId(10L);
        when(transactionConverter.transactionEntity(input)).thenReturn(mapped);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));

        // Act
        transactionService.addOrUpdateTransactions(input);

        // Assert: bắt đúng object được save để kiểm tra nội dung
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(captor.capture());
        TransactionEntity saved = captor.getValue();
        assertSame(customer, saved.getCustomer());
        assertEquals("CSKH", saved.getCode());
        assertEquals("Gọi điện tư vấn", saved.getNote());
    }

    @Test
    @DisplayName("Thêm giao dịch cho khách hàng KHÔNG tồn tại -> ném ServiceException, không gọi save")
    void addOrUpdateTransactions_customerNotFound_throwsAndNeverSaves() {
        TransactionTypeDTO input = dto(null, 99L, "CSKH", "abc");
        when(transactionConverter.transactionEntity(input)).thenReturn(entity(null, "CSKH", "abc"));
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> transactionService.addOrUpdateTransactions(input));

        assertEquals("Không tìm thấy khách hàng với id = 99", ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Truyền DTO null -> ném ServiceException, không đụng tới converter và repository")
    void addOrUpdateTransactions_nullInput_throwsAndNoInteraction() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> transactionService.addOrUpdateTransactions(null));

        assertEquals("Thông tin giao dịch không được để trống", ex.getMessage());
        verifyNoInteractions(transactionConverter, transactionRepository, customerRepository);
    }

    @Test
    @DisplayName("Sửa giao dịch -> lưu nội dung mới, giữ nguyên khách hàng / người tạo / ngày tạo cũ")
    void addOrUpdateTransactions_updateExisting_keepsCustomerAndCreatedInfo() {
        // Arrange
        TransactionTypeDTO input = dto(5L, 10L, "DDX", "Ghi chú mới");
        TransactionEntity mapped = entity(5L, "DDX", "Ghi chú mới");

        CustomerEntity customer = new CustomerEntity();
        customer.setId(10L);
        Date createdDate = new Date();
        TransactionEntity old = entity(5L, "DDX", "Ghi chú cũ");
        old.setCustomer(customer);
        old.setCreatedBy("manager1");
        old.setCreatedDate(createdDate);

        when(transactionConverter.transactionEntity(input)).thenReturn(mapped);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(old));

        // Act
        transactionService.addOrUpdateTransactions(input);

        // Assert
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());
        TransactionEntity saved = captor.getValue();
        assertEquals("Ghi chú mới", saved.getNote());
        assertSame(customer, saved.getCustomer());
        assertEquals("manager1", saved.getCreatedBy());
        assertEquals(createdDate, saved.getCreatedDate());
        verify(customerRepository, never()).findById(any()); // sửa thì không cần tra lại khách hàng
    }

    @Test
    @DisplayName("Sửa giao dịch KHÔNG tồn tại -> ném ServiceException, không gọi save")
    void addOrUpdateTransactions_updateIdNotFound_throwsAndNeverSaves() {
        TransactionTypeDTO input = dto(77L, 10L, "CSKH", "x");
        when(transactionConverter.transactionEntity(input)).thenReturn(entity(77L, "CSKH", "x"));
        when(transactionRepository.findById(77L)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> transactionService.addOrUpdateTransactions(input));

        assertEquals("Không tìm thấy giao dịch với id = 77", ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy giao dịch CSKH của khách hàng -> trả đúng số phần tử, đúng thứ tự")
    void findByCodeAndCustomerId_hasData_returnsConvertedList() {
        TransactionEntity t1 = entity(1L, "CSKH", "Lần 1");
        TransactionEntity t2 = entity(2L, "CSKH", "Lần 2");
        when(transactionRepository.findAllByCustomerIdAndCode(10L, "CSKH")).thenReturn(Arrays.asList(t1, t2));
        when(transactionConverter.transactionTypeDTO(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity e = invocation.getArgument(0);
                    return dto(e.getId(), 10L, e.getCode(), e.getNote());
                });

        List<TransactionTypeDTO> result = transactionService.findByCodeAndCustomerId("CSKH", 10L);

        assertEquals(2, result.size());
        assertEquals("Lần 1", result.get(0).getNote());
        assertEquals("Lần 2", result.get(1).getNote());
    }

    @Test
    @DisplayName("Khách hàng chưa có giao dịch DDX -> trả list rỗng (không null), không gọi converter")
    void findByCodeAndCustomerId_noData_returnsEmptyListNotNull() {
        when(transactionRepository.findAllByCustomerIdAndCode(10L, "DDX")).thenReturn(Collections.emptyList());

        List<TransactionTypeDTO> result = transactionService.findByCodeAndCustomerId("DDX", 10L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(transactionConverter);
    }

    @Test
    @DisplayName("Tìm giao dịch theo id không tồn tại -> ném ServiceException kèm id")
    void findTransactionById_notFound_throwsWithMessage() {
        when(transactionRepository.findById(123L)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> transactionService.findTransactionById(123L));

        assertEquals("Không tìm thấy giao dịch với id = 123", ex.getMessage());
    }

    @Test
    @DisplayName("loadTransaction đúng id + khách hàng -> trả về đúng ghi chú")
    void loadTransaction_found_returnsNote() {
        when(transactionRepository.findByIdAndCustomerId(1L, 10L))
                .thenReturn(entity(1L, "CSKH", "Khách hẹn xem nhà thứ 7"));

        TransactionResponseDTO result = transactionService.loadTransaction(1L, 10L);

        assertEquals("Khách hẹn xem nhà thứ 7", result.getNote());
    }

    @Test
    @DisplayName("loadTransaction với khách hàng không sở hữu giao dịch -> ném ServiceException")
    void loadTransaction_wrongCustomer_throws() {
        when(transactionRepository.findByIdAndCustomerId(1L, 999L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> transactionService.loadTransaction(1L, 999L));
    }
}