# Phase 1 設計 — 認証 + 打刻 + 履歴閲覧

## スコープ

| US | 機能 |
|----|------|
| — | ID/パスワード認証（ログイン/ログアウト） |
| US-1 | 出勤打刻 |
| US-2 | 退勤打刻 |
| US-3 | 勤怠履歴閲覧（月単位）※ Phase 1 は出退勤時刻のみ。勤務時間・残業時間は Phase 2 で追加 |

---

## 1. ドメインモデル

### Entity

```
┌─────────────────────────────┐
│ Employee（社員）             │
├─────────────────────────────┤
│ id: Long (PK)               │
│ employeeCode: String (UK)   │
│ name: String                │
│ email: String (UK)          │
│ passwordHash: String        │
│ role: Role (ENUM)           │
│ enabled: boolean            │
│ version: Long               │
│ createdAt: LocalDateTime    │
│ updatedAt: LocalDateTime    │
└─────────────────────────────┘

┌─────────────────────────────┐
│ AttendanceRecord（勤怠記録） │
├─────────────────────────────┤
│ id: Long (PK)               │
│ employeeId: Long (FK)       │
│ workDate: LocalDate (UK*)   │
│ clockInTime: LocalDateTime  │
│ clockOutTime: LocalDateTime │
│ version: Long               │
│ createdAt: LocalDateTime    │
│ updatedAt: LocalDateTime    │
└─────────────────────────────┘
* UK: (employeeId, workDate) の複合ユニーク
```

### Enum

```
Role: EMPLOYEE, MANAGER, ADMIN
```

### Value Object（Phase 1 では不要）

勤務時間計算は Phase 2 で導入するため、Phase 1 では打刻時刻の記録のみ。

### Repository

| Repository | 主なメソッド |
|-----------|-------------|
| EmployeeRepository | `findByEmail(String)` |
| AttendanceRecordRepository | `findByEmployeeIdAndWorkDate(Long, LocalDate)`, `findByEmployeeIdAndWorkDateBetween(Long, LocalDate, LocalDate)` |

### Service

| Service | 責務 |
|---------|------|
| AuthService | ログイン認証、セッション管理 |
| AttendanceService | 打刻処理（出勤/退勤）、履歴取得 |

---

## 2. DB 設計

### employees テーブル

| カラム | 型 | 制約 | 備考 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — |
| employee_code | VARCHAR(20) | NOT NULL, UNIQUE | 社員番号 |
| name | VARCHAR(100) | NOT NULL | 氏名 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | ログインID |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'EMPLOYEE' | EMPLOYEE/MANAGER/ADMIN |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 無効化フラグ |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL | — |
| updated_at | TIMESTAMP | NOT NULL | — |

### attendance_records テーブル

| カラム | 型 | 制約 | 備考 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — |
| employee_id | BIGINT | NOT NULL, FK(employees.id) | — |
| work_date | DATE | NOT NULL | 勤務日 |
| clock_in_time | TIMESTAMP | NOT NULL | 出勤時刻（レコード作成時に必ずセット） |
| clock_out_time | TIMESTAMP | NULL | 退勤時刻 |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL | — |
| updated_at | TIMESTAMP | NOT NULL | — |

**インデックス:**
- `uk_attendance_employee_date`: UNIQUE (employee_id, work_date)
- `idx_attendance_employee_date_range`: (employee_id, work_date) — 月次検索用

### ER 図

```
employees ||--o{ attendance_records : "1人が複数日の勤怠を持つ"
```

---

## 3. API 設計

### 認証

| Method | Path | 説明 | Request | Response |
|--------|------|------|---------|----------|
| POST | /api/auth/login | ログイン（セッション開始） | LoginRequest | EmployeeResponse |
| POST | /api/auth/logout | ログアウト（セッション破棄） | — | 204 |
| GET | /api/auth/me | ログインユーザー情報 | — | EmployeeResponse |

### 打刻

| Method | Path | 説明 | Request | Response |
|--------|------|------|---------|----------|
| POST | /api/attendance/clock-in | 出勤打刻 | — | AttendanceResponse |
| POST | /api/attendance/clock-out | 退勤打刻 | — | AttendanceResponse |
| GET | /api/attendance/today | 本日の打刻状態 | — | AttendanceResponse (未打刻なら null フィールド) or 204 |

### 勤怠履歴

| Method | Path | 説明 | Query | Response |
|--------|------|------|-------|----------|
| GET | /api/attendance/history | 月次履歴取得 | year, month | List\<AttendanceResponse\> |

---

## 4. DTO 定義

### Request

```java
// ログイン
record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}
```

### Response

