import React from "react";
import { STAGES } from "./types";

interface StageIndicatorProps {
  currentStage: number;
}

export const StageIndicator: React.FC<StageIndicatorProps> = ({
  currentStage,
}) => {
  return (
    <div className="pt-6 pb-2 w-full">
      <div className="flex items-start w-full">
        {STAGES.map((s, idx) => {
          const isCompleted = currentStage > s.id;
          const isActive = currentStage === s.id;
          const isLast = idx === STAGES.length - 1;

          return (
            <React.Fragment key={s.id}>
              {/* Node */}
              <div className="flex flex-col items-center shrink-0">
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
                
              </div>

              {/* Connector line to next node (skip after last node) */}
              {!isLast && (
                <div className="flex-1 h-1 mx-2 mt-5 rounded-full bg-gray-3 overflow-hidden">
                  <div
                    className="h-full bg-blue transition-all duration-300"
                    style={{
                      width: currentStage > s.id ? "100%" : "0%",
                    }}
                  />
                </div>
              )}
            </React.Fragment>
          );
        })}
      </div>

      {/* Mobile Stage Subtitle */}
      <p className="text-center text-xs font-semibold text-blue mt-3 ">
         {STAGES[currentStage - 1].title}
      </p>
    </div>
  );
};