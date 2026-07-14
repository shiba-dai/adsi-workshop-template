import type { AttendanceResponse } from "@/lib/api-client";

interface AttendanceTableProps {
  records: AttendanceResponse[];
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

export default function AttendanceTable({ records }: AttendanceTableProps) {
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
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
