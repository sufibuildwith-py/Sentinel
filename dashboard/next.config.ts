import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  ...(process.env.VERCEL ? {} : { output: "standalone" as const }),
  env: {
    NEXT_PUBLIC_SENTINEL_BUILD_SHA:
      process.env.VERCEL_GIT_COMMIT_SHA ?? process.env.GIT_COMMIT ?? "development",
  },
};

export default nextConfig;
