"use client";

interface ClockOutButtonProps {
  disabled: boolean;
  onClick: () => void;
  isLoading: boolean;
}

export default function ClockOutButton({
  disabled,
  onClick,
  isLoading,
}: ClockOutButtonProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled || isLoading}
      className="rounded-lg bg-orange-600 px-8 py-4 text-lg font-semibold text-white shadow-md hover:bg-orange-700 disabled:cursor-not-allowed disabled:bg-gray-300 disabled:text-gray-500 transition-colors"
    >
      {isLoading ? "処理中..." : "退勤"}
    </button>
  );
}
