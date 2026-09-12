import React from "react";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs } from "@/components/ui/tabs";
import { TabsList } from "@/components/ui/tabs-list";
import { TabsTrigger } from "@/components/ui/tabs-trigger";
import { TabsContent } from "@/components/ui/tabs-content";

interface Activity {
  id: string;
  type: "review" | "study" | "experiment" | "practice" | "quiz";
  title: string;
  status: "completed" | "pending";
}

interface TodayDashboardProps {
  currentWeek: number;
  currentDay: number;
  dayTitle: string;
  activities: Activity[];
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
  loadContent: (path: string) => Promise<{ markdown: string; title: string }>;
  loadTrap: (path: string) => Promise<string>;
}

export const TodayDashboard: React.FC<TodayDashboardProps> = ({
  currentWeek,
  currentDay,
  dayTitle,
  activities,
  progress,
  examContext,
  loadContent,
  loadTrap,
}) => {
  return (
    <main className="p-4 md:p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header section */}
        <header className="mb-6">
          <h1 className="text-3xl font-bold text-foreground mb-2">
            Java Certification Journey
          </h1>
          <p className="text-muted-foreground">
            Week {currentWeek} · Day {currentDay}
          </p>
        </header>

        {/* Today's focus */}
        <div className="card mb-6">
          <div className="card-header">
            <h2 className="card-title flex items-center gap-2">
              <span className="w-8 h-8 rounded bg-primary/10 flex items-center justify-center">
                <svg
                  className="w-4 h-4 text-primary"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path
                    d="M12 3v5h5v2h-5V7H9v2H7v5h2v5h5v-2h5v-5h-2V7h-5V3h5ZM7 15h10v2H7v-2H7v2H7v-2H7ZM7 10h2v5H7v-5H7Z"
                  />
                </svg>
              </span>
              {dayTitle}
            </h2>
          </div>
          <div className="card-body">
            <p className="text-muted-foreground mb-4">
              Foco: Language Basics + Date-Time - StringBuilder
            </p>

            <ActivityList activities={activities} />
          </div>
        </div>

        {/* Progress and Exams sidebar */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {/* Progress Card */}
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Progresso</h3>
            </div>
            <div className="card-body">
              <Progress
                value={progress.scorePercent}
                max={progress.totalQuestions}
                variant="primary"
              />
              <p className="text-sm text-muted-foreground mt-2">
                {progress.score} — {progress.scorePercent}%
              </p>
              {passingStatus(passingStatusVal(progress.passing))}
            </div>
          </div>

          {/* Exam Card */}
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Exame</h3>
            </div>
            <div className="card-body">
              <Badge
                variant={examContext.passing ? "default" : "destructive"}
              >
                {examContext.status}
              </Badge>
              <p className="text-sm text-muted-foreground mt-1">
                {examContext.grade}/{examContext.totalQuestions}
              </p>
            </div>
          </div>

          {/* Trap Card */}
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Trap</h3>
            </div>
            <div className="card-body">
              <TrapCard />
            </div>
          </div>
        </div>

        {/* Content and Activities area */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {/* Left: Activities */}
          <div className="md:col-span-1">
            <ActivityList activities={activities} />
          </div>

          {/* Right: Content */}
          <div className="col-span-1">
            <Tabs defaultValue="content" className="w-full">
              <TabsList className="justify-center">
                <TabsTrigger value="content" className="py-2 px-3">
                  Conteúdo
                </TabsTrigger>
                <TabsTrigger value="trap" className="py-2 px-3">
                  Trap
                </TabsTrigger>
              </TabsList>

              <TabsContent value="content" >
                <ContentArea loadContent={loadContent} />
              </TabsContent>

              <TabsContent value="trap">
                <TrapArea loadTrap={loadTrap} />
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </div>
    </main>
  );
};

function passingStatusVal(passing: boolean) {
  return passing ? "Concluído" : "Em andamento";
}

function passingStatusDisplay(status: string) {
  return (
    <span>
      <Badge
        variant={status === "Concluído" ? "default" : "destructive"}
      >
        {status}
      </Badge>
    </span>
  );
}