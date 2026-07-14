// Types
export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  name: string;
  email: string;
  role: string;
}

export interface AttendanceResponse {
  id: number;
  workDate: string;
  clockInTime: string | null;
  clockOutTime: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ApiError {
  status: number;
  message: string;
}

function getApiBase(): string {
  if (typeof window === "undefined") {
    return process.env.NEXT_PUBLIC_ASSET_PREFIX ?? "";
  }
  // ブラウザ上では現在のURLからbasePathを動的に検出する
  // SageMaker: /codeeditor/default/absports/3001/login → prefix = /codeeditor/default/absports/3001
  const match = window.location.pathname.match(/^(\/.*\/absports\/\d+)/);
  if (match) {
    return match[1];
  }
  return "";
}

function withBasePath(path: string): string {
  return `${getApiBase()}${path}`;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error: ApiError = {
      status: response.status,
      message: response.statusText,
    };
    throw error;
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

// Auth API
export async function login(request: LoginRequest): Promise<EmployeeResponse> {
  const response = await fetch(withBasePath("/api/auth/login"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(request),
  });
  return handleResponse<EmployeeResponse>(response);
}

export async function logout(): Promise<void> {
  const response = await fetch(withBasePath("/api/auth/logout"), {
    method: "POST",
    credentials: "include",
  });
  if (!response.ok) {
    throw { status: response.status, message: response.statusText } as ApiError;
  }
}

export async function getMe(): Promise<EmployeeResponse> {
  const response = await fetch(withBasePath("/api/auth/me"), {
    credentials: "include",
  });
  return handleResponse<EmployeeResponse>(response);
}

// Attendance API
export async function clockIn(): Promise<AttendanceResponse> {
  const response = await fetch(withBasePath("/api/attendance/clock-in"), {
    method: "POST",
    credentials: "include",
  });
  return handleResponse<AttendanceResponse>(response);
}

export async function clockOut(): Promise<AttendanceResponse> {
  const response = await fetch(withBasePath("/api/attendance/clock-out"), {
    method: "POST",
    credentials: "include",
  });
  return handleResponse<AttendanceResponse>(response);
}

export async function getToday(): Promise<AttendanceResponse | null> {
  const response = await fetch(withBasePath("/api/attendance/today"), {
    credentials: "include",
  });
  if (response.status === 204) {
    return null;
  }
  return handleResponse<AttendanceResponse>(response);
}

export async function getHistory(
  year: number,
  month: number
): Promise<AttendanceResponse[]> {
  const response = await fetch(
    withBasePath(`/api/attendance/history?year=${year}&month=${month}`),
    { credentials: "include" }
  );
  return handleResponse<AttendanceResponse[]>(response);
}
