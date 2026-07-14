"use client";

import type { AttendanceDetailResponse } from "@/lib/api-client";

interface AttendanceTableProps {
  records: AttendanceDetailResponse[];
  onEdit?: (record: AttendanceDetailResponse) => void;
}

function formatTime(isoTime: string | null): string {
  if (!isoTime) return "-";
  const date = new Date(isoTime);
  const hours = date.getHours().toString().padStart(2, "0");
  const minutes = date.getMinutes().toString().padStart(2, "0");
  return `${hours}:${minutes}`;
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const weekdays = ["日", "月", "火", "水", "木", "金", "土"];
  const weekday = weekdays[date.getDay()];
  return `${month}/${day} (${weekday})`;
}

function formatMinutes(minutes: number): string {
  if (minutes === 0) return "-";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}:${m.toString().padStart(2, "0")}`;
}

export default function AttendanceTable({ records, onEdit }: AttendanceTableProps) {
  if (records.length === 0) {
    return (
      <p className="text-center text-gray-500 py-8">
        この月の勤怠記録はありません。
      </p>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr className="bg-gray-100">
            <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold text-gray-700">
              日付
            </th>
            <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold text-gray-700">
              出勤
            </th>
            <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold text-gray-700">
              退勤
            </th>
            <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold text-gray-700">
              休憩
            </th>
            <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold text-gray-700">
              実働
            </th>
            <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold text-gray-700">
              残業
            </th>
            {onEdit && (
              <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold text-gray-700">
                操作
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {records.map((record) => (
            <tr key={record.id} className="hover:bg-gray-50">
              <td className="border border-gray-200 px-4 py-2 text-sm text-gray-900">
                {formatDate(record.workDate)}
              </td>
              <td className="border border-gray-200 px-4 py-2 text-sm text-gray-900">
                {formatTime(record.clockInTime)}
              </td>
              <td className="border border-gray-200 px-4 py-2 text-sm text-gray-900">
                {formatTime(record.clockOutTime)}
              </td>
              <td className="border border-gray-200 px-4 py-2 text-sm text-gray-900">
                {formatMinutes(record.breakMinutes)}
              </td>
              <td className="border border-gray-200 px-4 py-2 text-sm text-gray-900">
                {formatMinutes(record.workingMinutes)}
              </td>
              <td className="border border-gray-200 px-4 py-2 text-sm text-gray-900">
                {record.overtimeMinutes > 0 ? (
                  <span className="text-orange-600 font-medium">
                    {formatMinutes(record.overtimeMinutes)}
                  </span>
                ) : (
                  "-"
                )}
              </td>
              {onEdit && (
                <td className="border border-gray-200 px-4 py-2 text-sm">
                  <button
                    onClick={() => onEdit(record)}
                    className="text-blue-600 hover:text-blue-800 hover:underline text-sm"
                  >
                    編集
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
