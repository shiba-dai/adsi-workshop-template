# Unit 02: Auth（認証）

## Phase: B（unit_01 完了後、unit_03 と並列可能）

## 依存

- unit_01（Employee Entity, EmployeeRepository, DTO）

## ストーリー

- ログイン（ID/PW → セッション開始）
- ログアウト（セッション破棄）
- ログインユーザー情報取得

## API

| Method | Path | 説明 |
|--------|------|------|
| POST | /api/auth/login | ログイン → EmployeeResponse（セッション Cookie 発行） |
| POST | /api/auth/logout | ログアウト → 204（セッション破棄） |
| GET | /api/auth/me | 認証済みユーザー情報 → EmployeeResponse |

## 成果物

### Service

- `AuthService` (interface)
  - `EmployeeResponse login(LoginRequest request)`
- `AuthServiceImpl`
  - email で Employee 検索 → BCrypt 照合 → セッションに認証情報をセット

### Controller

- `AuthController` (@RestController)

### Security 設定

- `SecurityConfig` — SecurityFilterChain Bean
  - `/api/auth/login` → permitAll
  - `/api/**` → authenticated
  - CSRF 無効（REST API）
  - セッション管理: デフォルト（IF_REQUIRED）
  - 未認証時: 401 を返す（リダイレクトしない）

### テスト

- `AuthServiceImpl` ユニットテスト
  - 正常ログイン → EmployeeResponse 返却
  - 存在しないメール → 401
  - パスワード不一致 → 401
  - 無効化ユーザー → 403
- `AuthController` @WebMvcTest
  - POST /login 正常系 → 200 + EmployeeResponse
  - POST /login 異常系 → 401
  - GET /me 認証済み → 200 + EmployeeResponse
  - GET /me 未認証 → 401
  - POST /logout → 204

### Seed データ

`V3__insert_seed_data.sql` に以下を含める（unit_01 で作成）:

```sql
-- パスワード: password123 (BCrypt)
INSERT INTO employees (employee_code, name, email, password_hash, role, enabled, version, created_at, updated_at)
VALUES ('EMP001', '田中太郎', 'tanaka@example.com', '$2a$10$...', 'EMPLOYEE', true, 0, NOW(), NOW());
```

## パッケージ構成（追加分）

```
com.example.attendance
├── service/
│   ├── AuthService.java
│   └── impl/
│       └── AuthServiceImpl.java
├── controller/
│   └── AuthController.java
└── security/
    └── SecurityConfig.java
```

## 完了条件

- [ ] ログイン成功時にセッションが作成され EmployeeResponse が返る
- [ ] 不正な認証情報で 401 が返る
- [ ] セッション Cookie 付きリクエストで /api/auth/me が成功する
- [ ] セッションなしリクエストで 401 が返る
- [ ] ログアウトでセッションが破棄される
- [ ] テストカバレッジ 80% 以上
