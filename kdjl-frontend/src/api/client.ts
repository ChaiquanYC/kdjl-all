import axios from 'axios';
import type { ApiResponse } from '@/types';

const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      // Reload to force ProtectedRoute re-check (which will redirect to /login)
      window.location.replace('/login');
    }
    return Promise.reject(error);
  },
);

export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> {
  const { data } = await client.get<ApiResponse<T>>(url, { params });
  return data;
}

export async function apiPost<T>(url: string, body?: unknown): Promise<ApiResponse<T>> {
  const { data } = await client.post<ApiResponse<T>>(url, body);
  return data;
}

export default client;
