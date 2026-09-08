# Validação da etapa 3

Data: 07/09/2026.

## Resultado

- APK de desenvolvimento gerado e instalado em um Samsung SM-A305GT com Android 11.
- Nove testes unitários aprovados, sem falhas.
- Seis testes instrumentados aprovados no aparelho, sem falhas, em 14,977 segundos.
- Análise estática Android concluída sem erros.
- Versão do aplicativo atualizada para 0.3.0, código 3.

## Cobertura verificada

Além dos fluxos das etapas anteriores, os testes verificam:

1. validação de nome, presença de exercícios, séries, repetições, carga e intervalo;
2. criação de uma ficha pela interface usando um exercício da biblioteca;
3. exibição separada dos valores planejados;
4. persistência da ordem e de todos os campos após reabrir o banco;
5. edição, duplicação e arquivamento;
6. migração encadeada da versão 1 para a versão 3 preservando o perfil;
7. estados vazio, ativo e arquivado na interface.

A tela inicial, a lista vazia e o editor também foram inspecionados visualmente no aparelho real.

## Limitações esperadas

- A ficha ainda não pode ser executada; não existem séries realizadas nem histórico.
- A carga é apenas um valor digitado pelo usuário e não constitui recomendação.
- Os dados são locais e são apagados ao desinstalar o aplicativo ou limpar seus dados.
- A variante de publicação ainda não possui configuração de assinatura.
