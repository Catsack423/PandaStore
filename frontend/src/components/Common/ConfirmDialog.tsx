"use client";

import React, { useState, useEffect } from "react";
import { AlertTriangle, CheckCircle, Info, Loader2, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";

export interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (inputValue?: string) => Promise<void> | void;
  title: string;
  description: string;
  confirmText?: string;
  cancelText?: string;
  variant?: "danger" | "primary" | "warning";
  icon?: "alert" | "info" | "check" | "trash";
  showInput?: boolean;
  inputLabel?: string;
  inputPlaceholder?: string;
  inputDefaultValue?: string;
  inputRequired?: boolean;
  loading?: boolean;
}

export default function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmText = "Confirm",
  cancelText = "Cancel",
  variant = "primary",
  icon = "info",
  showInput = false,
  inputLabel,
  inputPlaceholder,
  inputDefaultValue = "",
  inputRequired = false,
  loading: externalLoading = false,
}: ConfirmDialogProps) {
  const [internalLoading, setInternalLoading] = useState(false);
  const [inputValue, setInputValue] = useState(inputDefaultValue);
  const [inputError, setInputError] = useState("");

  useEffect(() => {
    if (isOpen) {
      setInputValue(inputDefaultValue);
      setInputError("");
      setInternalLoading(false);
    }
  }, [isOpen, inputDefaultValue]);

  if (!isOpen) return null;

  const isLoading = externalLoading || internalLoading;

  const handleConfirm = async () => {
    if (showInput && inputRequired && !inputValue.trim()) {
      setInputError("This field is required.");
      return;
    }

    try {
      setInternalLoading(true);
      await onConfirm(inputValue);
      onClose();
    } catch {
      // Keep modal open on error
    } finally {
      setInternalLoading(false);
    }
  };

  const getIcon = () => {
    switch (icon) {
      case "trash":
      case "alert":
        return <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-red-light-6 text-red"><AlertTriangle className="size-6" /></div>;
      case "check":
        return <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-green-light-6 text-green"><CheckCircle className="size-6" /></div>;
      case "info":
      default:
        return <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-blue/10 text-blue"><Info className="size-6" /></div>;
    }
  };

  const getConfirmButtonClasses = () => {
    switch (variant) {
      case "danger":
        return "bg-red text-white hover:bg-red-dark";
      case "warning":
        return "bg-yellow-dark text-white hover:bg-opacity-90";
      case "primary":
      default:
        return "bg-blue text-white hover:bg-blue-dark";
    }
  };

  return (
    <div className="fixed inset-0 z-[99999] flex items-center justify-center bg-black/60 p-4 animate-fade-in backdrop-blur-sm">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        className="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl transition-all sm:p-7"
      >
        <button
          type="button"
          onClick={onClose}
          disabled={isLoading}
          aria-label="Close dialog"
          className="absolute right-4 top-4 rounded-lg p-1.5 text-dark-4 hover:bg-gray-1 hover:text-dark transition-colors"
        >
          <X className="size-5" />
        </button>

        <div className="flex items-start gap-4">
          {getIcon()}
          <div className="min-w-0 flex-1">
            <h3 id="confirm-dialog-title" className="text-lg font-semibold text-dark">
              {title}
            </h3>
            <p className="mt-1 text-sm leading-relaxed text-dark-4">
              {description}
            </p>
          </div>
        </div>

        {showInput && (
          <div className="mt-4 space-y-1.5">
            {inputLabel && (
              <label htmlFor="confirm-dialog-input" className="block text-xs font-medium text-dark">
                {inputLabel} {inputRequired && <span className="text-red">*</span>}
              </label>
            )}
            <textarea
              id="confirm-dialog-input"
              rows={3}
              value={inputValue}
              onChange={(e) => {
                setInputValue(e.target.value);
                if (inputError) setInputError("");
              }}
              placeholder={inputPlaceholder}
              disabled={isLoading}
              className="w-full rounded-lg border border-gray-3 p-3 text-sm focus:border-blue focus:outline-none focus:ring-1 focus:ring-blue"
            />
            {inputError && <p className="text-xs text-red">{inputError}</p>}
          </div>
        )}

        <div className="mt-6 flex flex-col-reverse gap-2.5 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isLoading}
            className="border-gray-3 text-dark hover:bg-gray-1"
          >
            {cancelText}
          </Button>
          <Button
            type="button"
            onClick={handleConfirm}
            disabled={isLoading}
            className={`${getConfirmButtonClasses()} min-w-24`}
          >
            {isLoading ? (
              <span className="flex items-center gap-2">
                <Loader2 className="size-4 animate-spin" />
                Processing...
              </span>
            ) : (
              confirmText
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
