"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import AttendanceTable from "@/components/AttendanceTable";
import { getMe, getHistory } from "@/lib/api-client";
import type { EmployeeResponse, AttendanceResponse } from "@/lib/api-client";

export default function HistoryPage() {
  const [employee, setEmployee] = useState<EmployeeResponse | null>(null);
  const [records, setRecords] = useState<AttendanceResponse[]>([]);
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [month, setMonth] = useState(() => new Date().getMonth() + 1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchEmployee = useCallback(async () => {
    try {
      const me = await getMe();
      setEmployee(me);
    } catch {
      setError("ユーザー情報の取得に失敗しました。");
    }
  }, []);

  const fetchHistory = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getHistory(year, month);
      setRecords(data);
    } catch {
      setError("勤怠履歴の取得に失敗しました。");
    } finally {
      setIsLoading(false);
    }
  }, [year, month]);

  useEffect(() => {
    fetchEmployee();
  }, [fetchEmployee]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const handlePrevMonth = () => {
    if (month === 1) {
      setYear((prev) => prev - 1);
      setMonth(12);
    } else {
      setMonth((prev) => prev - 1);
    }
  };

  const handleNextMonth = () => {
    if (month === 12) {
      setYear((prev) => prev + 1);
      setMonth(1);
    } else {
      setMonth((prev) => prev + 1);
    }
  };

  if (!employee) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <Header employeeName={employee.name} />

      <main className="mx-auto max-w-4xl px-4 py-8">
        <div className="mb-4">
          <Link
            href="/"
            className="text-blue-600 hover:text-blue-800 hover:underline text-sm"
          >
            ダッシュボードに戻る
          </Link>
        </div>

        {error && (
          <div className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="rounded-lg bg-white p-6 shadow-md">
          <div className="mb-6 flex items-center justify-between">
            <button
              onClick={handlePrevMonth}
              className="rounded px-3 py-1 text-gray-700 hover:bg-gray-100 transition-colors"
            >
              前月
            </button>
            <h2 className="text-xl font-bold text-gray-900">
              {year}年{month}月
            </h2>
            <button
              onClick={handleNextMonth}
              className="rounded px-3 py-1 text-gray-700 hover:bg-gray-100 transition-colors"
            >
              翌月
            </button>
          </div>

          {isLoading ? (
            <p className="text-center text-gray-500 py-8">読み込み中...</p>
          ) : (
            <AttendanceTable records={records} />
          )}
        </div>
      </main>
    </div>
  );
}
