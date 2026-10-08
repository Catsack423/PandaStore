"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Bell, BellOff, LoaderCircle } from "lucide-react";
import { useAuth } from "@/app/context/AuthContext";
import { Button, buttonVariants } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";

type Notification = {
  notificationId: number;
  title: string;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
};

type NotificationResult = {
  success: boolean;
  message?: string;
  data?: Notification[];
};

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ""
    : new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(date);
}

export default function NotificationPopover() {
  const { user, isLoading: authLoading } = useAuth();
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [markingAll, setMarkingAll] = useState(false);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      setNotifications([]);
      setError("");
      setLoading(false);
      return;
    }

    let active = true;
    async function loadNotifications() {
      setLoading(true);
      try {
        const response = await fetch("/api/notifications", { cache: "no-store" });
        const body: NotificationResult = await response.json();
        if (!response.ok || !body.success || !Array.isArray(body.data)) {
          throw new Error(body.message || "Could not load notifications");
        }
        if (active) {
          setNotifications(body.data);
          setError("");
        }
      } catch (cause) {
        if (active) {
          setError(cause instanceof Error ? cause.message : "Could not load notifications");
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadNotifications();
    const interval = window.setInterval(() => void loadNotifications(), 60_000);
    const onFocus = () => void loadNotifications();
    window.addEventListener("focus", onFocus);
    return () => {
      active = false;
      window.clearInterval(interval);
      window.removeEventListener("focus", onFocus);
    };
  }, [user?.id, authLoading, refreshKey]);

  const unread = notifications.filter((item) => !item.isRead);

  async function markAsRead(notificationId: number) {
    if (pendingId !== null || markingAll) return;
    setPendingId(notificationId);
    setError("");
    try {
      const response = await fetch(`/api/notifications/${notificationId}/read`, {
        method: "PATCH",
      });
      const body: NotificationResult = await response.json();
      if (!response.ok || !body.success) throw new Error(body.message || "Could not mark as read");
      setNotifications((current) => current.map((item) =>
        item.notificationId === notificationId ? { ...item, isRead: true } : item,
      ));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not mark as read");
    } finally {
      setPendingId(null);
    }
  }

  async function markAllAsRead() {
    if (markingAll || pendingId !== null || unread.length === 0) return;
    setMarkingAll(true);
    setError("");
    const results = await Promise.allSettled(unread.map(async ({ notificationId }) => {
      const response = await fetch(`/api/notifications/${notificationId}/read`, { method: "PATCH" });
      const body: NotificationResult = await response.json();
      if (!response.ok || !body.success) throw new Error(body.message || "Could not mark as read");
      return notificationId;
    }));
    const completed = new Set(results.flatMap((result) => result.status === "fulfilled" ? [result.value] : []));
    setNotifications((current) => current.map((item) =>
      completed.has(item.notificationId) ? { ...item, isRead: true } : item,
    ));
    if (results.some((result) => result.status === "rejected")) {
      setError("Some notifications could not be marked as read. Try again.");
    }
    setMarkingAll(false);
  }

  return (
    <Popover open={open} onOpenChange={(nextOpen) => {
      setOpen(nextOpen);
      if (nextOpen && user) setRefreshKey((current) => current + 1);
    }}>
      <PopoverTrigger
        render={<Button variant="ghost" size="icon-lg" />}
        aria-label={unread.length ? `Notifications, ${unread.length} unread` : "Notifications"}
        className="relative h-10 w-10 rounded-full text-blue hover:bg-blue-light-5 hover:text-blue-dark aria-expanded:bg-blue-light-5 aria-expanded:text-blue"
      >
        <Bell className="h-5 w-5" aria-hidden="true" />
        {unread.length > 0 && (
          <span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full border-2 border-white bg-blue text-[10px] font-semibold leading-none text-white">
            {unread.length > 9 ? "9+" : unread.length}
          </span>
        )}
      </PopoverTrigger>

      <PopoverContent
        align="end"
        className="w-80 max-w-[calc(100vw-1.5rem)] overflow-hidden rounded-xl border-gray-3 p-0 shadow-[0_18px_50px_rgba(28,39,76,0.16)] sm:w-[360px]"
      >
        <div className="flex items-center justify-between gap-3 border-b border-gray-3 px-4 py-3.5">
          <div className="flex min-w-0 items-center gap-2">
            <PopoverTitle className="font-semibold text-dark">Notifications</PopoverTitle>
            {unread.length > 0 && (
              <span className="rounded-full bg-blue-light-5 px-2 py-0.5 text-xs font-semibold text-blue">
                {unread.length} new
              </span>
            )}
          </div>
          {user && unread.length > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="shrink-0 text-xs text-blue hover:bg-blue-light-5 hover:text-blue-dark"
              disabled={markingAll || pendingId !== null}
              onClick={() => void markAllAsRead()}
            >
              {markingAll ? "Marking…" : "Mark all as read"}
            </Button>
          )}
        </div>

        {error && (
          <div role="alert" className="flex items-start justify-between gap-3 border-b border-red-light-4 bg-red-light-6 px-4 py-2.5 text-xs text-red-dark">
            <span>{error}</span>
            <Button variant="link" size="xs" className="h-auto p-0 text-red-dark" onClick={() => setRefreshKey((current) => current + 1)}>
              Retry
            </Button>
          </div>
        )}

        {authLoading || (loading && notifications.length === 0) ? (
          <div className="flex items-center justify-center gap-2 px-4 py-10 text-sm text-dark-4">
            <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" /> Loading notifications…
          </div>
        ) : !user ? (
          <div className="px-5 py-9 text-center">
            <p className="font-medium text-dark">Sign in to see notifications</p>
            <p className="mt-1 text-sm text-dark-4">Updates about your orders and account appear here.</p>
            <Link href="/signin" className={buttonVariants({ variant: "ghost", className: "mt-4 bg-blue px-4 text-white hover:bg-blue-dark hover:text-white" })} onClick={() => setOpen(false)}>
              Sign in
            </Link>
          </div>
        ) : error && notifications.length === 0 ? (
          <p className="px-5 py-8 text-center text-sm text-dark-4">Notifications could not be loaded. Use Retry to try again.</p>
        ) : notifications.length === 0 ? (
          <div className="flex flex-col items-center px-5 py-10 text-center">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-light-5 text-blue"><BellOff className="h-5 w-5" aria-hidden="true" /></span>
            <p className="mt-3 font-medium text-dark">No notifications yet</p>
            <p className="mt-1 text-sm text-dark-4">Order and account updates will appear here.</p>
          </div>
        ) : (
          <div className="max-h-[min(24rem,calc(100dvh-12rem))] divide-y divide-gray-3 overflow-y-auto">
            {notifications.map((item) => (
              <Button
                key={item.notificationId}
                variant="ghost"
                className="h-auto w-full items-start justify-start gap-3 rounded-none px-4 py-3 text-left whitespace-normal hover:bg-blue-light-5 disabled:opacity-100"
                disabled={item.isRead || pendingId !== null || markingAll}
                aria-label={item.isRead ? `${item.title}, read` : `Mark ${item.title} as read`}
                onClick={() => void markAsRead(item.notificationId)}
              >
                <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${item.isRead ? "bg-gray-4" : "bg-blue"}`} aria-hidden="true" />
                <span className="min-w-0 flex-1">
                  <span className="flex items-start justify-between gap-3">
                    <span className={`min-w-0 break-words text-sm font-semibold ${item.isRead ? "text-dark-4" : "text-dark"}`}>{item.title}</span>
                    <span className="shrink-0 pt-0.5 text-xs font-normal text-dark-4">{formatDate(item.createdAt)}</span>
                  </span>
                  <span className="mt-1 block break-words text-xs font-normal leading-5 text-dark-4">{item.message}</span>
                </span>
              </Button>
            ))}
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
}
