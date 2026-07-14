# Phase 2 設計 — 打刻修正 + 休憩記録 + 勤務時間計算

## スコープ

| US | 機能 |
|----|------|
| US-4 | 打刻修正（月次承認前は自由修正。Phase 4 で承認ロック導入後に制限） |
| US-5 | 休憩記録（手動記録。12:00-13:00 自動控除は計算ロジックで対応） |
| — | 勤務時間計算（実労働時間・残業・36協定警告） |

---

## 1. ドメインモデル

### 新規 Entity

```
┌─────────────────────────────────┐
│ BreakRecord（休憩記録）          │
├─────────────────────────────────┤
│ id: Long (PK)                   │
│ attendanceRecordId: Long (FK)   │
│ startTime: LocalDateTime        │
│ endTime: LocalDateTime          │
│ version: Long                   │
│ createdAt: LocalDateTime        │
│ updatedAt: LocalDateTime        │
└─────────────────────────────────┘
```

### Value Object

```
┌──────────────────────────┐
│ WorkDuration（勤務時間）  │
├──────────────────────────┤
│ totalMinutes: int         │  退勤 - 出勤（分）
│ breakMinutes: int         │  自動控除 + 手動休憩（分）
│ workingMinutes: int       │  total - break（分）
│ overtimeMinutes: int      │  working - 450（分）※負なら0
└──────────────────────────┘
```

※ 所定労働時間 = 7時間30分 = 450分

### Repository

| Repository | 主なメソッド |
|-----------|-------------|
| BreakRecordRepository | `findByAttendanceRecordId(Long)`, `findByAttendanceRecordIdIn(List<Long>)` |

### Service（拡張）

| Service | 追加メソッド |
|---------|-------------|
| AttendanceService | `updateAttendance(Long employeeId, Long recordId, UpdateAttendanceRequest)` |
| BreakService（新規） | `addBreak(Long employeeId, Long attendanceId, CreateBreakRequest)`, `deleteBreak(Long employeeId, Long breakId)`, `getBreaks(Long attendanceId)` |
| WorkingTimeService（新規） | `calculateDuration(AttendanceRecord, List<BreakRecord>)`, `getMonthlyOvertime(Long employeeId, int year, int month)`, `getYearlyOvertime(Long employeeId, int year)` |

---

## 2. DB 設計

### break_records テーブル（新規）

| カラム | 型 | 制約 | 備考 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — |
| attendance_record_id | BIGINT | NOT NULL, FK(attendance_records.id) | — |
| start_time | TIMESTAMP | NOT NULL | 休憩開始時刻 |
| end_time | TIMESTAMP | NOT NULL | 休憩終了時刻 |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL | — |
| updated_at | TIMESTAMP | NOT NULL | — |

**インデックス:**
- `idx_break_attendance_id`: (attendance_record_id)

### マイグレーション番号

| 番号 | 内容 |
|------|------|
| V4 | `break_records` テーブル作成 |

---

## 3. API 設計

### 打刻修正

| Method | Path | 説明 | Request | Response |
|--------|------|------|---------|----------|
| PUT | /api/attendance/{id} | 打刻修正 | UpdateAttendanceRequest | AttendanceDetailResponse |

### 休憩記録

| Method | Path | 説明 | Request | Response |
|--------|------|------|---------|----------|
| GET | /api/attendance/{attendanceId}/breaks | 休憩一覧 | — | List\<BreakResponse\> |
| POST | /api/attendance/{attendanceId}/breaks | 休憩追加 | CreateBreakRequest | BreakResponse (201) |
| DELETE | /api/attendance/{attendanceId}/breaks/{breakId} | 休憩削除 | — | 204 |

### 勤怠履歴（拡張）

| Method | Path | 説明 | Query | Response |
|--------|------|------|-------|----------|
| GET | /api/attendance/history | 月次履歴（勤務時間含む） | year, month | List\<AttendanceDetailResponse\> |
| GET | /api/attendance/overtime | 月次残業サマリー | year, month | OvertimeSummaryResponse |

---

## 4. DTO 定義

### Request

```java
record UpdateAttendanceRequest(
    @NotNull LocalDateTime clockInTime,
    LocalDateTime clockOutTime,
    @NotNull Long version
) {}

record CreateBreakRequest(
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime
) {}
```

