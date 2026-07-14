# Unit of Work — Phase 1

## 依存図

```
Phase A: インターフェース定義（共通基盤）
  └─ unit_01_foundation
       ├── Flyway マイグレーション（employees, attendance_records）
       ├── Entity（Employee, AttendanceRecord）
       ├── Enum（Role）
       ├── DTO（全 record）
       ├── Repository interface
       └── 共通エラーハンドリング（@RestControllerAdvice）

Phase B: 独立実装（並列可能）
  ├─ unit_02_auth        ← unit_01 に依存
  │    └── AuthService, AuthController, SecurityFilterChain
  │
  └─ unit_03_attendance  ← unit_01 に依存
       └── AttendanceService, AttendanceController

Phase C: フロントエンド（Phase B 完了後）
  └─ unit_04_frontend    ← unit_02, unit_03 に依存
       └── ログイン画面, 打刻画面, 履歴画面, API クライアント
```

## Unit 一覧

| Unit | 名前 | Phase | 依存先 | ストーリー |
|------|------|-------|--------|-----------|
| unit_01 | Foundation（共通基盤） | A | なし | — |
| unit_02 | Auth（認証） | B | unit_01 | ログイン/ログアウト |
| unit_03 | Attendance（打刻・履歴） | B | unit_01 | US-1, US-2, US-3 |
| unit_04 | Frontend（画面） | C | unit_02, unit_03 | US-1, US-2, US-3 + ログイン |

## 実装順序

1. **unit_01** → 基盤が揃う
2. **unit_02 と unit_03** → 並列実装可能
3. **unit_04** → Backend API が揃った後
4. **統合テスト** → 全 Unit 完了後
