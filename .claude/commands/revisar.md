---
description: Revisa as mudanças atuais procurando gargalos de performance, com foco em consultas ao banco
---

Use o agente `revisor-performance` para revisar as mudanças de código.

Alvo: $ARGUMENTS

Se o alvo acima estiver vazio, o agente decide sozinho, nesta ordem: `git diff HEAD`, depois `git diff --staged`, depois `git diff main...HEAD`.

Exemplos de alvo: `--staged`, `main...HEAD`, `HEAD~3`, ou o caminho de um arquivo ou pasta.

Repasse o relatório do agente na íntegra, sem resumir os achados. Se ele não encontrar nada, diga isso em uma linha.
