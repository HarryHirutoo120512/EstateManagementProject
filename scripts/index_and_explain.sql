-- 0. Kiểm tra kiểu cột (quan trọng cho CAST) và index hiện có
SHOW CREATE TABLE rentarea;
SHOW INDEX FROM building;
SHOW INDEX FROM rentarea;
SHOW INDEX FROM assignmentbuilding;

-- 1. ĐO TRƯỚC khi tạo index (ghi lại execution time, rows examined)
EXPLAIN ANALYZE
SELECT b.* FROM building b
WHERE b.district = 'QUAN_1'
  AND b.rentprice >= 10 AND b.rentprice <= 50
  AND EXISTS (SELECT 1 FROM rentarea r
              WHERE r.buildingid = b.id
                AND CAST(r.value AS UNSIGNED) >= 100 AND CAST(r.value AS UNSIGNED) <= 300)
  AND EXISTS (SELECT 1 FROM assignmentbuilding a WHERE a.buildingid = b.id AND a.staffid = 3)
ORDER BY b.id DESC
LIMIT 20 OFFSET 0;

-- 2. Index có chủ đích (cột '=' trước, cột khoảng sau)
CREATE INDEX idx_building_district_price ON building (district, rentprice);
CREATE INDEX idx_rentarea_building ON rentarea (buildingid);
-- Bỏ qua nếu SHOW INDEX cho thấy assignmentbuilding đã có index tương đương
CREATE INDEX idx_assignment_building_staff ON assignmentbuilding (buildingid, staffid);

ANALYZE TABLE building, rentarea, assignmentbuilding;

-- 3. ĐO LẠI đúng câu EXPLAIN ANALYZE ở bước 1 và so sánh
