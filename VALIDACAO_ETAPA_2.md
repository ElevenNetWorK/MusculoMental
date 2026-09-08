# Validação da etapa 2

Data: 07/09/2026.

## Resultado

- APK de desenvolvimento gerado, instalado e aberto em um Samsung SM-A305GT com Android 11.
- Sete testes unitários aprovados, sem falhas.
- Quatro testes instrumentados aprovados no aparelho, sem falhas, em 10,948 segundos.
- Análise estática Android concluída sem erros.

## Cobertura verificada

Os testes unitários cobrem as regras do perfil e as combinações de busca e filtros da biblioteca.

No aparelho foram verificados:

1. criação, edição e cancelamento do perfil;
2. persistência do perfil após reabrir o banco;
3. criação única do catálogo local e persistência dos oito exercícios;
4. relações nos dois sentidos entre exercícios e músculos;
5. classificação da rosca inversa com braquial e braquiorradial como principais e bíceps como assistente;
6. migração do banco da versão 1 para a 2 preservando o perfil existente;
7. inicialização visual da biblioteca, filtros, lista e navegação em um aparelho real.

## Limitações esperadas

- O catálogo tem conteúdo demonstrativo que ainda exige revisão profissional.
- As mídias permanecem sinalizadas como pendentes até a definição de origem e direitos de uso.
- Os dados continuam locais e são apagados ao desinstalar o aplicativo ou limpar seus dados.
- A variante de publicação ainda não possui configuração de assinatura.
