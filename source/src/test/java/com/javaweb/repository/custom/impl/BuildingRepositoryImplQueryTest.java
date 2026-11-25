package com.javaweb.repository.custom.impl;

import com.javaweb.model.request.BuildingSearchRequest;
import com.javaweb.repository.custom.impl.BuildingRepositoryImpl.BuiltQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BuildingRepositoryImplQueryTest {

    private BuiltQuery build(BuildingSearchRequest req) {
        return BuildingRepositoryImpl.buildSearchQuery(req, null);
    }

    @Test
    @DisplayName("Request rỗng -> không có điều kiện, không LIMIT")
    void emptyRequest_addsNoCondition() {
        BuiltQuery q = build(new BuildingSearchRequest());
        assertTrue(q.params.isEmpty());
        assertFalse(q.sql.contains("LIMIT"));
        assertTrue(q.sql.contains("ORDER BY b.id DESC"));
    }

    @Test
    @DisplayName("[SQL Injection] Payload tấn công chỉ nằm trong params, KHÔNG nằm trong câu SQL")
    void injectionPayload_staysInParamsNotInSql() {
        BuildingSearchRequest req = new BuildingSearchRequest();
        req.setName("' OR 1=1 --");

        BuiltQuery q = build(req);

        assertTrue(q.sql.contains("b.name LIKE :name"));
        assertFalse(q.sql.contains("OR 1=1"));
        assertFalse(q.sql.contains("--"));
        assertEquals("%' OR 1=1 --%", q.params.get("name"));
    }

    @Test
    @DisplayName("[SQL Injection] Mọi kiểu dữ liệu đều được tham số hóa (chuỗi, số, mã quận, staffId, typeCode)")
    void allInputsAreParameterized() {
        BuildingSearchRequest req = new BuildingSearchRequest();
        req.setDistrict("QUAN_1'; DROP TABLE building;--");
        req.setStaffId(7L);
        req.setNumberOfBasement(2L);
        req.setTypeCode(Arrays.asList("A'; DROP TABLE x;--"));

        BuiltQuery q = build(req);

        assertFalse(q.sql.contains("DROP"));
        assertTrue(q.sql.contains("b.district = :district"));
        assertTrue(q.sql.contains("a.staffid = :staffId"));
        assertTrue(q.sql.contains("b.numberofbasement = :numberOfBasement"));
        assertEquals(7L, q.params.get("staffId"));
    }

    @Test
    @DisplayName("[Lỗi 4] Có WHERE kể cả khi không phân trang (pageable == null)")
    void whereKeptWhenNotPaged() {
        BuildingSearchRequest req = new BuildingSearchRequest();
        req.setDistrict("QUAN_1");

        BuiltQuery q = build(req);

        assertTrue(q.sql.contains("b.district = :district"));
        assertFalse(q.sql.contains("LIMIT"));
    }

    @Test
    @DisplayName("[Lỗi 1] Lọc diện tích -> dùng EXISTS (không phải EXIST) và tham số hóa")
    void rentArea_usesExists() {
        BuildingSearchRequest req = new BuildingSearchRequest();
        req.setAreaFrom(100L);
        req.setAreaTo(300L);

        BuiltQuery q = build(req);

        assertTrue(q.sql.contains("AND EXISTS (SELECT 1 FROM rentarea"));
        assertTrue(q.sql.contains(">= :areaFrom"));
        assertTrue(q.sql.contains("<= :areaTo"));
        assertEquals(100L, q.params.get("areaFrom"));
    }

    @Test
    @DisplayName("[Lỗi 2] Lọc giá thuê -> mỗi điều kiện có AND phía trước")
    void rentPrice_hasAndBeforeEachCondition() {
        BuildingSearchRequest req = new BuildingSearchRequest();
        req.setRentPriceFrom(10L);
        req.setRentPriceTo(50L);

        BuiltQuery q = build(req);

        assertTrue(q.sql.contains("AND b.rentprice >= :rentPriceFrom"));
        assertTrue(q.sql.contains("AND b.rentprice <= :rentPriceTo"));
    }

    @Test
    @DisplayName("[Lỗi 3] Nhiều loại tòa nhà -> nối bằng OR, mỗi phần tử một tham số")
    void typeCode_joinedByOrWithOwnParams() {
        BuildingSearchRequest req = new BuildingSearchRequest();
        req.setTypeCode(Arrays.asList("NGUYEN_CAN", "NOI_THAT"));

        BuiltQuery q = build(req);

        assertTrue(q.sql.contains("(b.type LIKE :typeCode0 OR b.type LIKE :typeCode1)"));
        assertEquals("%NGUYEN_CAN%", q.params.get("typeCode0"));
        assertEquals("%NOI_THAT%", q.params.get("typeCode1"));
    }

    @Test
    @DisplayName("Lọc theo nhân viên -> EXISTS (không JOIN/DISTINCT/GROUP BY)")
    void staffScope_usesExistsNotJoin() {
        BuildingSearchRequest req = new BuildingSearchRequest();
        req.setStaffId(3L);

        BuiltQuery q = build(req);

        assertTrue(q.sql.contains("EXISTS (SELECT 1 FROM assignmentbuilding a"));
        assertFalse(q.sql.contains("JOIN"));
        assertFalse(q.sql.contains("DISTINCT"));
        assertFalse(q.sql.contains("GROUP BY"));
    }

    @Test
    @DisplayName("Phân trang -> ORDER BY ổn định + LIMIT/OFFSET dạng tham số")
    void paging_addsStableOrderAndParams() {
        BuiltQuery q = BuildingRepositoryImpl.buildSearchQuery(new BuildingSearchRequest(), PageRequest.of(2, 20));

        assertTrue(q.sql.contains("ORDER BY b.id DESC"));
        assertTrue(q.sql.contains("LIMIT :limit OFFSET :offset"));
        assertEquals(20, q.params.get("limit"));
        assertEquals(40L, q.params.get("offset"));
    }

    @Test
    @DisplayName("Request null -> không NullPointerException")
    void nullRequest_isSafe() {
        assertDoesNotThrow(() -> BuildingRepositoryImpl.buildSearchQuery(null, null));
    }
}
