"use client";
import { useEffect, useState } from "react";
import { ArrowUp } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function ScrollToTop() {
  const [isVisible, setIsVisible] = useState(false);

  // Top: 0 takes us all the way back to the top of the page
  // Behavior: smooth keeps it smooth!
  const scrollToTop = () => {
    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  useEffect(() => {
    let visible = window.scrollY > 300;
    setIsVisible(visible);

    const toggleVisibility = () => {
      const nextVisible = window.scrollY > 300;
      if (nextVisible !== visible) {
        visible = nextVisible;
        setIsVisible(nextVisible);
      }
    };

    window.addEventListener("scroll", toggleVisibility, { passive: true });

    return () => window.removeEventListener("scroll", toggleVisibility);
  }, []);

  return (
    <>
      {isVisible && (
        <Button
          type="button"
          variant="default"
          size="icon"
          onClick={scrollToTop}
          aria-label="Scroll to top"
          className="fixed bottom-7 right-7 z-999 h-9 w-9 bg-blue text-white shadow-lg hover:bg-blue-dark sm:bottom-8 sm:right-8 sm:h-10 sm:w-10"
        >
          <ArrowUp className="size-5" aria-hidden="true" />
        </Button>
      )}
    </>
  );
}
