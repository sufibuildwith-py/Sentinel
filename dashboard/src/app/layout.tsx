import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "@/components/providers";

export const metadata: Metadata = {
  title: "Sentinel Revenue Intelligence",
  description: "Test Mode revenue recovery command center",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full" suppressHydrationWarning>
      <body className="min-h-full"><Providers>{children}</Providers></body>
    </html>
  );
}
