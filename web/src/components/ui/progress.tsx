import { cn } from "@/lib/utils";

export const Progress = React.forwardRef<HTMLDivElement, React.ProgressElement["props"] & { variant?: "default" | "primary" | "secondary" }>(({ className, variant = "default", value, max = 100, ...props }) => {
  const percentage = (value / max) * 100;
  const barColor = variant === "primary" ? "bg-primary" : variant === "secondary" ? "bg-secondary" : "bg-muted";

  return (
    <div className={cn("h-2 rounded-full background-background overflow-hidden")}>
      <div
        className={cn(
          "h-full rounded-full bg-gradient-to-r from-primary to-primary/60",
          variant === "secondary" && "bg-secondary/20",
          variant === "default" && "bg-muted/20",
          "transition-all duration-500 ease-in-out",
          `data-[percentage="${percentage}"]::before`,
        )}
        style={{ width: `${percentage}%` }}
      >
        <span
          className="absolute right-2 top-1/2 -translate-y-1/2 text-xs font-medium transform"
          style={variant === "primary" ? { color: "currentColor" } : { color: "muted-foreground" }}
        >
          {Math.round(percentage)}%
        </span>
      </div>
    </div>
  );
});