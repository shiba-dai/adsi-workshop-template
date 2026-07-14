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

export interface BreakResponse {
  id: number;
  startTime: string;
  endTime: string;
  durationMinutes: number;
}

export interface AttendanceDetailResponse {
  id: number;
  workDate: string;
  clockInTime: string | null;
  clockOutTime: string | null;
  workingMinutes: number;
  breakMinutes: number;
  overtimeMinutes: number;
  breaks: BreakResponse[];
  version: number;
}

export interface OvertimeSummaryResponse {
  year: number;
  month: number;
  monthlyOvertimeMinutes: number;
  yearlyOvertimeMinutes: number;
  monthlyLimitWarning: boolean;
  yearlyLimitWarning: boolean;
}

export interface UpdateAttendanceRequest {
  clockInTime: string;
  clockOutTime: string | null;
  version: number;
}

export interface CreateBreakRequest {
  startTime: string;
  endTime: string;
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
): Promise<AttendanceDetailResponse[]> {
  const response = await fetch(
    withBasePath(`/api/attendance/history?year=${year}&month=${month}`),
    { credentials: "include" }
  );
  return handleResponse<AttendanceDetailResponse[]>(response);
}

export async function updateAttendance(
  id: number,
  request: UpdateAttendanceRequest
): Promise<AttendanceDetailResponse> {
  const response = await fetch(withBasePath(`/api/attendance/${id}`), {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(request),
  });
  return handleResponse<AttendanceDetailResponse>(response);
}

export async function getBreaks(attendanceId: number): Promise<BreakResponse[]> {
  const response = await fetch(
    withBasePath(`/api/attendance/${attendanceId}/breaks`),
    { credentials: "include" }
  );
  return handleResponse<BreakResponse[]>(response);
}

export async function addBreak(
  attendanceId: number,
  request: CreateBreakRequest
): Promise<BreakResponse> {
  const response = await fetch(
    withBasePath(`/api/attendance/${attendanceId}/breaks`),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(request),
    }
  );
  return handleResponse<BreakResponse>(response);
}

export async function deleteBreak(
  attendanceId: number,
  breakId: number
): Promise<void> {
  const response = await fetch(
    withBasePath(`/api/attendance/${attendanceId}/breaks/${breakId}`),
    {
      method: "DELETE",
      credentials: "include",
    }
  );
  if (!response.ok) {
    throw { status: response.status, message: response.statusText } as ApiError;
  }
}

export async function getOvertimeSummary(
  year: number,
  month: number
): Promise<OvertimeSummaryResponse> {
  const response = await fetch(
    withBasePath(`/api/attendance/overtime?year=${year}&month=${month}`),
    { credentials: "include" }
  );
  return handleResponse<OvertimeSummaryResponse>(response);
}
