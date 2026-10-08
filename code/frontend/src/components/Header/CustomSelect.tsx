import React, { useState, useEffect } from "react";

type Option = { label: string; value: string };

const CustomSelect = ({ options, value, onChange }: {
  options: Option[]; value?: string; onChange?: (value: string) => void;
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [localValue, setLocalValue] = useState(options[0]?.value ?? "0");
  const selectedValue = value ?? localValue;
  const selectedOption = options.find((option) => option.value === selectedValue) ?? options[0];

  const toggleDropdown = () => {
    setIsOpen(!isOpen);
  };

  const handleOptionClick = (option: Option) => {
    setLocalValue(option.value);
    onChange?.(option.value);
    setIsOpen(false);
  };

  useEffect(() => {
    // closing modal while clicking outside
    function handleClickOutside(event: MouseEvent) {
      if (!(event.target as Element).closest(".dropdown-content")) {
        setIsOpen(false);
      }
    }

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  return (
    <div className="dropdown-content custom-select relative" style={{ width: "200px" }}>
      <div
        className={`select-selected whitespace-nowrap ${
          isOpen ? "select-arrow-active" : ""
        }`}
        onClick={toggleDropdown}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            toggleDropdown();
          }
          if (event.key === "Escape") setIsOpen(false);
        }}
        role="button"
        tabIndex={0}
        aria-expanded={isOpen}
        aria-label="Search category"
      >
        {selectedOption?.label}
      </div>
      <div className={`select-items ${isOpen ? "" : "select-hide"}`}>
        {options.map((option) => (
          <div
            key={option.value}
            onClick={() => handleOptionClick(option)}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                handleOptionClick(option);
              }
            }}
            role="button"
            tabIndex={isOpen ? 0 : -1}
            className={`select-item ${
              selectedValue === option.value ? "same-as-selected" : ""
            }`}
          >
            {option.label}
          </div>
        ))}
      </div>
    </div>
  );
};

export default CustomSelect;
