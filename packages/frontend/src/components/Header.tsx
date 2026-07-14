"use client";

import { useRouter } from "next/navigation";
import { logout } from "@/lib/api-client";

interface HeaderProps {
  employeeName: string;
}

export default function Header({ employeeName }: HeaderProps) {
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      router.push("/login");
    }
  };

  return (
    <header className="bg-white shadow">
      <div className="mx-auto max-w-4xl px-4 py-4 flex items-center justify-between">
        <h1 className="text-lg font-bold text-gray-900">勤怠管理システム</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{employeeName}</span>
          <button
            onClick={handleLogout}
            className="rounded bg-gray-200 px-3 py-1 text-sm text-gray-700 hover:bg-gray-300 transition-colors"
          >
            ログアウト
          </button>
        </div>
      </div>
    </header>
  );
}
