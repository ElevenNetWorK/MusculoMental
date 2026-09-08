# Instruções permanentes do projeto Músculo Mental

## Fonte de verdade

- Leia `docs/PLANEJAMENTO_COMPLETO.md` antes de alterar escopo, fluxos ou regras de negócio.
- Preserve as decisões registradas. Itens marcados como “a definir” continuam pendentes.
- `docs/MVP_PROPOSTO.md` é uma proposta de recorte, não autorização para descartar recursos futuros.
- Quando houver conflito, o planejamento completo e a instrução mais recente do usuário prevalecem.

## Forma de trabalho

- Antes de implementar uma etapa grande, apresente um plano curto e espere aprovação.
- Faça mudanças pequenas, verificáveis e fáceis de reverter.
- Não gere telas vazias apenas para aparentar avanço. Cada fluxo entregue deve funcionar de ponta a ponta dentro do limite da etapa.
- Não adicione bibliotecas sem justificar finalidade, manutenção e impacto.
- Nunca registre chaves, senhas, tokens ou dados pessoais reais no repositório.
- Ao terminar uma tarefa, informe arquivos alterados, testes executados, limitações e próximo passo recomendado.

## Tecnologia inicial

- Android nativo.
- Kotlin.
- Jetpack Compose e Material 3.
- Arquitetura em camadas, com apresentação, domínio e dados separáveis sem complexidade desnecessária.
- Estado de tela exposto por ViewModel.
- Coroutines e Flow para operações assíncronas.
- Room para persistência local quando a implementação começar.
- Navegação por Navigation Compose.
- Injeção de dependência: escolher somente após justificar; Hilt é a opção preferencial se necessário.
- Backend, autenticação remota, armazenamento de vídeos e sincronização ainda não estão definidos.

## Qualidade

- Use nomes claros em português no produto e nomes técnicos consistentes no código.
- Formate e compile o projeto após alterações relevantes.
- Crie testes para regras de negócio e persistência crítica.
- Preserve histórico: valores prescritos e executados são dados diferentes.
- Estados de carregamento, vazio, erro e sucesso devem ser tratados.
- A interface deve considerar fontes ampliadas, contraste, áreas de toque adequadas e uso durante o treino.
- O aplicativo deve tolerar interrupções, tela bloqueada e conexão instável nos fluxos de treino.

## Privacidade, saúde e responsabilidade

- Dados de saúde, fotos, limitações e evolução são privados por padrão.
- Compartilhamento exige consentimento explícito e deve poder ser revogado.
- Um vínculo com professor não libera automaticamente todo o histórico do aluno.
- IA é opcional, iniciada pelo usuário e nunca altera treinos automaticamente.
- Informações do smartwatch são estimativas e não constituem diagnóstico.
- O aplicativo oferece informação e organização; não substitui avaliação médica, fisioterapêutica, nutricional ou de Educação Física.
- Evite alegações clínicas e recomendações automáticas diante de dor, lesão ou risco.

## Regras essenciais do domínio

- Diferencie aluno, professor/personal e administração.
- Diferencie ficha individual de aula coletiva.
- Diferencie treino prescrito, treino pessoal planejado e treino livre.
- Preserve versões das fichas e o conteúdo vigente no dia de cada execução.
- Alterar um modelo não modifica fichas já atribuídas.
- Registre separadamente planejado e executado, inclusive carga, repetições e intervalo.
- Modo História é privado e autônomo por padrão.
- Conteúdo oficial e conteúdo criado por profissionais devem ser identificados separadamente.

## Restrições desta fase

- Não implementar pagamentos.
- Não implementar rede social.
- Não implementar diagnóstico, prescrição médica ou prescrição automática por IA.
- Não iniciar integração real com smartwatch antes da pesquisa de compatibilidade.
- Não decidir sozinho identidade visual, modelo gratuito/pago ou backend definitivo.
- Não publicar o aplicativo nem enviar dados a serviços externos sem autorização explícita.

