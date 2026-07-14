# Phase 3 設計 — 社員マスタ + 部署管理 + カレンダー管理

## スコープ

| US | 機能 |
|----|------|
| US-12 | 社員マスタ管理（追加・編集・無効化、ロール割当、部署割当） |
| US-13 | 部署管理（階層構造の追加・編集・無効化） |
| US-14 | カレンダー管理（土日祝日・会社休日の設定） |

### 前提

- Phase 1 が実装済みであること（`employees`, `attendance_records` テーブルが存在）
- Phase 2 とは独立して実装可能
- 操作権限: システム管理者（role=ADMIN）のみ

---

## 1. ドメインモデル

### Entity

```
┌─────────────────────────────────┐
│ Department（部署）               │
├─────────────────────────────────┤
│ id: Long (PK)                   │
│ name: String                    │
│ parentId: Long (FK, nullable)   │
│ displayOrder: Integer           │
│ enabled: boolean                │
│ version: Long                   │
│ createdAt: LocalDateTime        │
│ updatedAt: LocalDateTime        │
└─────────────────────────────────┘

┌─────────────────────────────────────┐
│ EmployeeDepartment（社員-部署紐付け）│
├─────────────────────────────────────┤
│ id: Long (PK)                       │
│ employeeId: Long (FK)               │
│ departmentId: Long (FK)             │
│ isMain: boolean                     │
│ createdAt: LocalDateTime            │
└─────────────────────────────────────┘

┌─────────────────────────────────┐
│ CalendarHoliday（休日カレンダー） │
├─────────────────────────────────┤
│ id: Long (PK)                   │
│ holidayDate: LocalDate (UK)     │
│ name: String                    │
│ holidayType: HolidayType (ENUM) │
│ version: Long                   │
│ createdAt: LocalDateTime        │
│ updatedAt: LocalDateTime        │
└─────────────────────────────────┘
```

### 既存 Entity への変更

`Employee` エンティティ自体の構造変更は不要。部署との紐付けは中間テーブル `employee_departments` で表現する。

### Enum

```
HolidayType: NATIONAL_HOLIDAY, COMPANY_HOLIDAY
```

### Repository

| Repository | 主なメソッド |
|-----------|-------------|
| DepartmentRepository | `findByEnabledTrue()`, `findByParentId(Long)` |
| EmployeeDepartmentRepository | `findByEmployeeId(Long)`, `findByDepartmentIdAndIsMainTrue(Long)` |
| CalendarHolidayRepository | `findByHolidayDateBetween(LocalDate, LocalDate)`, `findByHolidayDate(LocalDate)` |
| EmployeeRepository（既存拡張） | `findByEnabledTrue()`, `findAll(Pageable)` |

### Service

| Service | 責務 |
|---------|------|
| EmployeeManagementService | 社員の CRUD、ロール変更、部署割当 |
| DepartmentService | 部署の CRUD、階層構造の取得 |
| CalendarService | 休日の登録・削除・年度範囲取得 |

---

## 2. DB 設計

### departments テーブル（新規）

| カラム | 型 | 制約 | 備考 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — |
| name | VARCHAR(100) | NOT NULL | 部署名 |
| parent_id | BIGINT | NULL, FK(departments.id) | 親部署（NULL=最上位） |
| display_order | INT | NOT NULL, DEFAULT 0 | 表示順 |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 無効化フラグ |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL | — |
| updated_at | TIMESTAMP | NOT NULL | — |

**インデックス:**
- `idx_departments_parent_id`: (parent_id)

### employee_departments テーブル（新規）

| カラム | 型 | 制約 | 備考 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — |
| employee_id | BIGINT | NOT NULL, FK(employees.id) | — |
| department_id | BIGINT | NOT NULL, FK(departments.id) | — |
| is_main | BOOLEAN | NOT NULL, DEFAULT FALSE | メイン部署フラグ |
| created_at | TIMESTAMP | NOT NULL | — |

