# Etapa 4 — execução e registro por série

## Escopo entregue

Uma ficha ativa agora pode iniciar uma sessão. O usuário avança ou volta pelos exercícios, registra carga e repetições de cada série, copia os valores da série anterior, conclui ou ignora séries, pausa e retoma o treino e finaliza com um resumo.

Observação, esforço e desconforto são opcionais. O resumo mostra séries concluídas e ignoradas, registros por exercício e volume estimado quando houve carga informada.

## Decisões técnicas

- O banco Room foi atualizado da versão 3 para a 4 por migração explícita.
- Ao iniciar uma sessão, o aplicativo grava uma fotografia do nome da ficha, ordem dos exercícios e valores planejados. Alterações posteriores na ficha não reescrevem essa sessão.
- Cada série guarda estado planejado, concluído ou ignorado. Carga e repetições executadas ficam separadas dos valores planejados.
- Somente uma sessão pode permanecer em andamento ou pausada por vez.
- Abrir uma sessão pausada a coloca novamente em andamento. A posição atual do exercício é persistida.
- Copiar a série anterior preenche a próxima série, mas o usuário ainda precisa confirmar sua conclusão.
- A finalização exige que todas as séries sejam concluídas ou ignoradas.
- O cancelamento exige uma confirmação explícita na interface.

## Segurança e limites

Repetições executadas aceitam valores de 1 a 1000 e a carga opcional aceita de 0 a 1000 kg. Esforço e desconforto usam escala opcional de 1 a 10, sem interpretação clínica. A tela recomenda interromper o exercício e buscar avaliação profissional diante de dor forte, súbita ou persistente.

## Fora desta etapa

O cronômetro de descanso e o registro do intervalo efetivamente realizado pertencem à próxima etapa. Histórico navegável, calendário, gráficos e recordes também permanecem para uma etapa posterior.

## Próxima etapa proposta

Após aprovação, a etapa 5 pode implementar o cronômetro de descanso persistente, com início manual ou após uma série, pausa, retomada, ajuste e registro opcional do tempo efetivamente realizado.
