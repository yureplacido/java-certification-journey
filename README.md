# java-certification-journey

Laboratório pessoal de preparação para a certificação **Oracle Java SE 21 Developer Professional**,
com **Java 25** como objetivo secundário (estudado depois, como delta em relação ao Java 21).

## 1. Objetivo do projeto

Este repositório é um laboratório prático de estudo para a certificação **Java SE 21 Developer
Professional**. Ele concentra o planejamento, os exemplos de código, os testes, as pegadinhas e o
registro de erros em um único lugar, organizado por tópico do exame.

## 2. Perfil do estudante

- Software Architect.
- ~15 anos de experiência com Java.
- Trabalho profissional com Java, Spring, arquitetura distribuída, DDD, microservices, Kafka,
  RabbitMQ, Docker e Kubernetes.

Por isso o material aqui é **avançado e orientado a detalhes** que aparecem em exames de
certificação profissional — não é um curso introdutório de programação.

## 3. Certificação alvo

| Item | Valor |
| --- | --- |
| Certificação | Oracle Certified Professional: Java SE 21 Developer |
| Exame | 1Z0-830 |
| Formato | 50 questões de múltipla escolha, 120 minutos |
| Nota de corte | 68% (confirmar na página oficial) |

> Detalhes e objetivos oficiais em https://education.oracle.com/oracle-certified-professional-java-se-21-developer/pexam_1Z0-830

## 4. Estratégia Java 21 → Java 25

1. **Fase 1 (baseline):** dominar o exame 1Z0-830 usando Java 21. Todo o código do laboratório roda
   em Java 21 (compilação `--release 21`), cobrindo os objetivos oficiais em profundidade.
2. **Fase 2 (delta):** estudar Java 25 comparativamente — o que mudou entre 21 e 25, JEPs
   finalizados, migração de código e impacto em APIs (`java25/`).

Java 25 (LTS, lançado em set/2025) não será usado como baseline: primeiro a certificação 1Z0-830,
depois o delta.

## 5. Estrutura do repositório

```
java-certification-journey/
├── AGENTS.md               # instruções para agentes/contribuidores
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── mise.toml               # Java 21.0.2 e Maven 3.9.16 (versões geridas pelo mise)
├── pom.xml                 # projeto Maven — Java 21, JUnit 5
├── src/
│   ├── main/java/com/placido/certification/   # código de estudo por pacote/tópico
│   └── test/java/com/placido/certification/
│
├── docs/
│   ├── roadmap/            # planejamento (16-weeks.md, java-21.md, java-25.md)
│   ├── study-log/          # diário de estudo
│   ├── cheat-sheets/       # folhas de referência rápida
│   ├── traps/              # pegadinhas de exame (o material mais valioso)
│   ├── error-log.md        # erros cometidos e lições
│   └── progress.yaml       # fonte de verdade do progresso
│
├── language/   oop/   exceptions/   generics/   collections/
├── functional/ streams/   datetime/   io/   modules/
├── annotations/   reflection/   jdbc/   concurrency/   # notas e exercícios por tópico
│
├── exams/
│   ├── diagnostic/         # prova diagnóstica inicial
│   ├── mock-01/  mock-02/  mock-03/   # simulados progressivos
│   └── final/              # simulado final antes do exame real
│
└── java25/
    ├── delta/              # o que mudou entre 21 → 25
    ├── language/  apis/  migration/
```

Código Java compilado pelo Maven fica em `src/main/java/com/placido/certification/<tópico>/`; os
diretórios de tópico na raiz guardam notas, exemplos e exercícios em formato livre.

## 6. Metodologia de estudo

Ciclo por tópico (profundo, orientado a exame):

1. **Planejar:** ler o objetivo oficial correspondente no roadmap (`docs/roadmap/java-21.md`).
2. **Praticar:** escrever exemplos e exercícios direcionados no diretório do tópico, com testes
   JUnit 5 demonstrando o comportamento esperado.
3. **Caçar pegadinhas:** anotar em `docs/traps/` as armadilhas clássicas do tópico (o que o examinador
   costuma cobrar).
4. **Registrar erros:** sempre que errar, gravar em `docs/error-log.md` — erro, causa raiz, lição.
5. **Atualizar progresso:** refletir o estado do tópico em `docs/progress.yaml`.
6. **Autoavaliar:** provas diagnóstica/simuladas em `exams/` em pontos-chave do cronograma.

## 7. Como executar os testes

```bash
mise x -- mvn test          # recomendado (garante Java 21 + Maven do mise.toml)
# ou, com o mise já ativado no shell:
mvn test
```

Exemplo com foco em um único teste:

```bash
mise x -- mvn test -Dtest=SmokeTest
```

## 8. Como acompanhar o progresso

- **`docs/progress.yaml`** — fonte de verdade: status por tópico (`planned` → `studying` → `review`
  → `done`) e das provas (`pending`/`done`).
- **`docs/roadmap/16-weeks.md`** — cronograma de 16 semanas.
- **`docs/study-log/`** — entradas diárias (o que estudou, o que errou, o que fixou).
- **`docs/cheat-sheets/`** e **`docs/traps/`** — material de revisão rápida pré-exame.
- **`docs/error-log.md`** — registro cumulativo de erros, revisitado na reta final.