**制約:**
- `uk_emp_dept_unique`: UNIQUE (employee_id, department_id)
- ビジネスルール: 1社員につき is_main=TRUE は1件のみ（アプリ層で制御）

### calendar_holidays テーブル（新規）

| カラム | 型 | 制約 | 備考 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | — |
| holiday_date | DATE | NOT NULL, UNIQUE | 休日の日付 |
| name | VARCHAR(100) | NOT NULL | 休日名（例: 元日、創立記念日） |
| holiday_type | VARCHAR(30) | NOT NULL | NATIONAL_HOLIDAY / COMPANY_HOLIDAY |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL | — |
| updated_at | TIMESTAMP | NOT NULL | — |

### ER 図

```
departments (self-referencing)
  parent_id ──┐
              ↓
departments ||--o{ departments : "親子階層"

employees ||--o{ employee_departments : "社員の所属"
departments ||--o{ employee_departments : "部署のメンバー"

calendar_holidays (独立テーブル)
```

### マイグレーション番号

Phase 2 との衝突を避けるため以下を予約する:

| 番号 | 用途 |
|------|------|
| V4〜V6 | Phase 2 用（予約。触らない） |
| **V7** | `departments` テーブル作成 + シードデータ |
| **V8** | `employee_departments` テーブル作成 + 既存社員の割当 |
| **V9** | `calendar_holidays` テーブル作成 + 当年祝日データ |

---

## 3. API 設計

### 認可

Phase 3 の全 API は **ADMIN ロールのみ**アクセス可能。
SecurityFilterChain に `requestMatchers("/api/admin/**").hasRole("ADMIN")` を追加する。

### 部署管理

| Method | Path | 説明 | Request | Response |
|--------|------|------|---------|----------|
| GET | /api/admin/departments | 部署一覧（ツリー構造） | — | List\<DepartmentTreeResponse\> |
| GET | /api/admin/departments/{id} | 部署詳細 | — | DepartmentResponse |
| POST | /api/admin/departments | 部署作成 | CreateDepartmentRequest | DepartmentResponse (201) |
| PUT | /api/admin/departments/{id} | 部署更新 | UpdateDepartmentRequest | DepartmentResponse |
| PUT | /api/admin/departments/{id}/disable | 部署無効化 | — | 204 |

### 社員マスタ管理

| Method | Path | 説明 | Request | Response |
|--------|------|------|---------|----------|
| GET | /api/admin/employees | 社員一覧（ページネーション） | page, size, keyword | Page\<EmployeeDetailResponse\> |
| GET | /api/admin/employees/{id} | 社員詳細 | — | EmployeeDetailResponse |
| POST | /api/admin/employees | 社員作成 | CreateEmployeeRequest | EmployeeDetailResponse (201) |
| PUT | /api/admin/employees/{id} | 社員更新 | UpdateEmployeeRequest | EmployeeDetailResponse |
| PUT | /api/admin/employees/{id}/disable | 社員無効化 | — | 204 |
| PUT | /api/admin/employees/{id}/departments | 部署割当変更 | AssignDepartmentsRequest | 204 |

### カレンダー管理

| Method | Path | 説明 | Request | Response |
|--------|------|------|---------|----------|
| GET | /api/admin/calendar/holidays | 休日一覧 | year | List\<HolidayResponse\> |
| POST | /api/admin/calendar/holidays | 休日登録 | CreateHolidayRequest | HolidayResponse (201) |
| PUT | /api/admin/calendar/holidays/{id} | 休日更新 | UpdateHolidayRequest | HolidayResponse |
| DELETE | /api/admin/calendar/holidays/{id} | 休日削除 | — | 204 |
| POST | /api/admin/calendar/holidays/bulk | 年間祝日一括登録 | BulkCreateHolidaysRequest | List\<HolidayResponse\> (201) |

---

## 4. DTO 定義

### Request

