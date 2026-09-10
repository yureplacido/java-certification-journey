---
description: "Audita um tópico do repositório — verifica código, notas e questões contra a JLS/API e aponta erros. Somente leitura: não modifica arquivos. Use para validar o que já foi estudado."
agent: java-reviewer
---

Revise (audite) um tópico do laboratório.

Tópico: $ARGUMENTS (ex.: stream; se vazio, escolha o tópico com a maior lacuna indicada em
`docs/progress.yaml`).

Escopo da auditoria:
- Código em `src/main/java/com/placido/certification/<tópico>/` e testes associados.
- Notas e exemplos no diretório do tópico.
- Questões/traps relacionados (se houver).

Procedimento:
1. Confirme os comportamentos contra a JLS/Java API ou executando o código (`mise x -- mvn test`);
   em dúvida, proponha um experimento mínimo (sem criar arquivos permanentes).
2. Procure erros de compile-time, runtime e pegadinhas típicas de exame; classifique cada achado
   (compile-time, runtime, JLS, Java API, JVM, certificação).
3. Verifique se notas/afirmações são verificáveis e se há conteúdo duplicado.

Saída: relatório com referências `arquivo:linha`, classificação de cada achado e explicação
correta. Você NÃO deve modificar, criar ou apagar arquivos (permissão de edição negada).