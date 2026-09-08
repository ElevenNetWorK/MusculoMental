# Validação da etapa 5

Data: 08/09/2026.

## Resultado

- APK de desenvolvimento gerado e instalado em um Samsung SM-A305GT com Android 11.
- Quatorze testes unitários aprovados, sem falhas.
- Nove testes instrumentados aprovados no aparelho, sem falhas, em 20,331 segundos.
- Análise estática Android concluída sem erros.
- Versão do aplicativo atualizada para 0.5.0, código 5.

## Cobertura verificada

Além dos fluxos anteriores, os testes verificam:

1. cálculo do tempo decorrido e restante pelo relógio do sistema;
2. limites do tempo padrão;
3. início e encerramento do intervalo pela interface;
4. associação do intervalo à série correta;
5. persistência das preferências e do cronômetro após reabrir o banco;
6. pausa, retomada e ajuste do alvo;
7. correção manual marcada como aproximada;
8. exclusão do registro;
9. migração encadeada da versão 1 para a versão 5.

## Limitações verificadas

- Não há notificação persistente do sistema em segundo plano.
- O histórico completo de sessões ainda não possui tela própria.
- Dados continuam locais e são apagados ao desinstalar o aplicativo ou limpar seus dados.
- A variante de publicação ainda não possui configuração de assinatura.
