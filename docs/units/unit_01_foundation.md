# Unit 01: Foundation（共通基盤）

## Phase: A（最初に実装）

## 目的

全 Unit が依存する DB スキーマ・Entity・DTO・共通エラーハンドリングを定義する。

## 成果物

### Flyway マイグレーション

- `V1__create_employees_table.sql`
- `V2__create_attendance_records_table.sql`
- `V3__insert_seed_data.sql` — 開発用テストユーザー

### Entity

- `Employee` — employees テーブル対応
- `AttendanceRecord` — attendance_records テーブル対応

### Enum

- `Role` — EMPLOYEE, MANAGER, ADMIN

### DTO (record)

- `LoginRequest`
- `EmployeeResponse`
- `AttendanceResponse`
- `ErrorResponse`

### Repository (interface)

- `EmployeeRepository extends JpaRepository<Employee, Long>`
  - `Optional<Employee> findByEmail(String email)`
- `AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>`
  - `Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate)`
  - `List<AttendanceRecord> findByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate start, LocalDate end)`

### 共通エラーハンドリング

- `GlobalExceptionHandler` (@RestControllerAdvice)
  - ビジネス例外 → 適切な HTTP ステータス + ErrorResponse
  - バリデーション例外 → 400 + フィールドエラー

### テスト

- `@DataJpaTest`: Repository の基本 CRUD
- Entity のユニーク制約テスト
- Flyway マイグレーション適用確認

## パッケージ構成（案）

```
com.example.attendance
├── entity/
│   ├── Employee.java
│   └── AttendanceRecord.java
├── enums/
│   └── Role.java
├── repository/
│   ├── EmployeeRepository.java
│   └── AttendanceRecordRepository.java
├── dto/
│   ├── LoginRequest.java
│   ├── EmployeeResponse.java
│   ├── AttendanceResponse.java
│   └── ErrorResponse.java
└── exception/
    ├── GlobalExceptionHandler.java
    └── BusinessException.java
```

## 完了条件

- [ ] Flyway マイグレーションが正常に適用される
- [ ] Entity の @DataJpaTest が通る
- [ ] Repository の基本クエリがテストで確認される
- [ ] DTO が record で定義されている