```java
record EmployeeResponse(
    Long id,
    String employeeCode,
    String name,
    String email,
    String role
) {}

record AttendanceResponse(
    Long id,
    LocalDate workDate,
    LocalDateTime clockInTime,
    LocalDateTime clockOutTime
) {}
```

---

## 5. 認証方式

| 項目 | 仕様 |
|------|------|
| 方式 | セッションベース認証（HTTPセッション + Cookie） |
| セッション管理 | Spring Security デフォルト（サーバーサイドセッション） |
| セッションタイムアウト | 30分（application.properties で設定） |
| セッション固定攻撃対策 | Spring Security デフォルト（changeSessionId）を使用 |
| パスワードハッシュ | BCrypt |
| Spring Security | SecurityFilterChain Bean |
| Cookie | SameSite=Lax（CSRF 無効化の根拠） |
| 認可 | Phase 1 では全認証済みユーザーが同じ機能を使える |

### SecurityFilterChain 設定（概要）

```
permitAll: POST /api/auth/login
authenticated: /api/** (上記以外)
CSRF: 無効（SameSite=Lax Cookie により cross-site リクエストが防御されるため）
Session Fixation: changeSessionId（デフォルト）
```

---

## 6. 画面設計

### ページ一覧

| パス | ページ | 説明 |
|------|--------|------|
| /login | ログイン | email + password フォーム |
| / | ダッシュボード（打刻） | 打刻ボタン + 本日の状態表示 |
| /history | 勤怠履歴 | 月単位の一覧テーブル |

### ダッシュボード（打刻画面）

```
┌──────────────────────────────────────┐
│ ヘッダー: [ユーザー名] [ログアウト]    │
├──────────────────────────────────────┤
│                                      │
│   現在時刻: 2026-07-14 09:00:00     │
│                                      │
│   本日の状態: 未出勤 / 出勤済 / 退勤済 │
│                                      │
│   ┌──────────┐  ┌──────────┐        │
│   │  出勤    │  │  退勤    │        │
│   └──────────┘  └──────────┘        │
│                                      │
│   出勤時刻: 09:00:00                 │
│   退勤時刻: --:--:--                 │
│                                      │
├──────────────────────────────────────┤
│ フッター: [履歴を見る]               │
└──────────────────────────────────────┘
```

- 出勤ボタン: 未出勤時のみ活性
- 退勤ボタン: 出勤済かつ未退勤時のみ活性
- 退勤済の場合は両ボタン非活性 + 「本日の勤怠は完了しています」

### 勤怠履歴画面

```
┌──────────────────────────────────────┐
│ ヘッダー                             │
├──────────────────────────────────────┤
│ [◀ 前月] 2026年7月 [翌月 ▶]         │
│                                      │
│ | 日付  | 出勤   | 退勤   | 状態    |│
│ |-------|--------|--------|---------|│
│ | 07/01 | 09:00  | 18:00  | ✓      |│
│ | 07/02 | 09:15  | 17:30  | ⚠ 不足 |│
│ | 07/03 | --     | --     | 未打刻  |│
│ | ...   |        |        |         |│
└──────────────────────────────────────┘
```

- Phase 1 では「勤務時間」「残業時間」列は表示しない（Phase 2 で追加）
- 月切り替えで API を再取得

---

## 7. ビジネスルール（Phase 1 スコープ）

| ルール | 説明 |
|--------|------|
| 1日1回打刻 | 同一 employee_id + work_date で既にレコードがあれば出勤打刻を拒否 |
| 出勤→退勤の順序 | clock_in_time が null のレコードに対して退勤打刻は不可 |
| 退勤は1回のみ | clock_out_time が既に入っていれば退勤打刻を拒否 |
| 打刻時刻 | サーバー側の現在時刻を使用（クライアント送信時刻は使わない） |
| 日付判定 | 打刻時のサーバー日付を work_date とする |

---

## 8. エラーハンドリング

| 状況 | HTTPステータス | メッセージ例 |
|------|---------------|-------------|
| 認証失敗 | 401 | メールアドレスまたはパスワードが正しくありません |
| セッション期限切れ/未認証 | 401 | セッションが期限切れです。再度ログインしてください |
| 既に出勤打刻済み | 409 Conflict | 本日は既に出勤打刻されています |
| 出勤なしで退勤 | 400 | 出勤打刻がされていません |
| 既に退勤打刻済み | 409 Conflict | 本日は既に退勤打刻されています |
| 無効なユーザー | 403 | アカウントが無効化されています |

---

## 9. Phase 1 で作らないもの（次フェーズ以降）

- 勤務時間の自動計算（Phase 2）
- 休憩控除ロジック（Phase 2）
- 残業・36協定警告（Phase 2）
- 打刻修正機能（Phase 2）
- 部署・組織構造（Phase 3）
- 月次承認ワークフロー（Phase 4）
