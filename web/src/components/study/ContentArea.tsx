import React from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Tabs } from "@/components/ui/tabs";
import { TabsList } from "@/components/ui/tabs-list";
import { TabsTrigger } from "@/components/ui/tabs-trigger";
import { TabsContent } from "@/components/ui/tabs-content";

interface ContentAreaProps {
  loadContent: (path: string) => Promise<{ markdown: string; title: string }>;
}

interface ContentTab {
  value: string;
  label: string;
}

export const ContentArea: React.FC<ContentAreaProps> = ({
  loadContent,
}) => {
  const [content, setContent] = React.useState<{
    markdown: string;
    title: string;
  } | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const loadContentByPath = async (path: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await loadContent(path);
      setContent(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido");
    } finally {
      setLoading(false);
    }
  };

  // Load default content on mount
  React.useEffect(() => {
    loadContentByPath("language/stringbuilder-insert-replace.md");
  }, []);

  if (loading && !content) {
    return (
      <div className="h-64 grid place-items-center text-muted-foreground">
        Carregando conteúdo...
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-64 border rounded bg-red-100/20 p-4 text-red-400">
        <p>Erro ao carregar conteúdo: {error}</p>
      </div>
    );
  }

  if (!content) {
    return null;
  }

  return (
    <Card>
      <div className="p-6">
        <h3 className="text-xl font-semibold mb-4">{content.title}</h3>
        <div
          className="prose max-w-none text-left"
          dangerouslySetInnerHTML={{ __html: content.markdown }}
        />
      </div>
    </Card>
  );
};