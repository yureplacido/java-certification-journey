import React from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

interface Activity {
  id: string;
  type: "review" | "study" | "experiment" | "practice" | "quiz";
  title: string;
  status: "completed" | "pending";
}

interface ActivityListProps {
  activities: Activity[];
}

export const ActivityList: React.FC<ActivityListProps> = ({
  activities,
}) => {
  return (
    <div className="space-y-2">
      {activities.map((activity) => {
        const typeIcons: Record<string, string> = {
          review: "📖",
          study: "📚",
          experiment: "🧪",
          practice: "💪",
          quiz: "📝",
        };

        const statusClass =
          activity.status === "completed"
            ? "bg-primary/10 text-primary"
            : "bg-secondary/10 text-secondary";

        return (
          <div key={activity.id} className="flex items-center gap-3">
            <span className="w-8 h-8 rounded bg-gray-200/50 flex items-center justify-center text-sm">
              {typeIcons[activity.type]}
            </span>
            <div className="flex-1">
              <p className="font-medium">{activity.title}</p>
              <p className="text-xs text-muted-foreground">
                {activity.type}
              </p>
            </div>
            <Badge
              variant={activity.status === "completed" ? "default" : "outline"}
            >
              {activity.status}
            </Badge>
          </div>
        );
      })}
    </div>
  );
};