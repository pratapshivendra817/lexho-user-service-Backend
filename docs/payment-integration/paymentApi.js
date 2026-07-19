import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8082",
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function createPaymentOrder(bookingId, idempotencyKey) {
  return api.post(
    "/api/payments/create-order",
    { bookingId },
    { headers: { "Idempotency-Key": idempotencyKey } },
  );
}

export function verifyPayment(payload) {
  return api.post("/api/payments/verify", payload);
}

export function getPayment(bookingId) {
  return api.get(`/api/payments/${bookingId}`);
}
