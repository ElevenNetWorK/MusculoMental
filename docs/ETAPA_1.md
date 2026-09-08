# Etapa 1 — base Android e perfil local

## Autorização e escopo

O responsável aprovou começar pelo plano apresentado e implementar somente a primeira etapa. O protótipo local não substitui o escopo profissional e coletivo do planejamento completo.

Entregue nesta etapa: criar, consultar e editar nome de exibição, objetivo, experiência, frequência semanal disponível e modo de uso. O modo aluno é demonstrativo e não cria professor, vínculo ou prescrição. Não há telas de biblioteca, treino ou histórico vazias.

O perfil fica em Room, com uma única linha local. A interface oferece carregamento, ausência de perfil, falha com nova tentativa, validação, salvamento e retorno ao resumo. Cancelar a edição não grava o rascunho. SavedStateHandle mantém o formulário durante recriação pelo sistema. Fechar ou desinstalar o aplicativo são operações diferentes: desinstalar ou limpar seus dados apaga o perfil.

## Decisões técnicas

- Um módulo `app`; domínio e contrato do repositório independentes da interface e de Room.
- ViewModel, StateFlow, Coroutines e Navigation Compose; injeção manual por construtor.
- Room 2.8.4 com schema exportado, sem migração destrutiva. KSP gera o acesso ao banco.
- Material 3 claro/escuro seguindo o sistema, com cores padrão provisórias. A identidade visual continua pendente.
- minSdk 26; compileSdk/targetSdk 37; pacote base provisório `com.musculomental.app`. Debug usa `com.musculomental.app.dev`.
- Sem permissão de internet e sem backup automático do perfil. Não são coletados dados de saúde.
- DataStore foi adiado porque ainda não existe preferência independente do perfil. Hilt, WorkManager, mídia e serviços remotos não são necessários nesta etapa.
- AGP 9.3.2 e Gradle 9.5.0 substituem os candidatos do plano para usar a cadeia instalada e a correção estável de lint do AGP. As versões efetivamente usadas estão nos arquivos `build.gradle.kts` da raiz e de `app`. O catálogo de referência foi preservado em `docs/DEPENDENCIAS_REFERENCIA.toml`; ele não é consumido pelo build devido à falha local de geração dos acessores Java do Gradle. O compilador Kotlin executa no processo do Gradle para evitar a pasta externa do daemon.
- O objetivo é texto livre, obrigatório, de até 120 caracteres; nome de exibição de até 60; disponibilidade entre 1 e 7 dias. Não há recomendação automática de frequência.

## Abrir e compilar

Abra esta pasta no Android Studio. Configure um JDK compatível e instale a plataforma Android API 37. Configure `ANDROID_HOME` ou `sdk.dir` no arquivo local `local.properties` (ignorado pelo Git).

No Windows, com JAVA_HOME e SDK configurados:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

O ambiente desta validação usa JBR 21, com bytecode Java 17. Se o gerenciador automático da chave debug falhar ao bloquear `debug.keystore.lock`, o build aceita `-PdevelopmentKeystore=CAMINHO_ABSOLUTO_DA_CHAVE_DE_TESTE`. Essa opção só altera a variante debug e espera uma chave de desenvolvimento com alias e senhas padrão do Android. A chave não faz parte do projeto. Não use essa opção para chaves de publicação.

O segundo comando exige emulador ou aparelho conectado. O APK de desenvolvimento é gerado em `app/build/outputs/apk/debug/`.

## Verificação planejada

- Testes unitários: campos obrigatórios e limites, nenhuma gravação inválida, falha de leitura e nova tentativa, falha ao salvar sem perder rascunho e restauração do formulário.
- Teste de persistência em banco separado: salvar todos os campos, fechar/reabrir, editar e reabrir novamente.
- Teste de interface em banco isolado: criar, consultar, editar e cancelar sem alterar o perfil salvo.
- Compilação e lint; verificação visual e reabertura no aparelho quando disponível.

Os resultados efetivos estão registrados em `../VALIDACAO_ETAPA_1.md`.

## Próximo passo

Apresentar a etapa 1 ao responsável e aguardar autorização para a biblioteca inicial. Mídias e revisão anatômica precisam de definição antes de concluir essa próxima entrega.


