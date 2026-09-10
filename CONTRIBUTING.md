# Contributing

Este repositório é um laboratório pessoal de estudo. Contribuições são bem-vindas, mas o foco é o
plano de estudos do dono — antes de propor mudanças estruturais (novas dependências, multi-módulo,
reorganização de diretórios), abra uma issue/discussão.

## Como contribuir conteúdo novo

1. **Siga o layout:** código Java vai em `src/main/java/com/placido/certification/<tópico>/` com
   testes correspondentes em `src/test/java/...`. Notas/pegadinhas vão em `docs/`.
2. **Tópicos novos:** crie/atualize a entrada correspondente em `docs/roadmap` e `docs/progress.yaml`.
3. **Mantenha a baseline:** tudo deve compilar com `maven.compiler.release=21` (nada de Java 22+
   na fase 1Z0-830).
4. **Pegadinhas e erros:** novas pegadinhas vão em `docs/traps/`; erros cometidos no estudo, em
   `docs/error-log.md`.
5. **Verifique antes de finalizar:** sempre rode a suíte.

## Verificação

```bash
mise x -- mvn test
```