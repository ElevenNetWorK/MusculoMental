# Arquitetura Android proposta

## Objetivo técnico

Começar com um aplicativo Android local, confiável e simples de testar, mantendo caminhos claros para sincronização remota posterior. A arquitetura não deve antecipar a complexidade de toda a plataforma.

## Base recomendada

- Kotlin e Jetpack Compose.
- Material 3.
- Gradle Kotlin DSL e catálogo de versões.
- ViewModel, StateFlow, Coroutines e Navigation Compose.
- Room para banco local.
- DataStore para preferências.
- WorkManager apenas quando existir trabalho persistente que realmente o exija.
- Testes unitários de domínio e testes de persistência/navegação nos fluxos críticos.

As versões das dependências devem ser escolhidas pelo Codex a partir das versões estáveis compatíveis disponíveis no momento da criação, registrando a escolha no projeto.

## Estrutura inicial sugerida

```text
app/
  src/main/java/.../
    core/
      database/
      designsystem/
      navigation/
      common/
    domain/
      model/
      repository/
      usecase/
    data/
      local/
      repository/
    feature/
      onboarding/
      library/
      workoutbuilder/
      workoutsession/
      timer/
      history/
      settings/
```

Começar com um único módulo `app` organizado por pacotes. Separar módulos Gradle somente quando o tamanho ou o tempo de compilação justificar.

## Entidades mínimas

- `UserProfile`
- `MuscleRegion`
- `Muscle`
- `Exercise`
- `ExerciseMuscleRelation`
- `WorkoutPlan`
- `WorkoutPlanExercise`
- `PlannedSet`
- `WorkoutSession`
- `PerformedSet`
- `RestInterval`

Cada execução deve guardar uma fotografia dos valores planejados relevantes. Mudanças futuras no treino não podem reescrever o histórico realizado.

## Estados importantes

- Plano: rascunho, ativo e arquivado no MVP local.
- Sessão: não iniciada, em andamento, pausada, concluída ou cancelada.
- Série: planejada, concluída ou ignorada.
- Cronômetro: parado, executando, pausado e finalizado.

## Estratégia de dados

1. Fonte de verdade inicial: Room.
2. Repositórios escondem detalhes do banco das telas.
3. IDs locais estáveis, preferencialmente UUID.
4. Datas armazenadas de forma inequívoca e exibidas no fuso do usuário.
5. Migrações do banco não devem apagar histórico.
6. Dados demonstrativos entram por mecanismo separado dos dados reais.

## Caminho futuro para backend

Autenticação, sincronização, permissões professor–aluno, mídia e notificações exigirão uma decisão específica de backend. Antes disso, definir:

- modelo de consentimento e revogação;
- autorização por registro, não apenas por tela;
- política de retenção e exclusão;
- versionamento e resolução de conflitos;
- armazenamento protegido de fotos e dados sensíveis;
- auditoria de alterações profissionais.

Não acoplar as telas diretamente a um fornecedor remoto na fase local.

## Ordem recomendada de implementação

1. Criar esqueleto compilável, tema e navegação.
2. Modelar domínio e banco local com testes.
3. Implementar biblioteca inicial.
4. Implementar construtor de treino pessoal.
5. Implementar sessão e registro por série.
6. Implementar cronômetro e retomada.
7. Implementar histórico e evolução básica.
8. Revisar acessibilidade, falhas, testes e documentação.

Cada etapa deve terminar compilando e com um fluxo demonstrável.

