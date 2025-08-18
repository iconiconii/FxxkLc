import { cn } from "@/lib/utils"

interface SpinnerProps {
  className?: string
}

export function Spinner({ className }: SpinnerProps) {
  return (
    <div 
      className={cn(
        "animate-spin rounded-full border-2 border-gray-300 border-t-blue-600",
        className
      )}
    />
  )
}