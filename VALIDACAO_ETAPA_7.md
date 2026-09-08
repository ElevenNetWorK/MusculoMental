# Validação da etapa 7

Data: 08/09/2026.

## Resultado

- APK de desenvolvimento gerado e instalado em um Samsung SM-A305GT com Android 11.
- Dezoito testes unitários aprovados, sem falhas.
- Onze testes instrumentados aprovados no aparelho, sem falhas, em 26,888 segundos.
- Análise estática Android concluída sem erros; permaneceu um aviso informativo sobre versão mais recente do Gradle.
- Versão atualizada para 0.7.0, código 7.
- Versão instalada confirmada pelo gerenciador de pacotes do aparelho.

## Cobertura acrescentada

1. falha inicial e nova tentativa da biblioteca;
2. preservação da pesquisa ativa durante a recuperação;
3. uso do botão Voltar do sistema durante uma sessão;
4. transição da sessão para pausada sem cancelar ou concluir;
5. retorno à lista e retomada da mesma sessão;
6. regressão dos fluxos completos de perfil, biblioteca, ficha, sessão, cronômetro e histórico.

## Auditoria de acessibilidade e interface

- Cabeçalhos semânticos incluídos nas telas principais.
- Mensagens de erro importantes configuradas para anúncio por tecnologia assistiva.
- Progresso de séries exposto semanticamente.
- Descrição textual mantida no gráfico de evolução.
- Tela inicial revisada no aparelho com escala de fonte 150%, sem corte horizontal.
- Escala original do aparelho, 110%, restaurada após a revisão.
- Lint verificou tamanhos de toque e demais regras Android sem apontar erro.

## Limitações verificadas

- TalkBack ainda precisa de auditoria manual com usuário real.
- Valores digitados em série não confirmada não são considerados registro persistido.
- Não há notificação persistente do cronômetro após encerramento do processo.
- O APK é de desenvolvimento e não está pronto para loja.
- Não houve publicação nem transmissão de dados externos.
