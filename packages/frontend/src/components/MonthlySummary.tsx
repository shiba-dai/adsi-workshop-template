"use client";

import { useEffect, useState } from "react";
import { getHistory, getOvertimeSummary } from "@/lib/api-client";
import type {
  AttendanceDetailResponse,
  OvertimeSummaryResponse,
} from "@/lib/api-client";

function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}:${m.toString().padStart(2, "0")}`;
}

function getWorkingDaysInMonth(year: number, month: number): number {
  let count = 0;
  const daysInMonth = new Date(year, month, 0).getDate();
  for (let d = 1; d <= daysInMonth; d++) {
    const day = new Date(year, month - 1, d).getDay();
    if (day !== 0 && day !== 6) count++;
  }
  return count;
}

export default function MonthlySummary() {
  const [records, setRecords] = useState<AttendanceDetailResponse[]>([]);
  const [overtime, setOvertime] = useState<OvertimeSummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;

  useEffect(() => {
    async function fetch() {
      try {
        const [historyData, overtimeData] = await Promise.all([
          getHistory(year, month),
          getOvertimeSummary(year, month),
        ]);
        setRecords(historyData);
        setOvertime(overtimeData);
      } catch {
        // サマリーの取得失敗はサイレントに無視
      } finally {
        setIsLoading(false);
      }
    }
    fetch();
  }, [year, month]);

  if (isLoading) {
    return (
      <div className="rounded-lg bg-white p-6 shadow-md">
        <p className="text-gray-400 text-sm text-center">読み込み中...</p>
      </div>
    );
  }

  const attendedDays = records.filter((r) => r.clockInTime).length;
  const totalWorkingDays = getWorkingDaysInMonth(year, month);
  const totalWorkingMinutes = records.reduce(
    (sum, r) => sum + r.workingMinutes,
    0
  );
  const monthlyOvertime = overtime?.monthlyOvertimeMinutes ?? 0;
  const overtimePercent = Math.min(100, Math.round((monthlyOvertime / 2700) * 100));

  return (
    <div className="rounded-lg bg-white p-6 shadow-md">
      <h3 className="text-sm font-semibold text-gray-500 mb-3">
        今月の実績（{month}月）
      </h3>
      <div className="grid grid-cols-3 gap-4 text-center">
        <div>
          <p className="text-2xl font-bold text-gray-900">
            {attendedDays}
            <span className="text-sm font-normal text-gray-500">
              {" "}/ {totalWorkingDays}日
            </span>
          </p>
          <p className="text-xs text-gray-500 mt-1">出勤日数</p>
        </div>
        <div>
          <p className="text-2xl font-bold text-gray-900">
            {formatMinutes(totalWorkingMinutes)}
          </p>
          <p className="text-xs text-gray-500 mt-1">総実働</p>
        </div>
        <div>
          <p className={`text-2xl font-bold ${overtimePercent >= 80 ? "text-orange-600" : "text-gray-900"}`}>
            {formatMinutes(monthlyOvertime)}
          </p>
          <p className="text-xs text-gray-500 mt-1">残業</p>
        </div>
      </div>
      <div className="mt-4">
        <div className="flex justify-between text-xs text-gray-500 mb-1">
          <span>36協定（月45h）</span>
          <span>{overtimePercent}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2">
          <div
            className={`h-2 rounded-full transition-all ${
              overtimePercent >= 80 ? "bg-orange-500" : "bg-blue-500"
            }`}
            style={{ width: `${overtimePercent}%` }}
          />
        </div>
      </div>
    </div>
  );
}
