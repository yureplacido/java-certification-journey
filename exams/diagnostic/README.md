# 1Z0-830 Diagnostic Exam

## Overview

This diagnostic exam assesses your current knowledge of Java SE 21 in preparation for the **Oracle Certified Professional: Java SE 21 Developer** certification (1Z0-830).

## Exam Format

| Attribute | Value |
|-----------|-------|
| **Total Questions** | 50 |
| **Question Type** | Multiple choice (4 options, exactly 1 correct) |
| **Time Limit** | 120 minutes |
| **Passing Score** | 68% (34/50 correct) |
| **Delivery** | Paper-based (self-assessed) |

## Sections Covered

| # | Section | Questions | Weight |
|---|---------|-----------|--------|
| 1 | Language Basics | 1-6 | ~12% |
| 2 | Date-Time API | 7-10 | ~8% |
| 3 | Flow Control | 11-15 | ~10% |
| 4 | OOP - Classes, Records, Nested, Var | 16-20 | ~10% |
| 5 | OOP - Inheritance, Sealed, Patterns, Enums | 21-26 | ~12% |
| 6 | Exceptions | 27-30 | ~8% |
| 7 | Collections | 31-34 | ~8% |
| 8 | Functional | 35-38 | ~8% |
| 9 | Streams | 39-42 | ~8% |
| 10 | Modules (JPMS) | 43-45 | ~6% |
| 11 | Concurrency | 46-48 | ~6% |
| 12 | Annotations | 49-50 | ~4% |

## How to Take This Exam

1. **Set a timer** for 120 minutes
2. Open `questions.md` and work through all 50 questions
3. Record your answers on paper or in a separate file
4. When time is up, stop — do not go back to change answers
5. Use `answersheet.md` to check your answers
6. Calculate your score using the scoring table in `answersheet.md`

## After the Exam

- Review every question you got wrong
- Read the explanation carefully — note whether the error was:
  - **Compile-time** (you couldn't see the code wouldn't compile)
  - **Runtime** (you predicted wrong output or exception)
  - **Knowledge gap** (you didn't know the API or rule)
- Update your study plan based on weak sections
- Record your results in `docs/progress.yaml` (under `diagnostic`)

## Key Topics Tested (Java 21 Specific)

This exam emphasizes these Java 21 features with high exam weight:

- **Records** — compact constructors, auto-generated methods, finality
- **Sealed types** — `sealed`, `permits`, `non-sealed`, exhaustive switches
- **Pattern matching** — `instanceof` and `switch` patterns, guards, null handling
- **Virtual threads** — `newVirtualThreadPerTaskExecutor()`, thread behavior
- **Sequenced collections** — `SequencedCollection`, `getFirst()`, `addFirst()`
- **Text blocks** — incidental spacing, `lines()`, escape sequences
- **Try-with-resources** — execution order, suppressed exceptions
- **Generics and wildcards** — `? extends`, `? super`, erasure, PECS

## Files in This Directory

| File | Description |
|------|-------------|
| `questions.md` | All 50 questions with code, options, answers, and explanations |
| `answersheet.md` | Quick-reference answer key with scoring table |
| `README.md` | This file — exam instructions and format |

## Validation

All Java code in this exam has been compiled and tested with:
- **Java version:** 21.0.2 (via mise)
- **No Java 22+ features** are used
- Code that should compile does compile
- Code that should not compile has verified compilation errors
- Runtime behavior has been verified against actual JVM output
