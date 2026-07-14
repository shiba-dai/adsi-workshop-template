"use client";

import { useEffect, useState, useCallback } from "react";
import Header from "@/components/Header";
import AttendanceTable from "@/components/AttendanceTable";
import EditAttendanceModal from "@/components/EditAttendanceModal";
import WorkingTimeChart from "@/components/WorkingTimeChart";
import { getMe, getHistory, getOvertimeSummary } from "@/lib/api-client";
import { navigateTo, getFullPath } from "@/lib/navigation";
import type {
  EmployeeResponse,
  AttendanceDetailResponse,
  OvertimeSummaryResponse,
} from "@/lib/api-client";

function formatMinutesLabel(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}:${m.toString().padStart(2, "0")}`;
}

export default function HistoryPage() {
  const [employee, setEmployee] = useState<EmployeeResponse | null>(null);
  const [records, setRecords] = useState<AttendanceDetailResponse[]>([]);
  const [overtime, setOvertime] = useState<OvertimeSummaryResponse | null>(null);
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [month, setMonth] = useState(() => new Date().getMonth() + 1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [editingRecord, setEditingRecord] =
    useState<AttendanceDetailResponse | null>(null);

  const fetchEmployee = useCallback(async () => {
    try {
      const me = await getMe();
      setEmployee(me);
    } catch (e: unknown) {
      const apiError = e as { status?: number };
      if (apiError?.status === 401) {
        navigateTo("/login");
        return;
      }
      setError("ユーザー情報の取得に失敗しました。");
    }
  }, []);

  const fetchHistory = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      const [data, overtimeData] = await Promise.all([
        getHistory(year, month),
        getOvertimeSummary(year, month),
      ]);
      setRecords(data);
      setOvertime(overtimeData);
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

  const handleEdit = (record: AttendanceDetailResponse) => {
    setEditingRecord(record);
  };

  const handleEditSaved = () => {
    setEditingRecord(null);
    fetchHistory();
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

      <main className="mx-auto max-w-5xl px-4 py-8">
        <div className="mb-4">
          <a
            href={getFullPath("/")}
            className="text-blue-600 hover:text-blue-800 hover:underline text-sm"
          >
            ダッシュボードに戻る
          </a>
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

          {overtime && (overtime.monthlyLimitWarning || overtime.yearlyLimitWarning) && (
            <div className="mb-4 rounded bg-orange-50 border border-orange-200 p-3 text-sm text-orange-800">
              {overtime.monthlyLimitWarning && (
                <p>
                  ⚠ 月間残業: {formatMinutesLabel(overtime.monthlyOvertimeMinutes)} / 45:00
                  （36協定上限に達しています）
                </p>
              )}
              {overtime.yearlyLimitWarning && (
                <p>
                  ⚠ 年間残業: {formatMinutesLabel(overtime.yearlyOvertimeMinutes)} / 360:00
                  （36協定年間上限に達しています）
                </p>
              )}
            </div>
          )}

          {overtime && !overtime.monthlyLimitWarning && overtime.monthlyOvertimeMinutes > 0 && (
            <div className="mb-4 text-sm text-gray-600">
              月間残業: {formatMinutesLabel(overtime.monthlyOvertimeMinutes)} / 45:00
            </div>
          )}

          {isLoading ? (
            <p className="text-center text-gray-500 py-8">読み込み中...</p>
          ) : (
            <>
              <WorkingTimeChart records={records} />
              <AttendanceTable records={records} onEdit={handleEdit} />
            </>
          )}
        </div>
      </main>

      {editingRecord && (
        <EditAttendanceModal
          record={editingRecord}
          onClose={() => setEditingRecord(null)}
          onSaved={handleEditSaved}
        />
      )}
    </div>
  );
}
