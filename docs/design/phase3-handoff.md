# Phase 3 引き継ぎ資料

## 概要

Phase 3（社員マスタ + 部署管理 + カレンダー管理）の実装を別担当に委譲するための資料。

---

## 成果物一覧

| ファイル | 内容 |
|---------|------|
| `docs/design/phase3-design.md` | Phase 3 設計ドキュメント（ドメイン/DB/API/画面/ルール） |
| `docs/design/phase3-openapi.yaml` | OpenAPI 定義（Swagger UI で参照可能） |
| `docs/requirements/attendance-app.md` | 確定済み要求仕様（US-12, US-13, US-14 が対象） |

---

## 着手前の確認

1. Phase 1 の実装が `main` ブランチに入っていること
2. `npm run check:backend` が通ること
3. 要求仕様のうち不明点があれば Issue で質問

---

## 開発の進め方

このプロジェクトは SDD（仕様駆動開発）+ TDD で進める。

```
1. 設計確認（phase3-design.md を読む）
2. ブランチ作成（feature/phase3-xxx）
3. TDD で実装（テスト先行）
4. レビュー（/multi-agent-review で確認）
5. PR 作成
```

### 実装順序（推奨）

```
Step 1: DB マイグレーション（V7〜V9）
Step 2: Entity + Repository
Step 3: Service（ビジネスロジック）
Step 4: Controller（REST API）
Step 5: Frontend（管理画面）
```

部署 → 社員マスタ → カレンダーの順が依存関係的にスムーズ。

---

## Phase 2 との境界ルール

| 項目 | 取り決め |
|------|---------|
| マイグレーション番号 | Phase 2: V4〜V6、**Phase 3: V7〜V9** |
| `employees` テーブル | カラム追加しない。`employee_departments` 中間テーブルで対応 |
| SecurityFilterChain | `/api/admin/**` に ADMIN 認可を追加（マージ時に競合する可能性あり） |
| Java パッケージ | `com.example.attendance.department` / `.calendar` / `.admin` |
| フロント URL | `/admin/` 配下（Phase 2 は `/` 配下） |
| Header ナビゲーション | ADMIN メニュー追加時に競合する可能性あり → マージ時に手動解決 |

---

## 環境セットアップ

```bash
# リポジトリクローン後
npm run setup

# バックエンド起動（H2 — Docker不要）
npm run boot:workshop

# フロントエンド起動
npm run dev

# テスト
npm run check:backend
npm run lint:frontend
```

SageMaker 環境の場合:
```bash
npm run dev:sagemaker
```

---

## テストユーザー（シードデータ）

| メール | パスワード | ロール | 用途 |
|--------|-----------|--------|------|
| tanaka@example.com | password123 | EMPLOYEE | — |
| sato@example.com | password123 | MANAGER | — |
| suzuki@example.com | password123 | **ADMIN** | Phase 3 の画面操作 |

---

## 質問・確認先

- 要求仕様の不明点 → GitHub Issue で質問
- Phase 2 との競合が発生した場合 → PR レビューで調整
