# Unit 03: Attendance（打刻・履歴）

## Phase: B（unit_01 完了後、unit_02 と並列可能）

## 依存

- unit_01（AttendanceRecord Entity, AttendanceRecordRepository, DTO）
- unit_02（SecurityContext からログインユーザーを取得する必要あり）

> **注**: unit_02 の SecurityConfig が完成していなくても、テスト時は `@WithMockUser` やテスト用 SecurityContext で代替可能。Service 層は `employeeId` を引数として受け取るため、認証層との結合は Controller 層のみ。

## ストーリー

| US | 内容 |
|----|------|
| US-1 | 出勤打刻 |
| US-2 | 退勤打刻 |
| US-3 | 勤怠履歴閲覧（月単位） |

## API

| Method | Path | 説明 |
|--------|------|------|
| POST | /api/attendance/clock-in | 出勤打刻 |
| POST | /api/attendance/clock-out | 退勤打刻 |
| GET | /api/attendance/today | 本日の打刻状態 |
| GET | /api/attendance/history?year=&month= | 月次履歴 |

## 成果物

### Service

- `AttendanceService` (interface)
  - `AttendanceResponse clockIn(Long employeeId)`
  - `AttendanceResponse clockOut(Long employeeId)`
  - `Optional<AttendanceResponse> getToday(Long employeeId)`
  - `List<AttendanceResponse> getMonthlyHistory(Long employeeId, int year, int month)`
- `AttendanceServiceImpl`

### Controller

- `AttendanceController` (@RestController)
  - SecurityContext からログインユーザーの employeeId を取得
  - Service に委譲

### ビジネスルール

| ルール | 実装箇所 |
|--------|---------|
| 1日1回出勤 | Service — 同日レコード存在チェック |
| 出勤後に退勤 | Service — clockInTime != null チェック |
| 退勤は1回 | Service — clockOutTime == null チェック |
| サーバー時刻使用 | Service — `LocalDateTime.now()` / `LocalDate.now()` |

### 例外

- `AlreadyClockedInException` → 409
- `NotClockedInException` → 400
- `AlreadyClockedOutException` → 409

### テスト

- `AttendanceServiceImpl` ユニットテスト
  - 出勤打刻: 未打刻 → 成功
  - 出勤打刻: 既に打刻済み → AlreadyClockedInException
  - 退勤打刻: 出勤済み・未退勤 → 成功
  - 退勤打刻: 未出勤 → NotClockedInException
  - 退勤打刻: 既に退勤済み → AlreadyClockedOutException
  - 月次履歴: 指定月のレコードが返る
  - 月次履歴: レコードなし → 空リスト
- `AttendanceController` @WebMvcTest
  - POST /clock-in 正常系 → 200 + AttendanceResponse
  - POST /clock-in 重複 → 409
  - POST /clock-out 正常系 → 200
  - POST /clock-out 未出勤 → 400
  - GET /today 打刻あり → 200
  - GET /today 打刻なし → 204
  - GET /history 正常系 → 200 + リスト

## パッケージ構成（追加分）

```
com.example.attendance
├── service/
│   ├── AttendanceService.java
│   └── impl/
│       └── AttendanceServiceImpl.java
├── controller/
│   └── AttendanceController.java
└── exception/
    ├── AlreadyClockedInException.java
    ├── NotClockedInException.java
    └── AlreadyClockedOutException.java
```

## 完了条件

- [ ] 出勤打刻が正常に記録される
- [ ] 同日の二重出勤打刻が拒否される（409）
- [ ] 退勤打刻が正常に記録される
- [ ] 出勤なしの退勤打刻が拒否される（400）
- [ ] 月次履歴が正しく取得できる
- [ ] テストカバレッジ 80% 以上
