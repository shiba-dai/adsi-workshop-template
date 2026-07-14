"use client";

import { useEffect, useState, useCallback } from "react";
import { getFullPath } from "@/lib/navigation";
import Header from "@/components/Header";
import ClockInButton from "@/components/ClockInButton";
import ClockOutButton from "@/components/ClockOutButton";
import {
  getMe,
  getToday,
  clockIn,
  clockOut,
} from "@/lib/api-client";
import { navigateTo } from "@/lib/navigation";
import type { EmployeeResponse, AttendanceResponse } from "@/lib/api-client";

type AttendanceStatus = "not_clocked_in" | "clocked_in" | "clocked_out";

function getStatus(attendance: AttendanceResponse | null): AttendanceStatus {
  if (!attendance) return "not_clocked_in";
  if (!attendance.clockOutTime) return "clocked_in";
  return "clocked_out";
}

function getStatusLabel(status: AttendanceStatus): string {
  switch (status) {
    case "not_clocked_in":
      return "未出勤";
    case "clocked_in":
      return "出勤済";
    case "clocked_out":
      return "退勤済";
  }
}

function getStatusColor(status: AttendanceStatus): string {
  switch (status) {
    case "not_clocked_in":
      return "text-gray-500";
    case "clocked_in":
      return "text-blue-600";
    case "clocked_out":
      return "text-green-600";
  }
}

export default function DashboardPage() {
  const [employee, setEmployee] = useState<EmployeeResponse | null>(null);
  const [attendance, setAttendance] = useState<AttendanceResponse | null>(null);
  const [currentTime, setCurrentTime] = useState<string>("");
  const [isClockInLoading, setIsClockInLoading] = useState(false);
  const [isClockOutLoading, setIsClockOutLoading] = useState(false);
  const [error, setError] = useState("");

  const fetchData = useCallback(async () => {
    try {
      const [me, today] = await Promise.all([getMe(), getToday()]);
      setEmployee(me);
      setAttendance(today);
    } catch (e: unknown) {
      const apiError = e as { status?: number };
      if (apiError?.status === 401) {
        navigateTo("/login");
        return;
      }
      setError("データの取得に失敗しました。");
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      const hours = now.getHours().toString().padStart(2, "0");
      const minutes = now.getMinutes().toString().padStart(2, "0");
      const seconds = now.getSeconds().toString().padStart(2, "0");
      setCurrentTime(`${hours}:${minutes}:${seconds}`);
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  const handleClockIn = async () => {
    setIsClockInLoading(true);
    setError("");
    try {
      const result = await clockIn();
      setAttendance(result);
    } catch {
      setError("出勤打刻に失敗しました。");
    } finally {
      setIsClockInLoading(false);
    }
  };

  const handleClockOut = async () => {
    setIsClockOutLoading(true);
    setError("");
    try {
      const result = await clockOut();
      setAttendance(result);
    } catch {
      setError("退勤打刻に失敗しました。");
    } finally {
      setIsClockOutLoading(false);
    }
  };

  const status = getStatus(attendance);

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
        {error && (
          <div className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="rounded-lg bg-white p-8 shadow-md text-center">
          <p className="text-5xl font-mono font-bold text-gray-900 mb-4">
            {currentTime}
          </p>

          <p className={`text-xl font-semibold mb-8 ${getStatusColor(status)}`}>
            {getStatusLabel(status)}
          </p>

          <div className="flex justify-center gap-6">
            <ClockInButton
              disabled={status !== "not_clocked_in"}
              onClick={handleClockIn}
              isLoading={isClockInLoading}
            />
            <ClockOutButton
              disabled={status !== "clocked_in"}
              onClick={handleClockOut}
              isLoading={isClockOutLoading}
            />
          </div>
        </div>

        <div className="mt-6 text-center">
          <a
            href={getFullPath("/history")}
            className="text-blue-600 hover:text-blue-800 hover:underline"
          >
            勤怠履歴を見る
          </a>
        </div>
      </main>
    </div>
  );
}
