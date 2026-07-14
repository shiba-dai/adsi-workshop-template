# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

勤怠管理 Web アプリケーション（50名規模 SMB 向け）。
SDD（仕様駆動開発）＋ TDD で開発する。SageMaker Code Editor 上での開発を前提とする。

- **Backend**: Spring Boot (Java), JPA, Flyway, H2(dev)/PostgreSQL(prod)
- **Frontend**: Next.js, TypeScript, Tailwind CSS
- **インフラ**: AWS CDK (TypeScript)
- **構成**: モノレポ — `packages/` 配下に backend / frontend / infra

## コマンド

```bash
# セットアップ
npm run setup              # backend Gradle + frontend npm + infra npm

# 開発
npm run boot               # Backend 起動 (PostgreSQL)
npm run boot:workshop      # Backend 起動 (H2, Docker不要)
npm run dev                # Frontend dev server
npm run dev:sagemaker      # SageMaker 一括起動 (backend + frontend + proxy)
npm run dev:sagemaker:stop # SageMaker 停止

# 検証
npm run check:backend      # Backend コンパイル + テスト
npm run lint:frontend      # Frontend lint
```

## アーキテクチャ

- **レイヤー**: Controller → Service (interface+impl) → Repository (interface) → Entity
- **DB管理**: Flyway マイグレーション（`ddl-auto` 禁止）
- **DTO**: Java `record` で定義（Lombok `@Data` 禁止）
- **DI**: コンストラクタインジェクションのみ（`@Autowired` フィールドインジェクション禁止）
- **楽観ロック**: `@Version` 標準

## 開発プロセス（SDD）

仕様先行: 要求 → 設計 → (分割) → TDD実装。スキルは明示的に呼び出す:

1. `/requirements` — 要求仕様（ユーザーストーリー、[Question]/[Answer]）
2. `/design` — 設計（ドメイン/DB/API）
3. `/work-decomposition` — Unit of Work 分割
4. `/tdd-implementation` — Plan → 承認 → Red-Green-Refactor
5. `/verify` — 検証
6. `/multi-agent-review` — 並列 subagent レビュー

## 仕様ドキュメント

- `docs/requirements/attendance-app.md` — 確定済み要求仕様
- `docs/working/requirements/` — Q&A 作業ドキュメント
- `docs/design/phase1-design.md` — Phase 1 設計（ドメイン/DB/API/画面）
- `docs/design/phase1-openapi.yaml` — Phase 1 OpenAPI 定義
- `docs/units/` — Unit of Work 定義（Phase 1: 4 Unit）

## 実装フェーズ

| Phase | スコープ |
|-------|---------|
| **1（最優先）** | 認証 (ID/PW) + 出退勤打刻 + 勤怠履歴閲覧 |
| 2 | 打刻修正 + 休憩記録 + 勤務時間計算（残業・36協定警告） |
| 3 | 社員マスタ + 部署管理 + カレンダー管理 |
| 4 | 月次承認 + 有給管理 + 管理者一覧 + CSV レポート |

## SageMaker プレビュー

### 起動

```bash
npm run dev:sagemaker        # Backend(H2) + Frontend + proxy 一括起動
npm run dev:sagemaker:stop   # 停止
```

### ブラウザアクセス

1. PORTS タブで 3001 の地球儀ボタンをクリック
2. URL を以下の形式に修正（`/codeeditor/default/absports/PORT/` が正しいプレフィックス）:

```
https://<studio-domain>/codeeditor/default/absports/3001/login
```

**注意**: `/codeeditor/default/` は必須。省略すると「Unsupported URL path」になる。

### テストユーザー

| メール | パスワード | ロール |
|--------|-----------|--------|
| tanaka@example.com | password123 | 一般社員 |
| sato@example.com | password123 | 管理者 |
| suzuki@example.com | password123 | システム管理者 |

### トラブルシューティング

- **「Unsupported URL path」**: URL に `/codeeditor/default` が含まれているか確認
- **308リダイレクトでループ**: ブラウザキャッシュをクリア（Ctrl+Shift+Delete）
- **Frontend 500エラー**: `rm -rf packages/frontend/.next` で再起動
