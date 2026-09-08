# Etapa 3 — fichas pessoais de treino

## Escopo entregue

O usuário pode criar uma ficha com nome e uma lista ordenada de exercícios da biblioteca local. Cada item guarda séries, repetições, carga sugerida opcional e intervalo planejado. Também é possível editar, duplicar, arquivar e restaurar fichas.

As fichas aparecem identificadas como “Criado por você”. Ativas e arquivadas ficam em listas separadas, e o arquivamento é reversível.

## Decisões técnicas

- O banco Room foi atualizado da versão 2 para a 3 com migração explícita, preservando perfil e biblioteca.
- Ficha e exercício planejado são entidades separadas. A posição de cada item é persistida e protegida por um índice único dentro da ficha.
- Toda ficha desta etapa tem origem pessoal. Treinos prescritos e aulas coletivas continuarão sendo tipos distintos quando forem implementados.
- Séries e repetições usam números inteiros. A carga é opcional e aceita decimal; o intervalo é armazenado em segundos.
- A edição substitui somente a versão planejada atual. Nenhum histórico de execução existe ainda, evitando misturar valores planejados e realizados.
- A duplicação cria uma ficha ativa independente com o sufixo “(cópia)”.

## Validações

- Nome obrigatório, com até 80 caracteres.
- Pelo menos um exercício.
- De 1 a 20 séries e de 1 a 100 repetições por exercício.
- Carga opcional entre 0 e 1000 kg.
- Intervalo entre 0 e 600 segundos.

Esses limites validam a entrada e protegem o armazenamento. Eles não representam prescrição ou recomendação automática.

## Fora desta etapa

Iniciar treino, registrar séries realizadas, cronômetro, pausa e retomada, histórico e progressão de carga continuam fora do código atual. Backend, autenticação, professor real, pagamentos, IA, recursos sociais e relógios também permanecem fora do escopo.

## Próxima etapa proposta

Após aprovação, a etapa 4 pode implementar a execução de uma ficha, registrando cada série e mantendo os valores planejados e realizados em campos separados.
