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
  const pathname = window.location.pathname;

  // /codeeditor/.../absports/PORT がフルで含まれる場合
  const fullMatch = pathname.match(/^(\/codeeditor\/[^/]+\/absports\/\d+)/);
  if (fullMatch) {
    return fullMatch[1];
  }

  // /absports/PORT のみの場合、/codeeditor/default を補完
  const shortMatch = pathname.match(/^(\/absports\/\d+)/);
  if (shortMatch) {
    return `/codeeditor/default${shortMatch[1]}`;
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
