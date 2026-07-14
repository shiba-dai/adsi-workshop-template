"use client";

import { useState } from "react";
import type {
  AttendanceDetailResponse,
  BreakResponse,
  CreateBreakRequest,
} from "@/lib/api-client";
import {
  updateAttendance,
  addBreak,
  deleteBreak,
} from "@/lib/api-client";

interface EditAttendanceModalProps {
  record: AttendanceDetailResponse;
  onClose: () => void;
  onSaved: () => void;
}

function toTimeInput(isoTime: string | null): string {
  if (!isoTime) return "";
  const date = new Date(isoTime);
  const h = date.getHours().toString().padStart(2, "0");
  const m = date.getMinutes().toString().padStart(2, "0");
  return `${h}:${m}`;
}

function toIsoDateTime(dateStr: string, timeStr: string): string {
  return `${dateStr}T${timeStr}:00`;
}

function formatBreakTime(isoTime: string): string {
  const date = new Date(isoTime);
  const h = date.getHours().toString().padStart(2, "0");
  const m = date.getMinutes().toString().padStart(2, "0");
  return `${h}:${m}`;
}

export default function EditAttendanceModal({
  record,
  onClose,
  onSaved,
}: EditAttendanceModalProps) {
  const [clockIn, setClockIn] = useState(toTimeInput(record.clockInTime));
  const [clockOut, setClockOut] = useState(toTimeInput(record.clockOutTime));
  const [breaks, setBreaks] = useState<BreakResponse[]>(record.breaks);
  const [newBreakStart, setNewBreakStart] = useState("");
  const [newBreakEnd, setNewBreakEnd] = useState("");
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);

  const handleSave = async () => {
    setError("");
    setIsSaving(true);
    try {
      await updateAttendance(record.id, {
        clockInTime: toIsoDateTime(record.workDate, clockIn),
        clockOutTime: clockOut
          ? toIsoDateTime(record.workDate, clockOut)
          : null,
        version: record.version,
      });
      onSaved();
    } catch (e: unknown) {
      const apiError = e as { message?: string };
      setError(apiError?.message || "保存に失敗しました。");
    } finally {
      setIsSaving(false);
    }
  };

  const handleAddBreak = async () => {
    if (!newBreakStart || !newBreakEnd) return;
    setError("");
    try {
      const request: CreateBreakRequest = {
        startTime: toIsoDateTime(record.workDate, newBreakStart),
        endTime: toIsoDateTime(record.workDate, newBreakEnd),
      };
      const created = await addBreak(record.id, request);
      setBreaks([...breaks, created]);
      setNewBreakStart("");
      setNewBreakEnd("");
    } catch (e: unknown) {
      const apiError = e as { message?: string };
      setError(apiError?.message || "休憩の追加に失敗しました。");
    }
  };

  const handleDeleteBreak = async (breakId: number) => {
    setError("");
    try {
      await deleteBreak(record.id, breakId);
      setBreaks(breaks.filter((b) => b.id !== breakId));
    } catch (e: unknown) {
      const apiError = e as { message?: string };
      setError(apiError?.message || "休憩の削除に失敗しました。");
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4 shadow-xl">
        <h3 className="text-lg font-bold text-gray-900 mb-4">
          打刻修正 - {record.workDate}
        </h3>

        {error && (
          <div className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              出勤時刻
            </label>
            <input
              type="time"
              value={clockIn}
              onChange={(e) => setClockIn(e.target.value)}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              退勤時刻
            </label>
            <input
              type="time"
              value={clockOut}
              onChange={(e) => setClockOut(e.target.value)}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            />
          </div>

          <div>
            <h4 className="text-sm font-medium text-gray-700 mb-2">休憩記録</h4>
            {breaks.length > 0 && (
              <div className="mb-2 space-y-1">
                {breaks.map((b) => (
                  <div
                    key={b.id}
                    className="flex items-center justify-between bg-gray-50 rounded px-3 py-1 text-sm"
                  >
                    <span>
                      {formatBreakTime(b.startTime)} - {formatBreakTime(b.endTime)}{" "}
                      ({b.durationMinutes}分)
                    </span>
                    <button
                      onClick={() => handleDeleteBreak(b.id)}
                      className="text-red-600 hover:text-red-800 text-xs"
                    >
                      削除
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div className="flex gap-2 items-end">
              <div className="flex-1">
                <input
                  type="time"
                  value={newBreakStart}
                  onChange={(e) => setNewBreakStart(e.target.value)}
                  placeholder="開始"
                  className="w-full rounded border border-gray-300 px-2 py-1 text-sm"
                />
              </div>
              <span className="text-gray-500 text-sm">〜</span>
              <div className="flex-1">
                <input
                  type="time"
                  value={newBreakEnd}
                  onChange={(e) => setNewBreakEnd(e.target.value)}
                  placeholder="終了"
                  className="w-full rounded border border-gray-300 px-2 py-1 text-sm"
                />
              </div>
              <button
                onClick={handleAddBreak}
                disabled={!newBreakStart || !newBreakEnd}
                className="rounded bg-green-600 px-3 py-1 text-sm text-white hover:bg-green-700 disabled:opacity-50"
              >
                追加
              </button>
            </div>
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
          >
            キャンセル
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving || !clockIn}
            className="rounded bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isSaving ? "保存中..." : "保存"}
          </button>
        </div>
      </div>
    </div>
  );
}
