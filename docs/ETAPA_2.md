# Etapa 2 — biblioteca local de exercícios e músculos

## Escopo entregue

A etapa amplia o aplicativo da etapa 1 sem remover ou recriar o perfil existente. A tela inicial dá acesso a uma biblioteca local com oito exercícios demonstrativos, dez músculos e as relações entre eles.

O usuário pode buscar pelo nome de um exercício ou músculo, filtrar por região corporal e equipamento, abrir instruções e erros comuns de execução, navegar para a ficha de um músculo e voltar aos exercícios relacionados.

## Decisões técnicas

- A biblioteca usa o mesmo banco Room do perfil, atualizado da versão 1 para a versão 2 por uma migração explícita.
- Exercícios, músculos e seus vínculos são entidades separadas. A relação registra se cada músculo é principal ou assistente.
- O conteúdo inicial é semeado localmente na primeira consulta. O aplicativo continua funcional sem rede, conta ou backend.
- A busca considera nomes de exercícios e os nomes comuns e anatômicos dos músculos. Região e equipamento podem ser combinados.
- O estado de carregamento, falha e ausência de resultados é explícito na interface.

## Conteúdo e segurança

Os textos anatômicos e de execução são conteúdo demonstrativo em revisão. As telas informam que o material é educativo e não substitui avaliação profissional. Mídias de execução não foram incluídas até que origem, direitos de uso e processo de revisão estejam definidos.

## Fora desta etapa

Treino em execução, séries e cargas, histórico, Modo História, vínculo professor–aluno, autenticação, backend, pagamentos, recursos sociais, IA e integração com relógios continuam fora do código atual.

## Próxima etapa proposta

Após aprovação, a etapa 3 pode implementar a criação local de fichas de treino usando os exercícios já cadastrados, com validação e persistência. O escopo exato deve ser aprovado antes da programação.
