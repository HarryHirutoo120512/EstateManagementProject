package com.javaweb.converter;

import com.javaweb.entity.CustomerEntity;
import com.javaweb.model.dto.CustomerDTO;
import com.javaweb.model.response.CustomerSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CustomerConverterTest {

    private CustomerConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CustomerConverter();
        ReflectionTestUtils.setField(converter, "modelMapper", new ModelMapper());
    }

    private CustomerDTO dto(String fullname, String phone, String status) {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullname(fullname);
        dto.setCustomerPhone(phone);
        dto.setCompanyName("Công ty ABC");
        dto.setStatus(status);
        return dto;
    }

    @Test
    @DisplayName("DTO -> Entity: fullname/customerPhone map sang fullName/phone; status null -> CHUA_XL")
    void toCustomerEntity_nullStatus_mapsFieldsAndDefaultsStatus() {
        CustomerEntity entity = converter.toCustomerEntity(dto("Trần Thị B", "0901234567", null));

        assertEquals("Trần Thị B", entity.getFullName());
        assertEquals("0901234567", entity.getPhone());
        assertEquals("Công ty ABC", entity.getCompanyName());
        assertEquals("CHUA_XL", entity.getStatus());
    }

    @Test
    @DisplayName("DTO có status DANG_XL -> giữ nguyên, không bị ghi đè thành CHUA_XL")
    void toCustomerEntity_statusProvided_keepsStatus() {
        CustomerEntity entity = converter.toCustomerEntity(dto("Trần Thị B", "0901234567", "DANG_XL"));

        assertEquals("DANG_XL", entity.getStatus());
    }

    @Test
    @DisplayName("Entity -> SearchResponse: mã trạng thái DA_XL hiển thị thành 'Đã xử lý'")
    void toCustomerSearchResponse_statusCode_convertsToDisplayName() {
        CustomerEntity entity = new CustomerEntity();
        entity.setFullName("Lê Văn C");
        entity.setPhone("0911111111");
        entity.setStatus("DA_XL");

        CustomerSearchResponse res = converter.toCustomerSearchResponse(entity);

        assertEquals("Lê Văn C", res.getFullName());
        assertEquals("0911111111", res.getPhone());
        assertEquals("Đã xử lý", res.getStatus());
    }

    @Test
    @DisplayName("Entity -> DTO (màn sửa): fullName/phone map ngược lại sang fullname/customerPhone")
    void toCustomerDTO_mapsNameAndPhoneBack() {
        CustomerEntity entity = new CustomerEntity();
        entity.setFullName("Lê Văn C");
        entity.setPhone("0911111111");
        entity.setCompanyName("XYZ");

        CustomerDTO result = converter.toCustomerDTO(entity);

        assertEquals("Lê Văn C", result.getFullname());
        assertEquals("0911111111", result.getCustomerPhone());
        assertEquals("XYZ", result.getCompanyName());
    }

    @Test
    @DisplayName("[BẮT BUG] status là chuỗi rỗng tạo lúc runtime -> vẫn phải về CHUA_XL")
    void toCustomerEntity_emptyStatusCreatedAtRuntime_defaultsToChuaXuLy() {
        // new String("") mô phỏng chuỗi rỗng đến từ request. Code hiện tại so sánh bằng `==`
        // (so sánh địa chỉ, không so nội dung) nên test này sẽ ĐỎ.
        // Sửa trong CustomerConverter: `customerDTO.getStatus() == ""` -> `StringUtils.isBlank(customerDTO.getStatus())`
        CustomerEntity entity = converter.toCustomerEntity(dto("Trần Thị B", "0901234567", new String("")));

        assertEquals("CHUA_XL", entity.getStatus());
    }
}