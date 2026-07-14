# Unit 04: Frontend（画面）

## Phase: C（unit_02, unit_03 完了後）

## 依存

- unit_02（認証 API — ログイン/ログアウト/me）
- unit_03（打刻・履歴 API）

## ストーリー

| US | 内容 |
|----|------|
| — | ログイン画面 |
| US-1 | 出勤打刻（ボタン1クリック） |
| US-2 | 退勤打刻（ボタン1クリック） |
| US-3 | 勤怠履歴閲覧（月単位） |

## ページ

| パス | ページ | 認証 |
|------|--------|------|
| /login | ログイン | 不要 |
| / | ダッシュボード（打刻） | 必要 |
| /history | 勤怠履歴 | 必要 |

## 成果物

### 共通

- `lib/api-client.ts` — fetch ラッパー（credentials: 'include'、basePath 対応、エラーハンドリング）
- `middleware.ts` — 未認証時（401）の /login リダイレクト

### ログイン画面 (/login)

- email + password フォーム
- バリデーション（空チェック）
- ログイン成功 → セッション Cookie 自動付与 → / へ遷移
- ログイン失敗 → エラーメッセージ表示

### ダッシュボード (/)

- ヘッダー: ユーザー名 + ログアウトボタン
- 現在時刻表示（リアルタイム更新）
- 本日の打刻状態（GET /today）
- 出勤ボタン / 退勤ボタン（状態に応じた活性/非活性）
- 打刻成功時の即時 UI 更新

### 勤怠履歴 (/history)

- 月選択（前月/翌月ナビゲーション）
- 一覧テーブル（日付、出勤、退勤、状態）
- 所定時間未満の日に注意表示（簡易計算）

### テスト

- コンポーネントテスト（Vitest + Testing Library）
  - ログインフォーム: 入力 → 送信 → API 呼び出し
  - 打刻ボタン: 状態に応じた活性/非活性
  - 履歴テーブル: データ表示
- API モック: MSW

## ディレクトリ構成（案）

```
src/
├── app/
│   ├── layout.tsx
│   ├── page.tsx              # ダッシュボード
│   ├── login/
│   │   └── page.tsx
│   └── history/
│       └── page.tsx
├── components/
│   ├── Header.tsx
│   ├── ClockInButton.tsx
│   ├── ClockOutButton.tsx
│   └── AttendanceTable.tsx
├── lib/
│   └── api-client.ts
└── middleware.ts
```

## 完了条件

- [ ] ログイン → セッション Cookie → 認証付き API 呼び出しが動作する
- [ ] 未認証で / にアクセスすると /login にリダイレクトされる
- [ ] 出勤/退勤ボタンが状態に応じて正しく動作する
- [ ] 勤怠履歴が月単位で表示される
- [ ] SageMaker プレビューで動作確認できる（basePath 対応）
- [ ] コンポーネントテストが通る
