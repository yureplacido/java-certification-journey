import React from "react";
import { Card } from "@/components/ui/card";
import { Tabs } from "@/components/ui/tabs";
import { TabsList } from "@/components/ui/tabs-list";
import { TabsTrigger } from "@/components/ui/tabs-trigger";
import { TabsContent } from "@/components/ui/tabs-content";

interface TrapCardProps {
  loadTrap: (path: string) => Promise<string>;
}

interface TrapInfo {
  title: string;
  content: string;
}

export const TrapCard: React.FC<TrapCardProps> = ({
  loadTrap,
}) => {
  const [trap, setTrap] = React.useState<{ title: string; content: string } | null>(
    null
  );
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    loadTrap("docs/traps/stringbuilder-insert-replace-indices.md").then(
      (content) => {
        // Extract title from markdown
        const titleMatch = content.match(/^# (.+)$/m);
        const title = titleMatch ? titleMatch[1] : "Trap: StringBuilder";
        setTrap({ title, content });
        setLoading(false);
      }
    ).catch((err) => {
      setError(err instanceof Error ? err.message : "Erro ao carregar trap");
      setLoading(false);
    });
  }, [loadTrap]);

  if (loading && !trap) {
    return (
      <Card className="h-64">
        <div className="h-full grid place-items-center text-muted-foreground">
          Carregando trap...
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className="h-64">
        <div className="h-full p-4 text-red-400">
          <p>Erro: {error}</p>
        </div>
      </Card>
    );
  }

  return (
    <Card>
      <div className="p-6">
        <h3 className="text-xl font-semibold mb-4">{trap.title}</h3>
        <p className="text-muted-foreground mb-4">
          {trap.content.substring(0, 200)}...
        </p>
        <Button
          asChild
          className="mt-2"
          size="sm"
        >
          <svg
            className="w-4 h-4 inline mr-1"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path
              d="M15 12a3 3 0 100-6 3 3 0 000 6Z"
            />
            <path
              d="M1 3a2 2 0 012-2h4a2 2 0 012 2v6a2 2 0 01-2 2H3v-6Zm22-2a2 2 0 01-2 2h-4a2 2 0 01-2-2v-6a2 2 0 012-2h4a2 2 0 012 2v6Zm-6-6a3 3 0 100-6 3 3 0 000 6ZM5 3a2 2 0 012-2h6a2 2 0 012 2v6a2 2 0 01-2 2H5v-6Zm6 7a2 2 0 110 4h4a2 2 0 110-4h-4Zm-4-4a2 2 0 100-4 2 2 0 000 4Zm12 4a2 2 0 110-4h-4a2 2 0 110 4h4Z"
            />
          </svg>
          Ver mais
        </Button>
      </Card>
    </div>
  );
};