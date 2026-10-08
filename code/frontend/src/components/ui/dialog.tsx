"use client"

import * as React from "react"
import { Dialog as DialogPrimitive } from "@base-ui/react/dialog"
import { XIcon } from "lucide-react"
import { cn } from "@/lib/utils"

const Dialog = DialogPrimitive.Root
const DialogTrigger = DialogPrimitive.Trigger
const DialogClose = DialogPrimitive.Close
const DialogTitle = DialogPrimitive.Title
const DialogDescription = DialogPrimitive.Description

function DialogContent({ className, children, showCloseButton = true, ...props }: React.ComponentProps<typeof DialogPrimitive.Popup> & { showCloseButton?: boolean }) {
  return <DialogPrimitive.Portal>
    <DialogPrimitive.Backdrop className="fixed inset-0 z-[10000] bg-dark/60" />
    <DialogPrimitive.Viewport className="fixed inset-0 z-[10001] flex items-center justify-center overflow-y-auto p-4 sm:p-6">
      <DialogPrimitive.Popup className={cn("relative my-auto max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-xl border border-gray-3 bg-white p-6 text-dark shadow-xl outline-none sm:max-h-[calc(100dvh-3rem)]", className)} {...props}>
        {children}
        {showCloseButton && <DialogPrimitive.Close aria-label="Close dialog" className="absolute right-4 top-4 flex size-8 items-center justify-center rounded-lg text-dark-4 hover:bg-gray-1 hover:text-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue">
          <XIcon className="size-4" />
        </DialogPrimitive.Close>}
      </DialogPrimitive.Popup>
    </DialogPrimitive.Viewport>
  </DialogPrimitive.Portal>
}

function DialogHeader({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex flex-col gap-2 pr-8", className)} {...props} />
}

export { Dialog, DialogTrigger, DialogClose, DialogContent, DialogHeader, DialogTitle, DialogDescription }
