/**
 * SageMaker Code Editor 環境でのページナビゲーションユーティリティ。
 *
 * SageMaker のプロキシ構造:
 *   ブラウザ URL:   /codeeditor/default/absports/3001/login
 *   プロキシ → Next.js: /absports/3001/login (codeeditor/default が剥がれる)
 *   Next.js basePath: /absports/3001 → ページパス /login
 *
 * Next.js の内部リダイレクト (308等) は basePath のみ (/absports/3001) で返すため、
 * ブラウザが /absports/3001/... に直接アクセスし SageMaker が拒否する。
 *
 * このユーティリティは window.location.pathname からフルプレフィックス
 * (/codeeditor/default/absports/3001) を検出し、プログラマティックな遷移に使う。
 */

const SAGEMAKER_PATH_PATTERN = /^(\/codeeditor\/[^/]+\/absports\/\d+)/;
const BASE_PATH_PATTERN = /^(\/.*\/absports\/\d+)/;

/**
 * ブラウザの現在のURLからSageMakerのフルプレフィックスを検出する。
 * 例: /codeeditor/default/absports/3001
 *
 * SageMaker環境でない場合は NEXT_PUBLIC_BASE_PATH またはブランクを返す。
 */
export function getNavigationPrefix(): string {
  if (typeof window === "undefined") {
    return process.env.NEXT_PUBLIC_BASE_PATH ?? "";
  }

  const pathname = window.location.pathname;

  const sagemakerMatch = pathname.match(SAGEMAKER_PATH_PATTERN);
  if (sagemakerMatch) {
    return sagemakerMatch[1];
  }

  const baseMatch = pathname.match(BASE_PATH_PATTERN);
  if (baseMatch) {
    return baseMatch[1];
  }

  return process.env.NEXT_PUBLIC_BASE_PATH ?? "";
}

/**
 * 相対パスにSageMakerのフルプレフィックスを付与したパスを返す。
 *
 * @param relativePath - basePath以降のパス。例: "/login", "/history", "/"
 * @returns フルパス。例: "/codeeditor/default/absports/3001/login"
 */
export function getFullPath(relativePath: string): string {
  const prefix = getNavigationPrefix();
  if (relativePath === "/" || relativePath === "") {
    return prefix || "/";
  }
  const normalizedPath = relativePath.startsWith("/")
    ? relativePath
    : `/${relativePath}`;
  return `${prefix}${normalizedPath}`;
}

/**
 * プログラマティックにページ遷移する（window.location.href を使用）。
 * SageMakerプレフィックスを自動付与する。
 *
 * @param relativePath - basePath以降のパス。例: "/login", "/", "/history"
 */
export function navigateTo(relativePath: string): void {
  window.location.href = getFullPath(relativePath);
}
