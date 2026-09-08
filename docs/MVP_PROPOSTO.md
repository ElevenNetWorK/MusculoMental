# MVP proposto — Músculo Mental

## Objetivo

Entregar uma primeira versão Android testável que permita ao usuário consultar uma biblioteca inicial de exercícios, executar um treino, registrar cada série e acompanhar o histórico. O recorte valida o uso diário antes de adicionar backend, pagamentos, turmas completas, smartwatch e inteligência artificial.

## Público inicial para teste

- Praticante que monta o próprio treino pelo Modo História.
- Aluno que executa uma ficha demonstrativa atribuída localmente.
- Professor representado inicialmente por dados de demonstração; o painel profissional completo fica para uma etapa posterior.

## Fluxos incluídos

### 1. Entrada e perfil local

- Escolha entre experiência de aluno e treino autônomo.
- Nome de exibição, objetivo, experiência e frequência semanal.
- Dados apenas locais nesta primeira entrega, claramente identificados como protótipo.

### 2. Biblioteca inicial

- Lista pequena, revisável e expansível de exercícios.
- Pesquisa e filtros por região muscular e equipamento.
- Detalhe com instruções, músculo principal, auxiliares, erros comuns e mídia demonstrativa local.
- Navegação exercício → músculos e músculo → exercícios.

Catálogo inicial sugerido: supino reto, desenvolvimento de ombros, elevação lateral, leg press, agachamento, rosca direta, rosca inversa e um exercício de tríceps. A precisão do conteúdo deve ser validada antes de publicação.

### 3. Treinos pessoais

- Criar treino com nome e lista ordenada de exercícios.
- Definir séries, repetições, carga sugerida e intervalo.
- Editar, duplicar e arquivar um treino.
- Identificação visível “Criado por você”.

### 4. Execução do treino

- Iniciar sessão e avançar pelos exercícios.
- Registrar cada série individualmente.
- Ação rápida para copiar carga e repetições da série anterior, exigindo confirmação da nova série.
- Mostrar planejado e executado separadamente.
- Permitir observação, esforço e desconforto como campos opcionais.
- Continuar uma sessão interrompida.

### 5. Cronômetro

- Início manual ou automático após concluir uma série.
- Contagem regressiva baseada no intervalo planejado.
- Pausar, continuar, acrescentar tempo, encerrar e corrigir.
- Aviso por som ou vibração configurável.
- Registro opcional do intervalo efetivamente realizado.

### 6. Histórico básico

- Lista e calendário simples de sessões concluídas.
- Detalhes do treino realizado.
- Evolução de carga por exercício.
- Frequência semanal e recordes básicos calculados localmente.

## Fora deste primeiro recorte

- Cadastro remoto e autenticação real.
- Painel completo do professor, convites e sincronização professor–aluno.
- Turmas, presença, mensagens e aulas coletivas.
- Administração e moderação da plataforma.
- IA para analisar treino.
- Smartwatch e plataformas de saúde.
- Fotos corporais e documentos de saúde.
- Pagamentos, assinaturas e anúncios.
- Site público, iPhone e versão web.
- Alimentação, calorias, comunidade e desafios.

## Critérios de conclusão

- O projeto compila em configuração limpa.
- O usuário consegue criar, executar, interromper, retomar e concluir um treino.
- Planejado e executado permanecem distintos no histórico.
- Os dados continuam disponíveis após fechar e reabrir o aplicativo.
- O cronômetro funciona durante a sessão e não perde silenciosamente o estado.
- Regras centrais têm testes automatizados.
- Não existem dados pessoais reais, chaves ou dependência de serviço remoto.

## Decisão de experiência proposta

Cada série deve ser confirmada individualmente, com um botão para copiar os valores anteriores. Assim o aplicativo reduz digitação sem presumir que uma série foi realizada.

