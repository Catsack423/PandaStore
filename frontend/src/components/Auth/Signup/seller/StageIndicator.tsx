import React from "react";
import { STAGES } from "./types";

interface StageIndicatorProps {
  currentStage: number;
}

export const StageIndicator: React.FC<StageIndicatorProps> = ({
  currentStage,
}) => {
  return (
    <div className="pt-6 pb-2">
      <div className="relative flex items-center justify-between">
        {/* Background Track Line */}
        <div className="absolute left-6 right-6 top-1/2 -translate-y-1/2 h-1 bg-gray-3 z-0" />
        {/* Active Progress Line */}
        <div
          className="absolute left-6 top-1/2 -translate-y-1/2 h-1 bg-blue transition-all duration-300 z-0"
          style={{
            width:
              currentStage === 1
                ? "0%"
                : currentStage === 2
                  ? "50%"
                  : "calc(100% - 48px)",
          }}
        />

        {/* Stage Nodes */}
        {STAGES.map((s) => {
          const isCompleted = currentStage > s.id;
          const isActive = currentStage === s.id;

          return (
            <div
              key={s.id}
              className="relative z-10 flex flex-col items-center"
            >
              <div
                className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-all duration-200 ${
                  isCompleted
                    ? "bg-green text-white shadow-sm"
                    : isActive
                      ? "bg-blue text-white ring-4 ring-blue/20 shadow-md"
                      : "bg-white text-dark-4 border-2 border-gray-3"
                }`}
              >
                {isCompleted ? (
                  <svg
                    className="w-5 h-5"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2.5}
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                ) : (
                  s.id
                )}
              </div>
              <span
                className={`text-xs mt-2 font-medium text-center hidden sm:block ${
                  isActive
                    ? "text-blue font-bold"
                    : isCompleted
                      ? "text-dark"
                      : "text-dark-4"
                }`}
              >
                {s.title}
              </span>
            </div>
          );
        })}
      </div>

      {/* Mobile Stage Subtitle */}
      <p className="text-center text-xs font-semibold text-blue mt-3 sm:hidden">
        ขั้นตอนที่ {currentStage} จาก 3: {STAGES[currentStage - 1].title}
      </p>
    </div>
  );
};
