# Validação da etapa 1

Data: 07/09/2026.

## Resultado

- APK de desenvolvimento gerado, instalado e aberto em um Samsung SM-A305GT com Android 11.
- Identificador de desenvolvimento: `com.musculomental.app.dev`.
- SDK mínimo 26, SDK de destino 37 e versão 0.1.0 confirmados no APK.
- Cinco testes unitários aprovados, sem falhas.
- Dois testes instrumentados aprovados no aparelho, sem falhas, em 10,513 segundos.
- Lint concluído com zero erros. O relatório inicial continha dois avisos: atualização opcional do Gradle e ausência de ícone. O ícone provisório foi adicionado depois desse relatório.

## Cobertura verificada

Os testes unitários verificam campos obrigatórios, limites de texto e frequência, bloqueio de gravação inválida, recuperação após falhas de leitura e escrita e preservação do formulário durante recriação.

No aparelho foram verificados:

1. criação, consulta e edição do perfil;
2. cancelamento de edição sem gravar o rascunho;
3. persistência de todos os campos após fechar e reabrir o banco;
4. atualização do perfil e nova reabertura do banco.

A inicialização real do aplicativo também foi inspecionada visualmente no aparelho. A tela inicial exibiu o estado sem perfil, o aviso de armazenamento local e a ação para criação com texto legível.

## Observações do ambiente

O Windows bloqueou arquivos de lock do wrapper, da chave debug e do diretório temporário padrão. Para validar o projeto, o Gradle foi executado diretamente, uma chave de desenvolvimento temporária foi usada por meio de `-PdevelopmentKeystore` e os dois APKs foram instalados pelo ADB. A chave ficou fora do projeto.

Uma repetição posterior com Gradle 9.7.1 não foi concluída porque essa versão solicitou variantes adicionais do plugin Kotlin que não estavam no cache e a rede do processo estava bloqueada. Isso ocorreu depois da geração e instalação do APK, da aprovação dos cinco testes unitários, do lint e da aprovação dos dois testes instrumentados.

## Limitações esperadas

- Os dados são locais e são apagados ao desinstalar o aplicativo ou limpar seus dados.
- Não existe conta online, professor real, biblioteca, treino ou histórico nesta etapa.
- O tema e o ícone são provisórios; a identidade visual continua pendente.
- A variante de publicação não possui configuração de assinatura.

## Próximo passo

Aguardar aprovação antes de iniciar a etapa 2, biblioteca inicial. Antes de concluí-la, definir a origem e os direitos de uso das mídias e o processo de revisão anatômica.
