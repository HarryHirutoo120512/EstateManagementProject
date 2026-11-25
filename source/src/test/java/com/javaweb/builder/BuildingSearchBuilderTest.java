package com.javaweb.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildingSearchBuilderTest {

    @Test
    @DisplayName("Set đủ các trường có setter -> build() giữ nguyên giá trị từng trường")
    void build_allFieldsSet_keepsEveryValue() {
        List<String> types = Arrays.asList("NGUYEN_CAN", "NOI_THAT");

        BuildingSearchBuilder b = new BuildingSearchBuilder.Builder()
                .setName("Nam Giao").setFloorArea(500L).setDistrictId(1L)
                .setStreet("Phan Xích Long").setWard("Phường 2").setNumberOfBasement(2L)
                .setRentAreaFrom(100L).setRentAreaTo(300L)
                .setRentPriceFrom(10L).setRentPriceTo(50L)
                .setManagerName("Anh Long").setManagerPhoneNumber("0909123456")
                .setTypeCode(types).setStaffId(3L)
                .build();

        assertAll(
                () -> assertEquals("Nam Giao", b.getName()),
                () -> assertEquals(500L, b.getFloorArea()),
                () -> assertEquals(1L, b.getDistrictId()),
                () -> assertEquals("Phan Xích Long", b.getStreet()),
                () -> assertEquals("Phường 2", b.getWard()),
                () -> assertEquals(2L, b.getNumberOfBasement()),
                () -> assertEquals(100L, b.getAreaFrom()),
                () -> assertEquals(300L, b.getAreaTo()),
                () -> assertEquals(10L, b.getRentPriceFrom()),
                () -> assertEquals(50L, b.getRentPriceTo()),
                () -> assertEquals("Anh Long", b.getManagerName()),
                () -> assertEquals("0909123456", b.getManagerPhoneNumber()),
                () -> assertEquals(types, b.getTypeCode()),
                () -> assertEquals(3L, b.getStaffId())
        );
    }

    @Test
    @DisplayName("Không set gì -> mọi trường đều null (bộ lọc động sẽ bỏ qua toàn bộ)")
    void build_nothingSet_allFieldsNull() {
        BuildingSearchBuilder b = new BuildingSearchBuilder.Builder().build();

        assertAll(
                () -> assertNull(b.getName()),
                () -> assertNull(b.getDistrictId()),
                () -> assertNull(b.getRentPriceFrom()),
                () -> assertNull(b.getTypeCode()),
                () -> assertNull(b.getStaffId())
        );
    }

    @Test
    @DisplayName("direction và level: Builder chưa có setter -> luôn null (thiếu sót cần biết)")
    void build_directionAndLevel_alwaysNullBecauseNoSetter() {
        BuildingSearchBuilder b = new BuildingSearchBuilder.Builder().setName("X").build();

        // Builder chỉ có 14/16 setter. Muốn đủ 16 tiêu chí thì thêm setDirection/setLevel
        // trong Builder và gán trong constructor BuildingSearchBuilder(Builder).
        assertNull(b.getDirection());
        assertNull(b.getLevel());
    }
}