# 開発環境セットアップ & 起動ガイド

## 前提条件

- SageMaker Studio Code Editor 上で作業する想定
- Java 21, Node.js 20, npm が利用可能
- Docker 不要（H2 インメモリ DB を使用）

---

## 1. 初回セットアップ

```bash
cd adsi-workshop-template
npm run setup
```

Backend の Gradle ビルドと Frontend の npm install が実行される。

---

## 2. 起動

```bash
npm run dev:sagemaker
```

以下の3プロセスが一括起動する:

| プロセス | ポート | 役割 |
|---------|--------|------|
| Spring Boot (H2) | 8080 | Backend API |
| Next.js | 3001 | Frontend (内部) |
| proxy.js | 3000 | SageMaker URL を Backend/Frontend に振り分け |

---

## 3. ブラウザでアクセス

### 手順

1. Code Editor の左サイドバー「**PORTS**」タブを開く
2. ポート **3000** の行にある **地球儀ボタン** (Open in Browser) をクリック
3. ブラウザで開いた URL を **以下のように書き換える**

### URL の書き換えルール

地球儀で開くと以下のような URL になる:

```
https://<your-id>.studio.ap-northeast-1.sagemaker.aws/codeeditor/default/ports/3000/
```

**`ports` を `absports` に置換する:**

```
https://<your-id>.studio.ap-northeast-1.sagemaker.aws/codeeditor/default/absports/3000/
```

> `ports` のままだと「Unsupported URL path」エラーになる。

### ログインページへ直接アクセスする場合

```
https://<your-id>.studio.ap-northeast-1.sagemaker.aws/codeeditor/default/absports/3000/login
```

---

## 4. テストユーザー

| メール | パスワード | ロール | 用途 |
|--------|-----------|--------|------|
| tanaka@example.com | password123 | 一般社員 | 打刻・履歴閲覧 |
| sato@example.com | password123 | 管理者 | （Phase 4 で使用） |
| suzuki@example.com | password123 | システム管理者 | （Phase 3 で使用） |

---

## 5. 停止

```bash
npm run dev:sagemaker:stop
```

---

## 6. 画面一覧

| URL パス | 画面 | 説明 |
|---------|------|------|
| `/login` | ログイン | メール + パスワード |
| `/` | ダッシュボード | 打刻ボタン + 今月サマリー |
| `/history` | 勤怠履歴 | 月次テーブル + 勤務時間グラフ + 打刻修正 |

---

## 7. トラブルシューティング

### 「Unsupported URL path」が出る

URL に `/codeeditor/default/absports/3000/` が含まれているか確認。`ports` → `absports` の置換を忘れている可能性が高い。

### 起動時に「Address already in use」

前回のプロセスが残っている。停止してから再起動する:

```bash
npm run dev:sagemaker:stop
sleep 2
npm run dev:sagemaker
```

それでも解消しない場合:

```bash
# 残プロセスを強制終了
pkill -9 -f 'java.*attendance'
pkill -9 -f 'next-server'
pkill -9 -f 'node proxy'
sleep 2
npm run dev:sagemaker
```

### ログイン後に白画面 / 500 エラー

Frontend のビルドキャッシュが壊れている可能性:

```bash
rm -rf packages/frontend/.next
npm run dev:sagemaker:stop
npm run dev:sagemaker
```

### 308 リダイレクトでループする

ブラウザキャッシュをクリアする（Ctrl+Shift+Delete → キャッシュされた画像とファイル）。

---

## 8. テスト実行

```bash
# Backend テスト（JUnit）
npm run check:backend

# Frontend 型チェック
cd packages/frontend && npx tsc --noEmit
```

---

## 9. サンプルデータ

起動時に以下のデータが自動投入される（H2 インメモリのため再起動でリセット）:

- 社員3名（田中・佐藤・鈴木）
- 田中の 7/1〜7/13 出退勤データ（平日9日分）
- 7/9 に手動休憩1件

ダッシュボードの月次サマリーや履歴ページのグラフを確認できる。