### Response

```java
record AttendanceDetailResponse(
    Long id,
    LocalDate workDate,
    LocalDateTime clockInTime,
    LocalDateTime clockOutTime,
    int workingMinutes,
    int breakMinutes,
    int overtimeMinutes,
    List<BreakResponse> breaks,
    Long version
) {}

record BreakResponse(
    Long id,
    LocalDateTime startTime,
    LocalDateTime endTime,
    int durationMinutes
) {}

record OvertimeSummaryResponse(
    int year,
    int month,
    int monthlyOvertimeMinutes,
    int yearlyOvertimeMinutes,
    boolean monthlyLimitWarning,
    boolean yearlyLimitWarning
) {}
```

---

## 5. 勤務時間計算ロジック

### 実労働時間

```
実労働時間 = (退勤 - 出勤) - 自動控除 - 手動休憩合計
```

### 自動控除ルール

| 条件 | 控除 |
|------|------|
| 12:00〜13:00 に勤務中 AND 当日の勤務が6時間超 | 60分 |
| 上記以外 | 0分 |

「12:00〜13:00 に勤務中」= 出勤時刻 < 13:00 AND 退勤時刻 > 12:00

### 残業

```
残業 = max(0, 実労働時間 - 450分)
```

### 36協定警告

| 閾値 | 条件 |
|------|------|
| 月間上限 | 月の残業合計 >= 2700分（45時間） |
| 年間上限 | 年の残業合計 >= 21600分（360時間） |

警告表示のみ（打刻は拒否しない）。

---

## 6. 画面設計

### 勤怠履歴画面（拡張）

```
┌──────────────────────────────────────────────────────────────┐
│ [◀ 前月] 2026年7月 [翌月 ▶]                                  │
│                                                              │
│ ⚠ 月間残業: 42:30 / 45:00（警告ラインに近づいています）       │
│                                                              │
│ | 日付  | 出勤  | 退勤  | 休憩 | 実働  | 残業  | 操作      |│
│ |-------|-------|-------|------|-------|-------|-----------|│
│ | 7/1   | 09:00 | 18:30 | 1:00 | 8:30  | 1:00  | [編集]   |│
│ | 7/2   | 09:15 | 17:30 | 1:00 | 7:15  | 0:00  | [編集]   |│
│ | ...   |       |       |      |       |       |           |│
└──────────────────────────────────────────────────────────────┘
```

### 打刻修正モーダル

```
┌─────────────────────────────────┐
│ 打刻修正 - 7月1日               │
├─────────────────────────────────┤
│ 出勤時刻: [09:00]              │
│ 退勤時刻: [18:30]              │
│                                 │
│ 休憩記録:                       │
│ | 開始  | 終了  | 時間  | 操作 |│
│ | 12:00 | 13:00 | 1:00  | 自動 |│
│ | 15:00 | 15:15 | 0:15  | [削除]│
│                                 │
│ [＋ 休憩を追加]                  │
│                                 │
│ [キャンセル]        [保存]       │
└─────────────────────────────────┘
```

---

## 7. ビジネスルール

| ルール | 説明 |
|--------|------|
| 修正は自分の打刻のみ | 他人の打刻は修正不可 |
| 月次承認前のみ修正可 | Phase 4 で実装。Phase 2 では常に修正可 |
| 休憩の時間整合性 | start < end であること |
| 休憩は勤務時間内 | start >= clockIn AND end <= clockOut |
| 休憩の重複禁止 | 既存休憩と時間帯が重複する場合はエラー |
| 退勤前は勤務時間を計算しない | clockOutTime が null なら workingMinutes=0 |

---

## 8. エラーハンドリング

| 状況 | HTTPステータス | メッセージ例 |
|------|---------------|-------------|
| 他人の打刻を修正 | 403 | この勤怠記録を修正する権限がありません |
| 存在しない打刻ID | 404 | 指定された勤怠記録が見つかりません |
| 楽観ロック競合 | 409 | 他のユーザーが更新しました。再度お試しください |
| 休憩: start >= end | 400 | 休憩終了時刻は開始時刻より後にしてください |
| 休憩: 勤務時間外 | 400 | 休憩は勤務時間内に設定してください |
| 休憩: 時間帯重複 | 409 | 指定の時間帯は既存の休憩と重複しています |
