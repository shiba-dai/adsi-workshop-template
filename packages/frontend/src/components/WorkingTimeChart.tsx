"use client";

import type { AttendanceDetailResponse } from "@/lib/api-client";

interface WorkingTimeChartProps {
  records: AttendanceDetailResponse[];
}

const STANDARD_MINUTES = 450;
const MAX_DISPLAY_MINUTES = 720;

export default function WorkingTimeChart({ records }: WorkingTimeChartProps) {
  if (records.length === 0) return null;

  const maxMinutes = Math.max(
    MAX_DISPLAY_MINUTES,
    ...records.map((r) => r.workingMinutes)
  );

  return (
    <div className="rounded-lg bg-white p-6 shadow-md mb-6">
      <h3 className="text-sm font-semibold text-gray-500 mb-4">
        日別勤務時間
      </h3>
      <div className="flex items-end gap-1 h-32 relative">
        {/* 所定労働時間ライン */}
        <div
          className="absolute left-0 right-0 border-t border-dashed border-gray-400 pointer-events-none"
          style={{ bottom: `${(STANDARD_MINUTES / maxMinutes) * 100}%` }}
        >
          <span className="absolute -top-4 right-0 text-xs text-gray-400">
            7.5h
          </span>
        </div>

        {records.map((record) => {
          const total = record.workingMinutes;
          const standard = Math.min(total, STANDARD_MINUTES);
          const over = Math.max(0, total - STANDARD_MINUTES);
          const standardHeight = (standard / maxMinutes) * 100;
          const overHeight = (over / maxMinutes) * 100;
          const day = new Date(record.workDate).getDate();

          return (
            <div
              key={record.id}
              className="flex-1 flex flex-col items-center justify-end h-full group relative"
            >
              {/* ツールチップ */}
              <div className="absolute -top-8 hidden group-hover:block bg-gray-800 text-white text-xs rounded px-2 py-1 whitespace-nowrap z-10">
                {Math.floor(total / 60)}:{(total % 60).toString().padStart(2, "0")}
                {over > 0 && ` (残業 ${Math.floor(over / 60)}:${(over % 60).toString().padStart(2, "0")})`}
              </div>
              {/* 残業部分 */}
              {over > 0 && (
                <div
                  className="w-full bg-orange-400 rounded-t"
                  style={{ height: `${overHeight}%` }}
                />
              )}
              {/* 所定部分 */}
              {total > 0 && (
                <div
                  className={`w-full bg-blue-400 ${over > 0 ? "" : "rounded-t"} rounded-b`}
                  style={{ height: `${standardHeight}%` }}
                />
              )}
              {/* 日付ラベル */}
              <span className="text-xs text-gray-400 mt-1 leading-none">
                {day}
              </span>
            </div>
          );
        })}
      </div>
      <div className="flex items-center gap-4 mt-3 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-blue-400 rounded" />
          所定内
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-orange-400 rounded" />
          残業
        </span>
      </div>
    </div>
  );
}