```java
// 部署
record CreateDepartmentRequest(
    @NotBlank @Size(max = 100) String name,
    Long parentId,
    int displayOrder
) {}

record UpdateDepartmentRequest(
    @NotBlank @Size(max = 100) String name,
    Long parentId,
    int displayOrder,
    @NotNull Long version
) {}

// 社員
record CreateEmployeeRequest(
    @NotBlank @Size(max = 20) String employeeCode,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotNull String role
) {}

record UpdateEmployeeRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email String email,
    @NotNull String role,
    @NotNull Long version
) {}

record AssignDepartmentsRequest(
    @NotNull Long mainDepartmentId,
    List<Long> subDepartmentIds
) {}

// カレンダー
record CreateHolidayRequest(
    @NotNull LocalDate holidayDate,
    @NotBlank @Size(max = 100) String name,
    @NotNull String holidayType
) {}

record UpdateHolidayRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull String holidayType,
    @NotNull Long version
) {}

record BulkCreateHolidaysRequest(
    @NotNull @Size(min = 1) List<CreateHolidayRequest> holidays
) {}
```

### Response

```java
record DepartmentResponse(
    Long id,
    String name,
    Long parentId,
    int displayOrder,
    boolean enabled,
    Long version
) {}

record DepartmentTreeResponse(
    Long id,
    String name,
    int displayOrder,
    boolean enabled,
    List<DepartmentTreeResponse> children
) {}

record EmployeeDetailResponse(
    Long id,
    String employeeCode,
    String name,
    String email,
    String role,
    boolean enabled,
    DepartmentResponse mainDepartment,
    List<DepartmentResponse> subDepartments,
    Long version
) {}

record HolidayResponse(
    Long id,
    LocalDate holidayDate,
    String name,
    String holidayType,
    Long version
) {}
```

---

## 5. 画面設計

### ページ一覧

| パス | ページ | 説明 |
|------|--------|------|
| /admin/employees | 社員一覧 | 検索・ページネーション付き一覧 |
| /admin/employees/new | 社員作成 | フォーム |
| /admin/employees/{id} | 社員編集 | フォーム（部署割当含む） |
| /admin/departments | 部署管理 | ツリー表示 + CRUD |
| /admin/calendar | カレンダー管理 | 年度カレンダービュー + 休日設定 |

### 社員一覧画面

```
┌──────────────────────────────────────────────┐
│ ヘッダー                                      │
├──────────────────────────────────────────────┤
│ 社員管理                                      │
│ [検索: ________] [＋ 新規登録]                │
│                                              │
│ | 社員番号 | 氏名     | メール          | ロール | 部署   | 状態   | 操作 |
│ |----------|----------|----------------|--------|--------|--------|------|
│ | EMP001   | 田中太郎 | tanaka@...     | 一般   | 開発部 | 有効   | [編集] |
│ | EMP002   | 佐藤花子 | sato@...       | 管理者 | 営業部 | 有効   | [編集] |
│ | ...      |          |                |        |        |        |      |
│                                              │
│ [← 前へ] ページ 1/3 [次へ →]                 │
└──────────────────────────────────────────────┘
```

### 部署管理画面

```
┌──────────────────────────────────────────────┐
│ 部署管理                                      │
│ [＋ 新規部署]                                 │
│                                              │
│ ▼ 本社                                       │
│   ├─ ▼ 開発本部                              │
│   │   ├── 第一開発部                          │
│   │   └── 第二開発部                          │
│   ├─ ▼ 営業本部                              │
│   │   └── 営業部                              │
│   └── 管理部                                  │
│                                              │
│ ※各ノードにホバーで [編集] [無効化] 表示       │
└──────────────────────────────────────────────┘
```

### カレンダー管理画面

