# Estate Management System

![CI](https://github.com/HarryHirutoo120512/EstateManagementProject/actions/workflows/ci.yml/badge.svg)

Hệ thống quản lý tòa nhà cho thuê và khách hàng cho công ty môi giới bất động sản.
Quản lý (MANAGER) tạo tòa nhà, khách hàng và giao việc; nhân viên sale (STAFF) chỉ thấy dữ liệu được giao.
Tìm kiếm tòa nhà theo 16 tiêu chí, có cache Redis và ghi log hoạt động qua Spring Event (AFTER_COMMIT).

## Kiến trúc

```text
Trình duyệt (JSP) ──► Controller / REST API ──► Service ──► Repository ──► MySQL
                                                  │  │
                                   @Cacheable ────┘  └──── ApplicationEventPublisher ──► ActivityEventListener (AFTER_COMMIT, @Async)
                                       │
                                     Redis
```

## Tech stack

| Nhóm | Công nghệ |
| --- | --- |
| Backend | Java 8, Spring Boot 2.7, Spring MVC, Spring Data JPA, Spring Security |
| Database | MySQL 8 |
| Cache | Redis 7 (128MB, allkeys-lru) |
| Test | JUnit 5, Mockito, JaCoCo |
| DevOps | Docker Compose, GitHub Actions |

## Cách chạy

```bash
# 1. Bật Redis
docker compose up -d

# 2. Tạo database MySQL "estateadvance" và import dữ liệu mẫu
# 3. Khai báo biến môi trường DB_PASSWORD rồi chạy app
cd source
mvn spring-boot:run
```

* Web: http://localhost:8080

## Chạy test

```bash
cd source
mvn clean test
# Báo cáo: source/target/site/jacoco/index.html
```

* **~90 unit test**, chạy < 10 giây, không cần DB.
* Line coverage tầng Service: **≥ 80%** (build fail nếu < 70%).

![JaCoCo](docs/images/jacoco-report.png)

## Kết quả Redis

| Lần tìm kiếm | Nguồn dữ liệu | Thời gian |
| --- | --- | --- |
| Lần 1 | MySQL | X ms |
| Lần 2 (cùng bộ lọc) | Redis | Y ms |

Cache tự xóa khi thêm / sửa / xóa / giao tòa nhà (`@CacheEvict`), TTL 10 phút.
