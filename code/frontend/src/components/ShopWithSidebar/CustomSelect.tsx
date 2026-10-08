"use client";

import { useState } from "react";

type Option = { label: string; value: string };
type Props = { options: Option[]; value?: string; onChange?: (value: string) => void };

export default function CustomSelect({ options, value, onChange }: Props) {
  const [selectedValue, setSelectedValue] = useState(options[0]?.value ?? "");
  return (
    <select aria-label="Sort products" value={value ?? selectedValue}
      onChange={(event) => {
        setSelectedValue(event.target.value);
        onChange?.(event.target.value);
      }}
      className="h-10 max-w-full rounded-md border border-gray-3 bg-white px-3 text-sm text-dark focus:outline-none focus:ring-2 focus:ring-blue/20">
      {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
    </select>
  );
}
