import type { NextConfig } from "next";

// SageMaker 判定: SAGEMAKER_APP_TYPE が存在すれば SageMaker 環境
const isSageMaker = !!process.env.SAGEMAKER_APP_TYPE;
const smPort = process.env.SAGEMAKER_PORT || "3001";
const basePath = isSageMaker ? `/absports/${smPort}` : (process.env.NEXT_PUBLIC_BASE_PATH || undefined);
const assetPrefix = isSageMaker ? `/codeeditor/default/absports/${smPort}` : (process.env.NEXT_PUBLIC_ASSET_PREFIX || undefined);

const nextConfig: NextConfig = {
  basePath,
  assetPrefix,
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
};

export default nextConfig;
