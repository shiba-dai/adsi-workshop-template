"use client";

interface ClockInButtonProps {
  disabled: boolean;
  onClick: () => void;
  isLoading: boolean;
}

export default function ClockInButton({
  disabled,
  onClick,
  isLoading,
}: ClockInButtonProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled || isLoading}
      className="rounded-lg bg-blue-600 px-8 py-4 text-lg font-semibold text-white shadow-md hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300 disabled:text-gray-500 transition-colors"
    >
      {isLoading ? "処理中..." : "出勤"}
    </button>
  );
}
