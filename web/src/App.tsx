import React from "react";
import { useStudyData } from "@/hooks/useStudyData";
import { TodayDashboard } from "@/components/study/TodayDashboard";

export const App: React.FC = () => {
  const {
    today,
    progress,
    plan,
    loading,
    error,
  } = useStudyData();

  if (error && !today) {
    return <div>Error loading data</div>;
  }

  if (loading && !today) {
    return <div>Loading...</div>;
  }

  return <TodayDashboard
    currentWeek={today?.currentWeek ?? 1}
    currentDay={today?.currentDay ?? 1}
    dayTitle={today?.dayTitle ?? "StringBuilder"}
    activities={today?.activities ?? []}
    progress={{
      score: today?.progress?.score ?? "0/50",
      scorePercent: today?.progress?.scorePercent ?? 0,
      passing: today?.progress?.passing ?? false,
      totalQuestions: today?.progress?.totalQuestions ?? 50,
    }}
    examContext={{
      id: today?.examContext?.id ?? "diagnostic",
      status: today?.examContext?.status ?? "in_progress",
      grade: today?.examContext?.grade ?? 0,
      passing: today?.examContext?.passing ?? false,
    }}
    loadContent={async (path) => {
      // Remove leading slash if present
      const cleanPath = path.startsWith("/") ? path.slice(1) : path;
      const result = await fetch(
        `/api/content/${cleanPath}`
      ).then((res) => res.json());
      return { markdown: result.markdown, title: result.title };
    }}
    loadTrap={async (path) => {
      const cleanPath = path.startsWith("/") ? path.slice(1) : path;
      const res = await fetch(`/api/content/${cleanPath}`);
      const data = await res.json();
      return data.markdown;
    }};
APPX
echo "App.tsx modified"