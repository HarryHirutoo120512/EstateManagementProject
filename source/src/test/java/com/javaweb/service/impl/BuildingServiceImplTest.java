package com.javaweb.service.impl;

import com.javaweb.converter.BuildingDTOConverter;
import com.javaweb.entity.BuildingEntity;
import com.javaweb.entity.RentAreaEntity;
import com.javaweb.entity.UserEntity;
import com.javaweb.exception.ServiceException;
import com.javaweb.model.event.ActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.javaweb.model.dto.BuildingDTO;
import com.javaweb.model.request.BuildingSearchRequest;
import com.javaweb.model.response.BuildingSearchResponse;
import com.javaweb.model.response.ResponseDTO;
import com.javaweb.model.response.StaffResponseDTO;
import com.javaweb.repository.AssignmentBuildingRepository;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.RentAreaRepository;
import com.javaweb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingServiceImplTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RentAreaRepository rentAreaRepository;

    @Mock
    private AssignmentBuildingRepository assignmentBuildingRepository;

    @Mock
    private BuildingDTOConverter buildingDTOConverter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BuildingServiceImpl buildingService;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Tìm kiếm danh sách tòa nhà (findAll) -> Trả về danh sách BuildingSearchResponse đúng")
    void findAll_ReturnsBuildingSearchResponses() {
        BuildingSearchRequest request = new BuildingSearchRequest();
        request.setName("Tower");
        Pageable pageable = PageRequest.of(0, 5);

        BuildingEntity entity = new BuildingEntity();
        entity.setId(1L);
        entity.setName("Bitexco Tower");

        BuildingSearchResponse response = new BuildingSearchResponse();
        response.setId(1L);
        response.setName("Bitexco Tower");

        when(buildingRepository.findAll(request, pageable)).thenReturn(Collections.singletonList(entity));
        when(buildingDTOConverter.toBuildingSearchResponse(entity)).thenReturn(response);

        List<BuildingSearchResponse> result = buildingService.findAll(request, pageable);

        assertEquals(1, result.size());
        assertEquals("Bitexco Tower", result.get(0).getName());
        verify(buildingRepository, times(1)).findAll(request, pageable);
    }

    @Test
    @DisplayName("2. Tìm tòa nhà theo ID hợp lệ -> Trả về BuildingDTO")
    void findBuildingById_Success() {
        BuildingEntity entity = new BuildingEntity();
        entity.setId(1L);
        entity.setName("Landmark 81");

        BuildingDTO dto = new BuildingDTO();
        dto.setId(1L);
        dto.setName("Landmark 81");

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(buildingDTOConverter.toBuildingDTO_forUpdateBuilding(entity)).thenReturn(dto);

        BuildingDTO result = buildingService.findBuildingById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Landmark 81", result.getName());
    }

    @Test
    @DisplayName("3. Tìm tòa nhà theo ID không tồn tại -> Ném ServiceException")
    void findBuildingById_NotFound_ThrowsException() {
        when(buildingRepository.findById(999L)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> buildingService.findBuildingById(999L));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    @DisplayName("4. Thêm mới tòa nhà hợp lệ -> Lưu tòa nhà và tách lưu từng diện tích thuê")
    void addOrUpdateBuildings_CreateNew_Success() {
        BuildingDTO dto = new BuildingDTO();
        dto.setName("Sunwah Tower");
        dto.setTypeCode(Arrays.asList("TANG_TRET", "NGUYEN_CAN"));
        dto.setRentArea("100,200");

        BuildingEntity converted = new BuildingEntity();
        converted.setName("Sunwah Tower");
        RentAreaEntity r1 = new RentAreaEntity();
        r1.setValue("100");
        RentAreaEntity r2 = new RentAreaEntity();
        r2.setValue("200");
        converted.setItems(Arrays.asList(r1, r2));

        BuildingEntity savedEntity = new BuildingEntity();
        savedEntity.setId(10L);
        savedEntity.setName("Sunwah Tower");

        when(buildingDTOConverter.toBuildingEntity(dto)).thenReturn(converted);
        when(buildingDTOConverter.entityToEntity(any(BuildingEntity.class), eq(converted))).thenReturn(converted);
        when(buildingRepository.save(converted)).thenReturn(savedEntity);

        buildingService.addOrUpdateBuildings(dto);

        verify(buildingRepository, times(1)).save(converted);
        verify(rentAreaRepository, times(1)).deleteBuildingById(10L);
        ArgumentCaptor<RentAreaEntity> rentAreaCaptor = ArgumentCaptor.forClass(RentAreaEntity.class);
        verify(rentAreaRepository, times(2)).save(rentAreaCaptor.capture());

        List<RentAreaEntity> savedRentAreas = rentAreaCaptor.getAllValues();
        assertEquals("100", savedRentAreas.get(0).getValue());
        assertEquals(savedEntity, savedRentAreas.get(0).getBuilding());
        assertEquals("200", savedRentAreas.get(1).getValue());
        assertEquals(savedEntity, savedRentAreas.get(1).getBuilding());
        verify(eventPublisher).publishEvent(argThat((ActivityEvent e) -> ActivityEvent.BUILDING_SAVED.equals(e.getType()) && e.getMessage().contains("Sunwah Tower")));
    }

    @Test
    @DisplayName("5. Cập nhật tòa nhà khi ID tồn tại -> Dọn diện tích thuê cũ và lưu diện tích mới")
    void addOrUpdateBuildings_UpdateExisting_Success() {
        BuildingDTO dto = new BuildingDTO();
        dto.setId(5L);
        dto.setName("Sunwah Updated");
        dto.setTypeCode(Collections.singletonList("NOI_THAT"));
        dto.setRentArea("300");

        BuildingEntity existingInDb = new BuildingEntity();
        existingInDb.setId(5L);
        existingInDb.setName("Sunwah Old");

        BuildingEntity converted = new BuildingEntity();
        converted.setId(5L);
        converted.setName("Sunwah Updated");
        RentAreaEntity r1 = new RentAreaEntity();
        r1.setValue("300");
        converted.setItems(Collections.singletonList(r1));

        when(buildingRepository.findById(5L)).thenReturn(Optional.of(existingInDb));
        when(buildingDTOConverter.toBuildingEntity(dto)).thenReturn(converted);
        when(buildingDTOConverter.entityToEntity(existingInDb, converted)).thenReturn(converted);
        when(buildingRepository.save(converted)).thenReturn(converted);

        buildingService.addOrUpdateBuildings(dto);

        verify(buildingRepository, times(1)).findById(5L);
        verify(buildingRepository, times(1)).save(converted);
        verify(rentAreaRepository, times(1)).deleteBuildingById(5L);
        verify(rentAreaRepository, times(1)).save(r1);
    }

    @Test
    @DisplayName("6. Cập nhật tòa nhà nhưng ID không tồn tại -> Báo lỗi và không lưu DB")
    void addOrUpdateBuildings_UpdateIdNotFound_ThrowsException() {
        BuildingDTO dto = new BuildingDTO();
        dto.setId(404L);
        dto.setName("Ghost Building");
        dto.setTypeCode(Collections.singletonList("TANG_TRET"));

        when(buildingDTOConverter.toBuildingEntity(dto)).thenReturn(new BuildingEntity());
        when(buildingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> buildingService.addOrUpdateBuildings(dto));
        verify(buildingRepository, never()).save(any());
        verify(rentAreaRepository, never()).save(any());
    }

    static Stream<Arguments> invalidBuildingDTOs() {
        BuildingDTO nullName = new BuildingDTO();
        nullName.setName(null);
        nullName.setTypeCode(Collections.singletonList("TANG_TRET"));

        BuildingDTO blankName = new BuildingDTO();
        blankName.setName("   ");
        blankName.setTypeCode(Collections.singletonList("TANG_TRET"));

        BuildingDTO nullTypeCode = new BuildingDTO();
        nullTypeCode.setName("Valid Name");
        nullTypeCode.setTypeCode(null);

        BuildingDTO emptyTypeCode = new BuildingDTO();
        emptyTypeCode.setName("Valid Name");
        emptyTypeCode.setTypeCode(Collections.emptyList());

        return Stream.of(
                Arguments.of("Thiếu tên tòa nhà (null)", nullName),
                Arguments.of("Tên tòa nhà chỉ có khoảng trắng", blankName),
                Arguments.of("Thiếu loại tòa nhà (null)", nullTypeCode),
                Arguments.of("Danh sách loại tòa nhà rỗng", emptyTypeCode)
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidBuildingDTOs")
    @DisplayName("7-10. Thêm/sửa tòa nhà thiếu thông tin bắt buộc -> Báo lỗi và không lưu")
    void addOrUpdateBuildings_InvalidInput_ThrowsException(String scenario, BuildingDTO invalidDto) {
        assertThrows(ServiceException.class, () -> buildingService.addOrUpdateBuildings(invalidDto));
        verify(buildingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ActivityEvent.class));
    }

    @Test
    @DisplayName("11. Xóa danh sách tòa nhà hợp lệ -> Xóa phân công, xóa diện tích thuê và xóa tòa nhà")
    void deleteBuildings_Success() {
        BuildingEntity b1 = new BuildingEntity();
        b1.setId(1L);
        BuildingEntity b2 = new BuildingEntity();
        b2.setId(2L);

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b1));
        when(buildingRepository.findById(2L)).thenReturn(Optional.of(b2));

        buildingService.deleteBuildings(Arrays.asList(1L, 2L));

        verify(buildingRepository, times(1)).deleteAssignmentByBuildingId(1L);
        verify(buildingRepository, times(1)).deleteRentAreaByBuildingId(1L);
        verify(buildingRepository, times(1)).deleteById(1L);

        verify(buildingRepository, times(1)).deleteAssignmentByBuildingId(2L);
        verify(buildingRepository, times(1)).deleteRentAreaByBuildingId(2L);
        verify(buildingRepository, times(1)).deleteById(2L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("12-13. Xóa tòa nhà với danh sách ID rỗng hoặc null -> Báo lỗi")
    void deleteBuildings_NullOrEmptyIds_ThrowsException(List<Long> emptyIds) {
        assertThrows(ServiceException.class, () -> buildingService.deleteBuildings(emptyIds));
        verify(buildingRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("14. Hiển thị danh sách nhân viên để giao tòa nhà (listStaffs) -> Đánh dấu checked cho nhân viên đang phụ trách")
    void listStaffs_MarksAssignedStaffChecked() {
        BuildingEntity building = new BuildingEntity();
        building.setId(1L);

        UserEntity staff1 = new UserEntity();
        staff1.setId(2L);
        staff1.setFullName("Sale A");

        UserEntity staff2 = new UserEntity();
        staff2.setId(3L);
        staff2.setFullName("Sale B");

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(userRepository.findByStatusAndRoles_Code(1, "STAFF")).thenReturn(Arrays.asList(staff1, staff2));
        when(userRepository.findUsersByBuilding(building)).thenReturn(new ArrayList<>(Collections.singletonList(staff1)));

        ResponseDTO response = buildingService.listStaffs(1L);

        assertEquals("success", response.getMessage());
        @SuppressWarnings("unchecked")
        List<StaffResponseDTO> data = (List<StaffResponseDTO>) response.getData();
        assertEquals(2, data.size());
        assertEquals("checked", data.get(0).getChecked());
        assertEquals("", data.get(1).getChecked());
    }

    @Test
    @DisplayName("15. Đếm tổng số tòa nhà (countTotalItem) -> Trả về đúng số lượng từ Repository")
    void countTotalItem_ReturnsCount() {
        when(buildingRepository.countTotalItem()).thenReturn(15);
        assertEquals(15, buildingService.countTotalItem());
    }

    @Test
    @DisplayName("Trang 0 (trang đầu) -> truyền đúng Pageable xuống repository")
    void findAll_firstPage_passesPageableToRepository() {
        BuildingSearchRequest request = new BuildingSearchRequest();
        Pageable firstPage = PageRequest.of(0, 5);
        when(buildingRepository.findAll(request, firstPage)).thenReturn(Collections.emptyList());

        buildingService.findAll(request, firstPage);

        verify(buildingRepository).findAll(request, firstPage);
    }

    @Test
    @DisplayName("Trang vượt quá tổng số trang -> trả list rỗng (không null), không gọi converter")
    void findAll_pageOutOfRange_returnsEmptyList() {
        BuildingSearchRequest request = new BuildingSearchRequest();
        Pageable page100 = PageRequest.of(100, 5);
        when(buildingRepository.findAll(request, page100)).thenReturn(Collections.emptyList());

        List<BuildingSearchResponse> result = buildingService.findAll(request, page100);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(buildingDTOConverter);
    }

    @Test
    @DisplayName("Request null -> service tự tạo request rỗng, không NPE, không gắn staffId")
    void findAll_nullRequest_usesEmptyRequest() {
        Pageable firstPage = PageRequest.of(0, 5);
        when(buildingRepository.findAll(any(BuildingSearchRequest.class), eq(firstPage)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> buildingService.findAll(null, firstPage));

        ArgumentCaptor<BuildingSearchRequest> captor = ArgumentCaptor.forClass(BuildingSearchRequest.class);
        verify(buildingRepository).findAll(captor.capture(), eq(firstPage));
        assertNotNull(captor.getValue());
        assertNull(captor.getValue().getStaffId());
    }

    @Test
    @DisplayName("Xóa tòa nhà không tồn tại -> ném lỗi kèm id, không xóa gì")
    void deleteBuildings_idNotFound_throwsAndDeletesNothing() {
        when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> buildingService.deleteBuildings(Collections.singletonList(99L)));

        assertEquals("Không tìm thấy tòa nhà với id = 99", ex.getMessage());
        verify(buildingRepository, never()).deleteById(any());
        verify(buildingRepository, never()).deleteAssignmentByBuildingId(any());
    }
}
