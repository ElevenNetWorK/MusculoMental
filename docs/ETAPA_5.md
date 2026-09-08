# Etapa 5 — cronômetro de descanso

## Escopo entregue

Cada série concluída pode iniciar um intervalo manualmente ou de forma automática. O cronômetro usa o intervalo planejado da ficha, aceita contagem regressiva ou crescente e oferece pausa, retomada, encerramento, reinício e ajustes rápidos de menos ou mais 15 segundos.

O intervalo realizado pode ser registrado na série, corrigido manualmente ou excluído. Valores corrigidos aparecem como aproximados. O resumo do treino exibe o intervalo salvo junto à série correspondente.

## Preferências locais

- Cronômetro ativado ou desativado.
- Início manual ou automático após confirmar uma série.
- Contagem regressiva ou crescente.
- Aviso silencioso, por vibração ou por som.
- Encerramento do intervalo ativo ao confirmar a próxima série.
- Registro opcional do intervalo efetivamente realizado.
- Tempo padrão de 5 a 3600 segundos para situações sem intervalo planejado.

## Decisões técnicas

- O banco Room foi atualizado da versão 4 para a 5 por migração explícita.
- Cada intervalo pertence a uma série realizada e guarda separadamente tempo planejado, alvo ajustado e tempo efetivo.
- A contagem deriva do relógio do sistema, em vez de depender apenas de incrementos em memória. Ela se corrige após bloqueio de tela, ida ao segundo plano ou reabertura do aplicativo.
- Iniciar outro intervalo encerra o anterior, evitando dois cronômetros ativos.
- Copiar valores para uma nova série não dispara o cronômetro; ele começa somente após confirmar a série.
- Som e vibração não exigem conta, rede ou serviço externo.

## Limitações esperadas

- O aviso por som ou vibração depende de o processo do aplicativo continuar disponível. Ao reabrir o aplicativo, a contagem é recalculada corretamente.
- Notificação persistente do sistema e permissão de alerta em segundo plano ainda não foram implementadas.
- O histórico navegável e os gráficos continuam fora desta etapa.

## Próxima etapa proposta

Após aprovação, a etapa 6 pode implementar o histórico local de sessões, calendário simples, detalhes, evolução de carga, frequência semanal e recordes básicos.
