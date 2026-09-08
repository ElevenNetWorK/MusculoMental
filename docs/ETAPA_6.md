# Etapa 6 — histórico local e evolução

## Escopo entregue

O aplicativo ganhou uma área de histórico acessível pela tela inicial. Ela usa somente sessões concluídas e oferece quatro visões:

- resumo com frequência na semana e no mês, quantidade total, tempo acumulado e volume estimado;
- calendário mensal com marcação e seleção dos dias que possuem sessões;
- evolução da maior carga registrada por exercício em cada sessão, apresentada em gráfico e lista cronológica;
- recordes básicos por exercício: maior carga, maior número de repetições em uma série e maior volume de uma série.

A lista de sessões abre o detalhe completo do treino. O detalhe apresenta a prescrição congelada no início da sessão e, separadamente, repetições, cargas, séries ignoradas, intervalos realizados e avaliação informada pelo praticante.

## Decisões técnicas

- Nenhuma tabela ou migração foi necessária. O histórico é derivado das sessões e séries já persistidas pelo Room.
- Somente sessões com estado `COMPLETED` e horário de término entram nas métricas. Sessões pausadas ou canceladas permanecem fora do histórico.
- A semana começa na segunda-feira e os limites de semana, mês e dia usam o fuso horário local do aparelho.
- O volume estimado soma carga multiplicada por repetições apenas nas séries concluídas que possuem carga.
- A evolução considera a maior carga do exercício em cada sessão; sessões sem carga para o exercício não criam pontos no gráfico.
- Os cálculos ficam na camada de domínio e são determinísticos quando recebem horário e fuso, o que permite testar limites de período sem depender do relógio real.
- O gráfico foi implementado com recursos do próprio Compose, sem adicionar dependências.

## Privacidade e responsabilidade

Todos os dados continuam locais e privados por padrão. Os recordes servem para acompanhamento do que foi registrado; o aplicativo não recomenda aumento de carga nem substitui avaliação de profissional de Educação Física ou de saúde.

## Limitações esperadas

- Não há edição ou exclusão de uma sessão concluída nesta etapa.
- Calendário e métricas não permitem escolher intervalos personalizados.
- A evolução apresenta carga máxima; comparações de volume por grupo muscular e tendências avançadas ficam para etapas futuras.
- Desinstalar o aplicativo ou limpar seus dados remove o histórico local.

## Próxima etapa proposta

Após aprovação, a próxima etapa pode consolidar o MVP com revisão de acessibilidade, tratamento de recuperação e falhas, testes de uso em treino e preparação de um pacote de teste para distribuição controlada.
