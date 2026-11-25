package com.javaweb.converter;

import com.javaweb.entity.BuildingEntity;
import com.javaweb.entity.RentAreaEntity;
import com.javaweb.model.dto.BuildingDTO;
import com.javaweb.model.response.BuildingSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BuildingDTOConverterTest {

    private BuildingDTOConverter converter;

    @BeforeEach
    void setUp() {
        converter = new BuildingDTOConverter();
        // Dùng ModelMapper THẬT, không mock: converter là mapping thuần, mock thì không test được gì
        ReflectionTestUtils.setField(converter, "modelMapper", new ModelMapper());
    }

    private RentAreaEntity rentArea(String value) {
        RentAreaEntity r = new RentAreaEntity();
        r.setValue(value);
        return r;
    }

    private BuildingEntity building(String street, String ward, String district) {
        BuildingEntity entity = new BuildingEntity();
        entity.setName("Nam Giao Building");
        entity.setStreet(street);
        entity.setWard(ward);
        entity.setDistrict(district);
        return entity;
    }

    @Test
    @DisplayName("Entity có mã quận hợp lệ -> địa chỉ = street + ward + TÊN quận")
    void toBuildingSearchResponse_validDistrict_buildsAddressWithDistrictName() {
        BuildingEntity entity = building("59 Phan Xích Long", "Phường 2", "QUAN_1");

        BuildingSearchResponse res = converter.toBuildingSearchResponse(entity);

        assertEquals("Nam Giao Building", res.getName());
        assertEquals("59 Phan Xích Long Phường 2 Quận 1", res.getAddress());
    }

    @Test
    @DisplayName("District rỗng -> địa chỉ chỉ gồm street + ward, không dính chữ 'null'")
    void toBuildingSearchResponse_blankDistrict_addressWithoutDistrict() {
        BuildingSearchResponse res = converter.toBuildingSearchResponse(building("12 Lê Lợi", "Bến Nghé", ""));

        assertEquals("12 Lê Lợi Bến Nghé", res.getAddress());
        assertFalse(res.getAddress().contains("null"));
    }

    @Test
    @DisplayName("Danh sách diện tích thuê có phần tử null -> bỏ qua null, nối bằng dấu phẩy")
    void toBuildingSearchResponse_rentAreasContainNull_joinsOnlyValidValues() {
        BuildingEntity entity = building("A", "B", "QUAN_2");
        entity.setItems(Arrays.asList(rentArea("100"), null, rentArea("200"), rentArea(null)));

        BuildingSearchResponse res = converter.toBuildingSearchResponse(entity);

        assertEquals("100,200", res.getRentArea());
    }

    @Test
    @DisplayName("items = null -> rentArea là chuỗi rỗng, không NPE")
    void toBuildingSearchResponse_nullItems_returnsEmptyRentArea() {
        BuildingEntity entity = building("A", "B", "QUAN_3");
        entity.setItems(null);

        BuildingSearchResponse res = assertDoesNotThrow(() -> converter.toBuildingSearchResponse(entity));

        assertEquals("", res.getRentArea());
    }

    @Test
    @DisplayName("DTO -> Entity: tách rentArea theo dấu phẩy, bỏ phần tử rỗng, trim khoảng trắng; ghép typeCode")
    void toBuildingEntity_rentAreaAndTypeCode_splitAndJoinCorrectly() {
        BuildingDTO dto = new BuildingDTO();
        dto.setName("ACM Tower");
        dto.setRentArea(" 100, ,200 ,300");
        dto.setTypeCode(Arrays.asList("NGUYEN_CAN", "", "NOI_THAT"));

        BuildingEntity entity = converter.toBuildingEntity(dto);

        List<String> values = entity.getItems().stream()
                .map(RentAreaEntity::getValue)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("100", "200", "300"), values);
        assertEquals("NGUYEN_CAN,NOI_THAT", entity.getType());
    }

    @Test
    @DisplayName("DTO thiếu typeCode và rentArea (null) -> type = \"\", items rỗng, không NPE")
    void toBuildingEntity_nullTypeCodeAndRentArea_noNpe() {
        BuildingDTO dto = new BuildingDTO();
        dto.setName("Tòa nhà trống");

        BuildingEntity entity = assertDoesNotThrow(() -> converter.toBuildingEntity(dto));

        assertEquals("", entity.getType());
        assertTrue(entity.getItems().isEmpty());
    }

    @Test
    @DisplayName("Entity -> DTO (màn sửa): type 'A,B' tách thành list, rentArea nối lại bằng dấu phẩy")
    void toBuildingDTOForUpdate_splitsTypeAndJoinsRentArea() {
        BuildingEntity entity = building("A", "B", "QUAN_1");
        entity.setType("NGUYEN_CAN,NOI_THAT");
        entity.setItems(Arrays.asList(rentArea("100"), rentArea("200")));

        BuildingDTO dto = converter.toBuildingDTO_forUpdateBuilding(entity);

        assertEquals(Arrays.asList("NGUYEN_CAN", "NOI_THAT"), dto.getTypeCode());
        assertEquals("100,200", dto.getRentArea());
    }
}