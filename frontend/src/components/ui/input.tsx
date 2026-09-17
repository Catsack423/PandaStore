import * as React from "react";
import { cn } from "cn";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          "flex h-11 w-full rounded-lg border border-gray-3 bg-gray-1 px-4 py-2.5 text-sm text-dark placeholder:text-dark-5 transition-colors duration-200 outline-none focus:border-transparent focus:shadow-input focus:ring-2 focus:ring-blue/20 disabled:cursor-not-allowed disabled:opacity-50",
          className,
        )}
        ref={ref}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";

export { Input };
