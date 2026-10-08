"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { cleanupPendingProductImages } from "@/lib/pendingProductImages";

export interface User {
  id: string;
  name: string;
  username?: string;
  email: string;
  phone?: string;
  role: "ADMIN" | "SELLER" | "CUSTOMER";
  status: string;
}

type AuthResult = { success: boolean; user?: User; error?: string };
type Registration = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  fullName: string;
  phoneNumber: string;
};

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<AuthResult>;
  registerCustomer: (details: Registration) => Promise<AuthResult>;
  updateProfile: (details: { name?: string; email?: string; phone?: string }) => Promise<AuthResult>;
  changePassword: (currentPassword: string, password: string, confirmPassword: string) => Promise<AuthResult>;
  refreshUser: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

async function post(path: string, body: object) {
  const response = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await response.json().catch(() => null);
  return { response, data };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const pathname = usePathname();

  useEffect(() => {
    if (!user?.id) return;
    const cleanup = () => { void cleanupPendingProductImages(user.id); };
    cleanup();
    const timer = window.setInterval(cleanup, 60000);
    window.addEventListener("online", cleanup);
    return () => { window.clearInterval(timer); window.removeEventListener("online", cleanup); };
  }, [user?.id, pathname]);

  async function refreshUser() {
    try {
      const response = await fetch("/api/auth/me", { cache: "no-store" });
      if (response.ok) setUser((await response.json()).user);
      else if (response.status === 401) setUser(null);
    } catch {
      /* Keep the current user during a temporary network failure. */
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void refreshUser();
  }, []);

  async function login(
    usernameOrEmail: string,
    password: string,
  ): Promise<AuthResult> {
    try {
      const { response, data } = await post("/api/auth/login", {
        usernameOrEmail,
        password,
      });
      if (!response.ok || !data?.success)
        return { success: false, error: data?.message || "Sign in failed" };
      setUser(data.user);
      await refreshUser();
      return { success: true, user: data.user };
    } catch {
      return { success: false, error: "Could not reach the server" };
    }
  }

  async function registerCustomer(details: Registration): Promise<AuthResult> {
    try {
      const { response, data } = await post("/api/auth/register", details);
      if (!response.ok || !data?.success)
        return {
          success: false,
          error: data?.message || "Registration failed",
        };
      return { success: true };
    } catch {
      return { success: false, error: "Could not reach the server" };
    }
  }

  async function updateProfile(details: { name?: string; email?: string; phone?: string }): Promise<AuthResult> {
    try {
      const response = await fetch("/api/auth/me", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(details),
      });
      const data = await response.json().catch(() => null);
      if (!response.ok || !data?.success) {
        return { success: false, error: data?.message || "Failed to update profile" };
      }
      if (data.user) {
        setUser(data.user);
      }
      return { success: true, user: data.user };
    } catch {
      return { success: false, error: "Could not reach the server" };
    }
  }

  async function changePassword(
    currentPassword: string,
    password: string,
    confirmPassword: string,
  ): Promise<AuthResult> {
    try {
      const response = await fetch("/api/auth/reset-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentPassword, password, confirmPassword }),
      });
      const data = await response.json().catch(() => null);
      if (!response.ok || !data?.success) {
        return { success: false, error: data?.message || "Failed to update password" };
      }
      if (data?.data?.user) {
        const u = data.data.user;
        setUser({
          id: String(u.userId || u.id),
          name: u.fullName || u.username || u.name,
          email: u.email,
          phone: u.phoneNumber || u.phone || "",
          role: u.role,
          status: u.status,
        });
      }
      await refreshUser();
      return { success: true };
    } catch {
      return { success: false, error: "Could not reach the server" };
    }
  }

  async function logout() {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch {
      /* Local session is cleared below. */
    } finally {
      setUser(null);
      window.location.assign("/signin");
    }
  }

  return (
    <AuthContext.Provider
      value={{ user, isLoading, login, registerCustomer, updateProfile, changePassword, refreshUser, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
}
