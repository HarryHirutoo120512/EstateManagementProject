package com.javaweb.repository.custom.impl;

import com.javaweb.entity.BuildingEntity;
import com.javaweb.model.request.BuildingSearchRequest;
import com.javaweb.repository.custom.BuildingRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BuildingRepositoryImpl implements BuildingRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /** SQL + tham số, tách ra để test không cần DB. */
    public static class BuiltQuery {
        public final String sql;
        public final Map<String, Object> params;

        BuiltQuery(String sql, Map<String, Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }

    /** Mệnh đề WHERE + params. Tên cột là hằng số trong code; input của người dùng chỉ là GIÁ TRỊ tham số. */
    static BuiltQuery buildWhere(BuildingSearchRequest req) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        Map<String, Object> params = new LinkedHashMap<>();

        // Chuỗi: LIKE %x% có tham số (wildcard đầu => không dùng được B-Tree index)
        like(where, params, "b.name", "name", req.getName());
        like(where, params, "b.ward", "ward", req.getWard());
        like(where, params, "b.street", "street", req.getStreet());
        like(where, params, "b.direction", "direction", req.getDirection());
        like(where, params, "b.managername", "managername", req.getManagername());
        like(where, params, "b.managerphone", "managerphone", req.getManagerphone());

        // Mã quận là mã cố định => so sánh bằng (dùng được index; LIKE '%QUAN_1%' sẽ khớp nhầm QUAN_10...)
        if (StringUtils.hasText(req.getDistrict())) {
            where.append(" AND b.district = :district ");
            params.put("district", req.getDistrict().trim());
        }
        eq(where, params, "b.floorarea", "floorArea", req.getFloorArea());
        eq(where, params, "b.numberofbasement", "numberOfBasement", req.getNumberOfBasement());
        eq(where, params, "b.level", "level", req.getLevel());

        // Giá thuê: mỗi điều kiện có AND riêng
        if (req.getRentPriceFrom() != null) {
            where.append(" AND b.rentprice >= :rentPriceFrom ");
            params.put("rentPriceFrom", req.getRentPriceFrom());
        }
        if (req.getRentPriceTo() != null) {
            where.append(" AND b.rentprice <= :rentPriceTo ");
            params.put("rentPriceTo", req.getRentPriceTo());
        }

        // Diện tích thuê: EXISTS (không nhân dòng như JOIN).
        // rentarea.value là VARCHAR => CAST sang số, nếu không sẽ so sánh theo thứ tự chữ.
        if (req.getAreaFrom() != null || req.getAreaTo() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM rentarea r WHERE r.buildingid = b.id ");
            if (req.getAreaFrom() != null) {
                where.append(" AND CAST(r.value AS UNSIGNED) >= :areaFrom ");
                params.put("areaFrom", req.getAreaFrom());
            }
            if (req.getAreaTo() != null) {
                where.append(" AND CAST(r.value AS UNSIGNED) <= :areaTo ");
                params.put("areaTo", req.getAreaTo());
            }
            where.append(") ");
        }

        // Nhân viên phụ trách: EXISTS thay JOIN => không cần DISTINCT/GROUP BY
        if (req.getStaffId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM assignmentbuilding a "
                    + "WHERE a.buildingid = b.id AND a.staffid = :staffId) ");
            params.put("staffId", req.getStaffId());
        }

        // Nhiều loại tòa nhà: nối bằng OR, mỗi phần tử một tham số riêng
        if (req.getTypeCode() != null) {
            List<String> parts = new ArrayList<>();
            int i = 0;
            for (String code : req.getTypeCode()) {
                if (StringUtils.hasText(code)) {
                    String key = "typeCode" + i++;
                    parts.add("b.type LIKE :" + key);
                    params.put(key, "%" + code.trim() + "%");
                }
            }
            if (!parts.isEmpty()) {
                where.append(" AND (").append(String.join(" OR ", parts)).append(") ");
            }
        }
        return new BuiltQuery(where.toString(), params);
    }

    public static BuiltQuery buildSearchQuery(BuildingSearchRequest req, Pageable pageable) {
        if (req == null) {
            req = new BuildingSearchRequest();
        }
        BuiltQuery w = buildWhere(req);
        StringBuilder sql = new StringBuilder("SELECT b.* FROM building b ")
                .append(w.sql)
                .append(" ORDER BY b.id DESC "); // thứ tự ổn định => phân trang không nhảy dòng
        Map<String, Object> params = new LinkedHashMap<>(w.params);
        if (pageable != null && pageable.isPaged()) {
            sql.append(" LIMIT :limit OFFSET :offset ");
            params.put("limit", pageable.getPageSize());
            params.put("offset", pageable.getOffset());
        }
        return new BuiltQuery(sql.toString(), params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BuildingEntity> findAll(BuildingSearchRequest request, Pageable pageable) {
        BuiltQuery built = buildSearchQuery(request, pageable);
        Query query = entityManager.createNativeQuery(built.sql, BuildingEntity.class);
        built.params.forEach(query::setParameter);
        return query.getResultList();
    }

    /** Trước đây SELECT * rồi .size() => kéo cả bảng lên RAM. */
    @Override
    public int countTotalItem() {
        Number n = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM building").getSingleResult();
        return n.intValue();
    }

    @Transactional
    @Override
    public void deleteAssignmentByBuildingId(Long id) {
        entityManager.createNativeQuery("DELETE FROM assignmentbuilding WHERE buildingid = :id")
                .setParameter("id", id).executeUpdate();
    }

    @Transactional
    @Override
    public void deleteRentAreaByBuildingId(Long id) {
        entityManager.createNativeQuery("DELETE FROM rentarea WHERE buildingid = :id")
                .setParameter("id", id).executeUpdate();
    }

    private static void like(StringBuilder where, Map<String, Object> params,
                             String column, String key, String value) {
        if (StringUtils.hasText(value)) {
            where.append(" AND ").append(column).append(" LIKE :").append(key).append(" ");
            params.put(key, "%" + value.trim() + "%");
        }
    }

    private static void eq(StringBuilder where, Map<String, Object> params,
                           String column, String key, Object value) {
        if (value != null) {
            where.append(" AND ").append(column).append(" = :").append(key).append(" ");
            params.put(key, value);
        }
    }
}
