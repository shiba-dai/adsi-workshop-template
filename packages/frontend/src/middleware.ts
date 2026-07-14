import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export async function middleware(request: NextRequest): Promise<NextResponse> {
  const loginPath = "/login";
  const isLoginPage = request.nextUrl.pathname === loginPath;

  try {
    const meUrl = new URL("/api/auth/me", request.url);
    const response = await fetch(meUrl.toString(), {
      headers: {
        cookie: request.headers.get("cookie") ?? "",
      },
    });

    if (response.status === 401 && !isLoginPage) {
      const loginUrl = new URL(loginPath, request.url);
      return NextResponse.redirect(loginUrl);
    }

    if (response.ok && isLoginPage) {
      const dashboardUrl = new URL("/", request.url);
      return NextResponse.redirect(dashboardUrl);
    }
  } catch {
    if (!isLoginPage) {
      const loginUrl = new URL(loginPath, request.url);
      return NextResponse.redirect(loginUrl);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api/).*)"],
};
