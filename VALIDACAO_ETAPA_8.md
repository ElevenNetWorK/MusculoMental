# Validação da etapa 8

Data: 08/09/2026.

## Resultado

- APK de desenvolvimento gerado e instalado em um Samsung SM-A305GT com Android 11.
- Dezoito testes unitários aprovados, sem falhas.
- Doze testes instrumentados aprovados no aparelho, sem falhas, em 30,161 segundos.
- Análise estática Android concluída sem erros; permaneceu um aviso informativo sobre versão mais recente do Gradle.
- Versão instalada confirmada como 0.8.0, código 8.
- Nenhuma publicação ou transmissão externa de dados foi realizada.

## Cobertura acrescentada

1. correção de repetições e carga pela interface após concluir a sessão;
2. retorno imediato da confirmação junto à ação;
3. preservação visual da prescrição após a correção;
4. preservação de início, fim e horário original da série no banco;
5. correção de série ignorada para concluída;
6. atualização de avaliação e volume;
7. bloqueio do botão Voltar enquanto há uma transação ativa;
8. regressão de perfil, biblioteca, ficha, retomada, cronômetro e histórico.

## Verificação visual

- Tema escuro e fonte em 150% ativados simultaneamente no aparelho.
- Cabeçalho, texto, botões e aviso de privacidade permaneceram legíveis.
- Conteúdo excedente permaneceu acessível por rolagem vertical.
- Tema claro e escala original de 110% restaurados após o teste.

## Limitações verificadas

- Ainda não há trilha de auditoria para correções.
- Não houve teste com participantes externos ou auditoria completa com TalkBack.
- O cronômetro continua sem notificação persistente após encerramento do processo.
- A variante de publicação e a assinatura de produção continuam indefinidas.
