import { cn } from "@/lib/utils";

export interface BadgeProps extends ReactProps {
  variant?: "default" | "destructive" | "outline" | "secondary" | "ghost";
}

export const Badge = React.forwardRef<HTMLSpanElement, BadgeProps>(({ className, variant = "default", ...props }) => {
  const variants = {
    default: "bg-primary/10 text-primary",
    destructive: "bg-destructive/10 text-destructive-foreground",
    outline: "border border-input text-foreground",
    secondary: "bg-secondary/10 text-secondary",
    ghost: "hover:bg-accent hover:text-accent-foreground",
  };

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
        variants[variant],
        className,
      )}
      {...props}
    />
  );
});