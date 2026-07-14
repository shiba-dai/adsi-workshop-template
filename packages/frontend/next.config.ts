import type { NextConfig } from "next";

const isSageMaker = !!process.env.SAGEMAKER || !!process.env.SAGEMAKER_APP_TYPE;
const fullPrefix = "/codeeditor/default/absports/3000";

const nextConfig: NextConfig = {
  basePath: isSageMaker ? fullPrefix : undefined,
  assetPrefix: isSageMaker ? fullPrefix : undefined,
  skipTrailingSlashRedirect: isSageMaker || undefined,
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
