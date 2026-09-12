import React from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { DialogContent } from "@/components/ui/dialog";
import { DialogHeader } from "@/components/ui/dialog";
import { DialogTitle } from "@/components/ui/dialog";
import { DialogDescription } from "@/components/ui/dialog";
import { useState } from "react";

interface AgentPromptProps {
  generatePrompt: () => string;
}

export const AgentPrompt: React.FC<AgentPromptProps> = ({
  generatePrompt,
}) => {
  const [showDialog, setShowDialog] = useState(false);
  const [copied, setCopied] = useState(false);
  const [promptText, setPromptText] = useState("");

  // Generate and store the prompt
  React.useEffect(() => {
    const prompt = generatePrompt();
    setPromptText(prompt);
  }, [generatePrompt]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(promptText);
      setCopied(true);
      // Reset after 2 seconds
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      // Fallback for older browsers
      const textarea = document.createElement("textarea");
      textarea.value = promptText;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div>
      <Button
        variant="secondary"
        onClick={() => setShowDialog(true)}
        className="mb-2"
      >
        Gerar Prompt
      </Button>

      <Dialog open={showDialog} onOpenChange={setShowDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Prompt para Agente IA</DialogTitle>
            <DialogDescription>
              Contexto operacional para auxiliar no estudo de Java SE 21
            </DialogDescription>
          </DialogHeader>

          <div className="p-4 mb-4 rounded-border bg-background/50 min-h-[150px]">
            <pre
              className="text-xs text-muted-foreground overflow-auto"
            >
              {promptText}
            </pre>
          </div>

          <div className="flex justify-end gap-2">
            <Button
              onClick={handleCopy}
              disabled={copied}
              variant="secondary"
              size="sm"
            >
              {copied ? "Copiado!" : "Copiar"}
            </Button>
            <Button
              variant="link"
              size="sm"
              onClick={() => setShowDialog(false)}
            >
              Fechar
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};