```
┌──────────────────────────────────────────────┐
│ カレンダー管理                                │
│ [◀ 前年] 2026年 [翌年 ▶] [＋ 休日追加]       │
│                                              │
│ | 日付       | 休日名         | 種別       | 操作       |
│ |------------|---------------|------------|------------|
│ | 2026-01-01 | 元日          | 国民の祝日 | [編集][削除] |
│ | 2026-01-13 | 成人の日      | 国民の祝日 | [編集][削除] |
│ | 2026-04-01 | 創立記念日    | 会社休日   | [編集][削除] |
│ | ...        |               |            |            |
│                                              │
│ [一括登録: 国民の祝日を自動追加]              │
└──────────────────────────────────────────────┘
```

---

## 6. ビジネスルール

| ルール | 説明 |
|--------|------|
| ADMIN 専用 | Phase 3 の全操作は role=ADMIN のみ |
| 部署階層制限 | 最大3階層（本部→部→課） |
| 部署無効化 | 所属社員がいる部署は無効化不可（先に異動が必要） |
| 社員無効化 | 無効化した社員はログイン不可（既存の `enabled` フラグ） |
| メイン部署必須 | 社員は必ず1つのメイン部署を持つ |
| パスワード登録 | 新規作成時のみパスワードを指定。編集時はパスワード変更 API を別途用意（or 管理者リセット） |
| 社員番号ユニーク | employee_code の重複は許可しない |
| 休日日付ユニーク | 同じ日付の休日を重複登録できない |
| 自分自身の無効化禁止 | ADMIN が自分を無効化できない |

---

## 7. エラーハンドリング

| 状況 | HTTPステータス | メッセージ例 |
|------|---------------|-------------|
| ADMIN 以外がアクセス | 403 | 権限がありません |
| 部署名が重複 | 409 | 同じ名前の部署が既に存在します |
| 所属社員ありで部署無効化 | 400 | 所属社員がいるため無効化できません |
| 社員番号重複 | 409 | この社員番号は既に使用されています |
| メール重複 | 409 | このメールアドレスは既に使用されています |
| 休日日付重複 | 409 | この日付は既に登録されています |
| 楽観ロック競合 | 409 | 他のユーザーが更新しました。再度お試しください |
| 自分自身の無効化 | 400 | 自分自身を無効化することはできません |

---

## 8. セキュリティ考慮

- パスワードは必ず BCrypt でハッシュ化して保存
- 社員作成 API のレスポンスにパスワード関連情報を含めない
- ADMIN 権限チェックは SecurityFilterChain + メソッドレベル `@PreAuthorize` の二重防御
- ページネーションの `size` パラメータに上限を設ける（最大100）

---

## 9. Phase 2 との境界

| 項目 | Phase 2 の責務 | Phase 3 の責務 |
|------|---------------|---------------|
| `employees` テーブル | 変更なし | `employee_departments` 中間テーブルで部署を紐付け |
| マイグレーション | V4〜V6 | V7〜V9 |
| SecurityFilterChain | 変更なし | `/api/admin/**` に ADMIN 認可を追加 |
| パッケージ | `attendance` 配下 | `department`, `calendar`, `admin` 配下 |
| フロント URL | `/` 配下 | `/admin/` 配下 |

### 合流時の注意

- SecurityFilterChain の変更が競合する可能性あり → マージ時に手動解決
- `employees` テーブルへのカラム追加は行わない（中間テーブルで対応）
- フロントのナビゲーション（Header コンポーネント）に ADMIN メニューを追加する際、Phase 2 のメニュー追加と競合する可能性あり

---

## 10. テスト方針

| レイヤー | テスト内容 |
|---------|-----------|
| Repository | `@DataJpaTest` で CRUD + 階層検索を検証 |
| Service | ユニットテスト（Repository モック）でビジネスルールを検証 |
| Controller | `@WebMvcTest` で認可（ADMIN 以外は 403）+ リクエストバリデーション |
| 統合テスト | `@SpringBootTest` で部署→社員割当→カレンダー登録の一連フロー |
| フロント | コンポーネントテスト + ツリー表示の操作テスト |
