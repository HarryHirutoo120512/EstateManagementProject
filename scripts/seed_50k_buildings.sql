-- LÀM TRÊN BẢN SAO DB (vd estateadvance_bench), KHÔNG làm trên DB đang dùng.
-- Trước khi chạy: DESCRIBE building; để biết cột NOT NULL, rồi bổ sung vào INSERT.
-- Mã quận / loại tòa nhà / id nhân viên bên dưới là minh họa: thay bằng giá trị thật trong DB.
DELIMITER $$
DROP PROCEDURE IF EXISTS seed_buildings $$
CREATE PROCEDURE seed_buildings(IN total_rows INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE dist VARCHAR(20);
    DECLARE b_type VARCHAR(100);
    DECLARE last_id BIGINT;

    SET autocommit = 0;
    WHILE i <= total_rows DO
        SET dist   = ELT(1 + (i MOD 4), 'QUAN_1', 'QUAN_2', 'QUAN_3', 'QUAN_4');
        SET b_type = ELT(1 + (i MOD 3), 'TANG_TRET', 'NGUYEN_CAN', 'TANG_TRET,NGUYEN_CAN');

        INSERT INTO building (name, street, ward, district, numberofbasement, floorarea, rentprice, type)
        VALUES (CONCAT('Tower ', i, ' ', dist), CONCAT(i, ' Nguyen Hue'),
                CONCAT('Phuong ', 1 + (i MOD 10)), dist,
                1 + (i MOD 4), 200 + (i MOD 15) * 100, 10 + (i MOD 80), b_type);
        SET last_id = LAST_INSERT_ID();

        -- 3 diện tích thuê / tòa => ~150.000 dòng rentarea
        INSERT INTO rentarea (value, buildingid) VALUES
            (100 + (i MOD 5) * 50, last_id),
            (250 + (i MOD 5) * 50, last_id),
            (500 + (i MOD 4) * 100, last_id);

        -- giao ~1/10 tòa nhà cho nhân viên id 3 và 4 (thay bằng id nhân viên thật)
        IF (i MOD 10) = 0 THEN
            INSERT INTO assignmentbuilding (staffid, buildingid) VALUES (3, last_id), (4, last_id);
        END IF;

        IF (i MOD 1000) = 0 THEN COMMIT; END IF;
        SET i = i + 1;
    END WHILE;
    COMMIT;
    SET autocommit = 1;
END $$
DELIMITER ;

CALL seed_buildings(50000);
ANALYZE TABLE building, rentarea, assignmentbuilding;
SELECT COUNT(*) FROM building;
SELECT COUNT(*) FROM rentarea;
