import { useEffect, useState } from "react";

interface StudyTodayDTO {
  currentWeek: number;
  currentDay: number;
  dayTitle: string;
  activities: {
    id: string;
    status: "completed" | "pending";
  }[];
  progress: {
    score: string;
    scorePercent: number;
    passing: boolean;
    totalQuestions: number;
  };
  examContext: {
    id: string;
    status: string;
    grade: number;
    passing: boolean;
  };
}

interface ContentDTO {
  path: string;
  title: string;
  markdown: string;
}

export const useStudyData = () => {
  const [today, setToday] = useState<StudyTodayDTO | null>(null);
  const [progress, setProgress] = useState<any>(null);
  const [plan, setPlan] = useState<any>(null);
  const [content, setContent] = useState<{ markdown: string; title: string } | null>(
    null
  );
  const [trapContent, setTrapContent] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchToday = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/study/today");
      if (!res.ok) throw new Error("Failed to fetch study today");
      const data = await res.json();
      setToday(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido");
    } finally {
      setLoading(false);
    }
  };

  const fetchProgress = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/progress");
      if (!res.ok) throw new Error("Failed to fetch progress");
      const data = await res.json();
      setProgress(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido");
    } finally {
      setLoading(false);
    }
  };

  const fetchPlan = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/study/plan");
      if (!res.ok) throw new Error("Failed to fetch study plan");
      const data = await res.json();
      setPlan(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido");
    } finally {
      setLoading(false);
    }
  };

  const fetchContent = async (path: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/content${path}`);
      if (!res.ok) throw new Error("Failed to fetch content");
      const data: ContentDTO = await res.json();
      setContent({ markdown: data.markdown, title: data.title });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido");
    } finally {
      setLoading(false);
    }
  };

  const fetchTrap = async (path: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/content${path}`);
      if (!res.ok) throw new Error("Failed to fetch trap");
      const data: ContentDTO = await res.json();
      setTrapContent(data.markdown);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchToday();
    fetchProgress();
    fetchPlan();
  }, []);

  return {
    today,
    progress,
    plan,
    content,
    trapContent,
    loading,
    error,
    refetch: () => {
      fetchToday();
      fetchProgress();
    },
  };
};