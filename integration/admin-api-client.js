const ADMIN_AUTH_ENDPOINTS = Object.freeze({
  login: "/api/admin/login",
  verifyOtp: "/api/admin/verify-otp",
});

const ADMIN_FILE_ENDPOINTS = Object.freeze({
  uploadImage: "/api/admin/upload/image",
});

function normalizeBaseUrl(baseUrl) {
  return String(baseUrl || "").replace(/\/+$/, "");
}

export function createAdminApiClient({
  baseUrl,
  getToken = () => localStorage.getItem("adminAccessToken"),
  setToken = (token) => localStorage.setItem("adminAccessToken", token),
  clearToken = () => localStorage.removeItem("adminAccessToken"),
} = {}) {
  if (!baseUrl) {
    throw new Error("baseUrl is required");
  }
  const normalizedBaseUrl = normalizeBaseUrl(baseUrl);
  if (/\/(?:api\/)?auth$/i.test(normalizedBaseUrl)) {
    throw new Error("Admin API baseUrl must point to backend host, not /auth or /api/auth");
  }

  async function request(path, { method = "GET", body, headers = {}, auth = true } = {}) {
    if (path.startsWith("/auth/") || path.startsWith("/api/auth/")) {
      throw new Error("Admin frontend must use /api/admin/* endpoints, not /auth/*");
    }
    const token = getToken();
    const finalHeaders = {
      "Content-Type": "application/json",
      ...headers,
    };

    if (auth) {
      if (!token) {
        throw new Error("Missing admin access token");
      }
      finalHeaders.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`${normalizedBaseUrl}${path}`, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });

    const isJson = response.headers.get("content-type")?.includes("application/json");
    const payload = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `Request failed (${response.status})`;
      const error = new Error(message);
      error.status = response.status;
      error.payload = payload;
      throw error;
    }

    return payload;
  }

  async function requestFormData(path, formData, { method = "POST", headers = {}, auth = true } = {}) {
    if (path.startsWith("/auth/") || path.startsWith("/api/auth/")) {
      throw new Error("Admin frontend must use /api/admin/* endpoints, not /auth/*");
    }
    const token = getToken();
    const finalHeaders = { ...headers };

    if (auth) {
      if (!token) {
        throw new Error("Missing admin access token");
      }
      finalHeaders.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`${normalizedBaseUrl}${path}`, {
      method,
      headers: finalHeaders,
      body: formData,
    });

    const isJson = response.headers.get("content-type")?.includes("application/json");
    const payload = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `Request failed (${response.status})`;
      const error = new Error(message);
      error.status = response.status;
      error.payload = payload;
      throw error;
    }

    return payload;
  }

  return {
    login: (email, password) =>
      request(ADMIN_AUTH_ENDPOINTS.login, {
        method: "POST",
        auth: false,
        body: { email, password },
      }),

    verifyOtp: async (email, otp) => {
      const result = await request(ADMIN_AUTH_ENDPOINTS.verifyOtp, {
        method: "POST",
        auth: false,
        body: { email, otp },
      });

      if (result?.accessToken) {
        setToken(result.accessToken);
      }
      return result;
    },

    logout: () => clearToken(),

    getDashboardStats: () => request("/api/admin/dashboard/stats"),
    getDashboardRevenue: () => request("/api/admin/dashboard/revenue"),
    getDashboardServiceDistribution: () => request("/api/admin/dashboard/services-distribution"),

    getUsers: ({ page = 1, limit = 10, search, status } = {}) => {
      const params = new URLSearchParams({ page: String(page), limit: String(limit) });
      if (search) params.set("search", search);
      if (status) params.set("status", status);
      return request(`/api/admin/users?${params.toString()}`);
    },

    getProviders: ({ page = 1, limit = 10, search, status } = {}) => {
      const params = new URLSearchParams({ page: String(page), limit: String(limit) });
      if (search) params.set("search", search);
      if (status) params.set("status", status);
      return request(`/api/admin/providers?${params.toString()}`);
    },

    updateProviderVerification: (id, verificationStatus) =>
      request(`/api/admin/providers/${id}/verification`, {
        method: "PATCH",
        body: { status: verificationStatus },
      }),

    getBookings: ({ page = 1, limit = 10, search, status } = {}) => {
      const params = new URLSearchParams({ page: String(page), limit: String(limit) });
      if (search) params.set("search", search);
      if (status) params.set("status", status);
      return request(`/api/admin/bookings?${params.toString()}`);
    },

    getNotifications: ({ page = 1, limit = 10, search, status } = {}) => {
      const params = new URLSearchParams({ page: String(page), limit: String(limit) });
      if (search) params.set("search", search);
      if (status) params.set("status", status);
      return request(`/api/admin/notifications?${params.toString()}`);
    },

    createNotification: (payload) =>
      request("/api/admin/notifications", { method: "POST", body: payload }),

    uploadImage: (file) => {
      if (!file) {
        throw new Error("file is required");
      }
      const formData = new FormData();
      formData.append("file", file);
      return requestFormData(ADMIN_FILE_ENDPOINTS.uploadImage, formData, {
        method: "POST",
      });
    },
  };
}
