# Modelo de dados — Sistema de Gestão de Demandas de Condomínios

Este documento explica as decisões de modelagem por trás das entidades JPA em [`backend/src/main/java/com/condominiogestao/`](../backend/src/main/java/com/condominiogestao/) e da migration [`V1__init.sql`](../backend/src/main/resources/db/migration/V1__init.sql), e lista pontos que precisam de validação.

## Decisões já validadas com o cliente

- **Stack**: Frontend Next.js (React), Backend Java (Spring Boot) + Spring Data JPA/Hibernate, Postgres. (Ver nota sobre a mudança de Node/Prisma para Java logo abaixo.)
- **Etapas vs. Kanban são conceitos diferentes**:
  - `DemandaEtapa` = subtarefas internas de uma demanda (nome + prazo + concluída), definidas pelo funcionário. Ex: "Captura de orçamentos", "Reparo do serviço".
  - `Demanda.status_kanban` = status geral da demanda, visível no quadro Kanban.
- **Kanban com transição livre**: uma demanda pode pular etapas (ex: pintura de parede não passa por "Para votação em assembleia"). O funcionário movimenta livremente entre colunas, sem máquina de estados enforced no banco.
- **Colunas do Kanban são cadastradas por condomínio**: deixou de ser uma lista fixa (enum) igual para o sistema inteiro. Ver seção "Kanban por condomínio" abaixo.
- **Funcionário e morador podem atuar/residir em vários condomínios (N:N)**: `Funcionario` e `Morador` são papéis sobre uma `Pessoa` (identidade única, CPF único, login global) sem `id_condominio` fixo. O vínculo com cada condomínio fica em tabelas próprias — ver seção 1 abaixo.
- **`Pessoa` como identidade compartilhada**: nome/cpf/email/senha viraram uma tabela própria; `Funcionario` e `Morador` são extensões 1:1 dela (mesma PK). Ver seção dedicada abaixo.
- **Dois níveis de desativação, tanto para funcionário quanto para morador**: `Funcionario.situacao`/`Morador.situacao` desativam a pessoa naquele papel em **todos** os condomínios de uma vez (ex: desligamento da empresa administradora, ou morador que vendeu tudo e não é mais residente em lugar nenhum); `FuncionarioCondominio.situacao`/`MoradorCondominio.situacao` desativam **só naquele condomínio** (ex: saiu de um prédio mas continua em outro). Perfil (Síndico, Supervisor...) também é por vínculo — o mesmo funcionário pode ter perfis diferentes em condomínios diferentes.
- **Unidade vira campo simples, não tabela**: quem cadastra o morador é sempre um funcionário, que já sabe de cabeça o bloco e o número da unidade — não precisa de uma tabela `Unidade` separada para validar isso. `bloco` e `numero_unidade` ficam direto em `MoradorCondominio` (`id_bloco` opcional + `numero_unidade` texto livre).
- **`quantidade_casas` em `Condominio` é só informativo**: confirmado. Como não existe mais tabela `Unidade`, não há nada para "auto-gerar" a partir desse número — ele existe só como referência/config do condomínio.
- **Armazenamento de anexos**: confirmado — Cloudflare R2 em produção + MinIO local. Ver seção abaixo.
- **Quadro de avisos** (nova funcionalidade): comunicado geral do condomínio mantido por funcionários, pra reduzir demandas desnecessárias por algo que já é sabido. Ver seção dedicada abaixo.

## Backend migrado de Node/Prisma para Java/Spring Boot

O modelo de dados (entidades, relações, regras) não mudou — só a implementação técnica. Dois efeitos práticos dessa troca:

- **Migrations**: quem manda no schema agora é o Flyway (`backend/src/main/resources/db/migration/V1__init.sql`), não mais o Prisma. `V1__init.sql` é equivalente à migration Prisma antiga, com uma diferença deliberada:
- **Enums viraram `VARCHAR` + `CHECK`, não mais tipos `ENUM` nativos do Postgres**: Hibernate tem atrito conhecido com enums nativos do Postgres (exige anotação extra de baixo nível, `@JdbcTypeCode`). `VARCHAR(30) CHECK (coluna IN (...))` dá a mesma garantia de integridade no banco, com `@Enumerated(EnumType.STRING)` simples do lado Java. Os valores gravados continuam exatamente os mesmos (`ativo`, `inativo`, `sindico`, etc.) — os enums Java usam constantes em minúsculo (fora da convenção Java) só pra bater com esses valores sem precisar de um conversor por enum.
- **`ddl-auto: validate`**: o Hibernate nunca gera ou altera schema sozinho, só confere na subida da aplicação que as entidades batem com as tabelas reais. Mudança de schema = escrever uma nova migration SQL (`V2__...sql`) à mão.

## Pessoa como identidade compartilhada

Ideia do Romulo: em vez de `nome`/`cpf`/`email`/`senha_hash` duplicados em `Funcionario` e `Morador`, existe uma tabela `Pessoa` com esses campos, e `Funcionario`/`Morador` viram **extensões 1:1** dela — mesma chave primária (`id_funcionario`/`id_morador` É `id_pessoa`, não é FK solto com id próprio).

- **Não é herança de classe (Java) - é composição com PK compartilhada**: uma pessoa pode ter os dois papéis ao mesmo tempo (ex: síndico que também mora no condomínio) - herança clássica de ORM (`@Inheritance`) assume que cada linha da tabela-base corresponde a exatamente *um* subtipo, o que não vale aqui. `Funcionario`/`Morador` usam `@OneToOne` + `@MapsId` apontando pra `Pessoa`.
- **CPF agora é único de verdade**: antes, `funcionarios.cpf` e `moradores.cpf` eram unicidades *separadas* - nada impedia duas linhas com o mesmo CPF, uma em cada tabela, como duas identidades desconectadas. Com `Pessoa.cpf` único, isso vira uma identidade real: cadastrar um funcionário com um CPF que já é de um morador **reaproveita** a `Pessoa` existente (mesmo `id`) em vez de criar uma pessoa nova.
- **E-mail mora em `Pessoa`, obrigatório na aplicação pra Funcionario/Morador (não no banco)**: `Pessoa.email` continua opcional no schema (administrador pode não ter), mas os services de `Funcionario` e `Morador` agora exigem e-mail os dois - é o que identifica a pessoa no fluxo de "Esqueci minha senha" (ver seção de Autenticação) - e preenchem o da `Pessoa` se ela ainda não tiver um. Se a pessoa já tinha e-mail cadastrado (por outro papel), esse e-mail existente prevalece — o service não sobrescreve silenciosamente.
- `Funcionario`/`Morador` têm getters de atalho (`getNome()`, `getCpf()`, `getEmail()`) que só repassam pra `Pessoa` - o resto do código não precisa saber que o dado mora "num lugar diferente".

## Como as pessoas se ligam ao condomínio

`Funcionario` e `Morador` (papéis de uma `Pessoa`, CPF único — mesmo login em qualquer condomínio onde atuem/residam). O vínculo com cada condomínio vive em tabela própria:

- **`FuncionarioCondominio`**: liga `Funcionario` a `Condominio`, com `perfil` e `situacao` próprios daquele vínculo. Um funcionário pode ser Síndico no Condomínio A e Encarregado no B; pode estar ativo em um e inativo em outro.
- **`MoradorCondominio`**: liga `Morador` a `Condominio`, com `id_bloco` (opcional, só apartamento), `numero_unidade` (texto) e `situacao` próprios daquele vínculo. Cobre tanto "mesmo morador com unidades em condomínios diferentes" quanto "mais de um morador na mesma unidade" (ex: cônjuge, filhos) — cada um com sua própria linha.

**Regra de negócio importante para o backend**: ao aprovar/atribuir/solicitar uma demanda, validar que o funcionário/morador tem um vínculo **ativo** com o `id_condominio` daquela demanda — o banco não impede um funcionário de São Paulo ser marcado como responsável por uma demanda de um condomínio no Rio, isso é responsabilidade da aplicação.

## Kanban por condomínio

O quadro deixou de ter uma lista fixa de status (antes era um `enum` só, igual pra todo o sistema) e virou cadastro:

- **`StatusKanban`**: uma coluna do quadro, pertencente a um condomínio (`id_condominio`), com `nome` livre e `ordem` (posição de exibição). Cada condomínio cadastra as suas próprias colunas — um pode ter "Fila / Em andamento / Finalizada" e outro pode ter um fluxo mais detalhado, sem afetar os demais.
  - `visivel_externamente` (`BOOLEAN NOT NULL DEFAULT true`, V7): quando `false`, a coluna e as demandas nela somem do Kanban do morador (funcionário continua vendo).
  - `finalistico` (`BOOLEAN NOT NULL DEFAULT false`, V9): marca a coluna como situação terminal do fluxo (ex: "Finalizada", "Cancelada"). Só demanda numa coluna finalística pode ser **arquivada** — ver `Demanda.arquivada`.
- **`Demanda.id_status_kanban`**: aponta para uma linha de `StatusKanban`. Continua opcional (`nullable`) — só é preenchido depois que a demanda é aprovada e entra na fila.
- **`Demanda.arquivada`** (`BOOLEAN NOT NULL DEFAULT false`, V9): funcionário arquiva a demanda pelo card do Kanban quando ela está numa coluna finalística. Não apaga nada — só tira do fluxo ativo (mesma filosofia de soft-delete do resto do sistema).
- **`DemandaStatusKanbanHistorico`**: uma linha por transição de coluna, guardando `id_status_anterior` (nullable, a primeira transição não tem "anterior"), `id_status_novo`, **quem alterou** (`id_funcionario`) e **quando** (`created_at`). É o rastro de auditoria do Kanban.

**Sugestão de implementação** (não é decisão de schema, mas afeta o backend): ao cadastrar um condomínio novo, popular automaticamente as 7 colunas padrão que estavam no enum antigo (`Fila`, `Em andamento`, `Para análise das comissões`, `Para votação em assembleia`, `Demanda iniciada`, `Demanda pausada`, `Finalizada`) como `StatusKanban` daquele condomínio — assim o síndico já começa com um fluxo pronto, e edita/renomeia/reordena/adiciona colunas depois se quiser.

**Nota**: a aprovação/reprovação de demanda (item 4.2/4.3) **não** ganhou uma tabela de histórico própria — ela já é uma transição única (pendente → aprovada/reprovada), totalmente coberta pelos campos que já existem em `Demanda` (`status_aprovacao`, `data_aprovacao`, `justificativa_reprovacao`, `id_funcionario_aprovador`). O histórico dedicado faz sentido pro Kanban porque ali a mesma demanda passa por várias transições ao longo do tempo.

## Etiquetas

Nova funcionalidade: etiquetar demandas para classificação livre, à parte do Kanban.

- **`Etiqueta`**: cadastrada por condomínio (mesmo padrão de `StatusKanban`), com `descricao` (texto, **máx. 50 caracteres** — usei `@db.VarChar(50)`, então o próprio Postgres rejeita string maior, não é só validação de aplicação), `cor` (string livre guardando um hex, ex: `#2F80ED` — o formato em si precisa ser validado na aplicação, o banco não valida regex) e `situacao` (`ativo`/`inativo`, reaproveitando o enum `Situacao` já usado em outras entidades).
- **`DemandaEtiqueta`**: tabela de junção N:N — uma demanda pode ter várias etiquetas, e a mesma etiqueta pode estar em várias demandas. Chave primária composta (`id_demanda`, `id_etiqueta`), sem `id` próprio, por ser uma junção pura sem atributos além da data de vínculo.
- Nome duplicado dentro do mesmo condomínio é bloqueado por `@@unique([id_condominio, descricao])` — mas nada impede duas etiquetas com a mesma cor.
- Mesma regra de fronteira dos outros cadastros por condomínio: a aplicação precisa impedir que uma demanda receba uma etiqueta de outro condomínio (o banco não garante isso sozinho — ver seção de regras abaixo).
- **Etiquetas `inativo` continuam vinculadas às demandas que já as tinham** (a inativação não desfaz `DemandaEtiqueta`) — só devem parar de aparecer como opção para *adicionar* em novas demandas. Se quiser um comportamento diferente (remover de demandas antigas também), me avisa.

## Quadro de avisos

Ideia do Romulo: comunicado geral do condomínio, mantido por funcionários (ex: "Piscina interditada devido a vazamentos"), pra reduzir demandas repetidas por algo que já é de conhecimento geral — a pessoa vê o aviso ao entrar no sistema, em vez de abrir uma demanda.

- **`Aviso`**: `descricao` (**máx. 250 caracteres** — `VARCHAR(250)`, enforced no banco), `id_condominio`, `id_funcionario` (autor — adicionei além do que foi pedido, pra saber quem postou), `situacao` (ativo/inativo) e `data_expiracao` (**opcional** — confirmado; sem prazo definido, o aviso fica visível até alguém desativar manualmente).
- **Por que `situacao` além de `data_expiracao`**: cobre o caso de resolver o problema antes do prazo (piscina liberada no dia 2, mas a expiração cadastrada era pro dia 5) — sem isso, não teria como tirar o aviso da tela a não ser editando a data.
- **"Visível" = `situacao = ativo` E (`data_expiracao` nula OU no futuro)** — é essa combinação que o endpoint principal (`GET /api/avisos`) filtra.
- **Não é aberto pra qualquer cadastro**: os dados (`condominioId`, `id_funcionario` do autor) vêm do **contexto do login** (token), não de campos que o cliente preenche — o próprio sistema já sabe em qual condomínio e como qual funcionário a pessoa está agindo. Só quem está autenticado como funcionário pode criar/desativar; qualquer pessoa autenticada (funcionário ou morador) pode listar os avisos visíveis do condomínio do seu contexto atual.
- **Desenhado no diagrama ER** ([Planta do Banco](https://claude.ai/code/artifact/111904a7-c3bb-4cdf-bfdb-02c17176677e)), entre a coluna de vínculos e a de pessoas — o Romulo pediu pra manter o diagrama sempre sincronizado com o schema, então a partir de agora toda tabela nova entra nele, mesmo as mais simples (revertendo o critério usado antes pra `DemandaEtiqueta`, que ainda não foi desenhada por ser uma junção pura sem atributos).

## Tarefas agendadas

Pedido do Romulo: qualquer funcionário do condomínio registra um lembrete com data (ex: "renovar seguro do elevador"). Separado de `Demanda` (que é pedido de morador/funcionário com fluxo de aprovação/Kanban) e de `DemandaEtapa` (checklist dentro de uma demanda) — aqui é só um lembrete solto, do condomínio.

- **`TarefaAgendada`**: `id_condominio`, `id_funcionario` (autor), `titulo` (TEXT), `descricao` (TEXT) e **três datas puras** (`DATE`, sem hora): `data_tarefa` (a data da tarefa em si), `data_primeiro_aviso`, `data_segundo_aviso`. Todas obrigatórias; **sem** constraint de ordem entre elas (dá pra registrar uma tarefa cujo primeiro aviso já passou).
- **Datas puras de propósito**: o que importa é o dia. Um "sininho" no menu do app fica vermelho quando **qualquer** uma das três datas de **alguma** tarefa do condomínio é **hoje** — a comparação de "hoje" é feita no cliente (fuso do usuário).
- **Ordenação da listagem**: pela **menor das três datas** (`proximaDataRelevante`, método na entidade — não é coluna), crescente. O que vence primeiro (ou já venceu) fica no topo, casando com o alerta do sino.
- **Escopo atual**: criar + listar. Sem editar/concluir/remover ainda (não foi pedido).
- **Contexto, não corpo**: `condominioId` e autor vêm do token. Só funcionário cria/lista (morador não usa; administrador não tem condomínio próprio) — diferente de `Aviso`, não há visão de administrador gerenciando outro condomínio, então o `condominioId` nunca vem de fora.

## Armazenamento de anexos (item 4.9) — confirmado

`DemandaDocumento` guarda só metadados + `url`; o arquivo em si (foto, PDF) fica fora do Postgres.

**Decisão**: **Cloudflare R2** em produção + **MinIO** (compatível com S3) rodando local via `docker-compose` em desenvolvimento.
- R2 usa a mesma API do S3, mas **sem cobrar egress** — relevante aqui porque fotos/PDFs de demanda são reabertos várias vezes por síndico e moradores (visualização repetida custaria caro num bucket S3 tradicional).
- Como MinIO fala o mesmo protocolo S3, o código de upload/download é **um só client**, sem `if (ambiente === 'local') { ... } else { ... }` — dev e produção rodam o mesmo caminho de código, só troca endpoint/credenciais via variável de ambiente.
- Migrar de R2 para S3 (ou vice-versa) no futuro, se precisar, é só trocar endpoint — não exige reescrever nada.

## Notas técnicas de implementação (não exigem validação, só registrando o porquê)

- **Senha**: `senha_hash` mora em `Pessoa` agora (era em `Funcionario`/`Morador`) — nunca texto puro. bcrypt ou argon2 na camada de aplicação. `precisa_trocar_senha` (boolean, nasce `true`) acompanha - toda `Pessoa` nova já nasce com senha padrão + essa flag ligada, e só o fluxo "Esqueci minha senha" desliga (ver seção de Autenticação).
- **Demanda sigilosa** (item 4.8): além de quem criou a demanda, `DemandaAcessoSigiloso` guarda uma lista explícita de funcionários/moradores autorizados a ver uma demanda marcada como sigilosa. Regra de negócio na aplicação: se `sigilosa = true`, só o solicitante, o responsável atual, e quem estiver nessa tabela podem visualizar.
- **Regras que o Postgres/Hibernate não garantem sozinhos** (validar no service, além do que já é `CHECK` na migration):
  - Em `Demanda`: exatamente um entre `id_morador_solicitante` / `id_funcionario_solicitante` deve estar preenchido (nunca os dois, nunca nenhum) — **já é `CHECK` no banco** (`chk_demanda_solicitante_unico` em `V1__init.sql`), mas vale validar no service também pra dar um erro 400 claro em vez de deixar estourar a constraint como erro 500.
  - Em `Demanda`: o funcionário/morador solicitante, aprovador e responsável precisam ter vínculo ativo (`FuncionarioCondominio`/`MoradorCondominio`) com o `id_condominio` da própria demanda.
  - Em `Demanda`/`DemandaStatusKanbanHistorico`: o `StatusKanban` referenciado precisa pertencer ao mesmo `id_condominio` da demanda — o banco não impede escolher uma coluna de Kanban de outro condomínio.
  - Em `DemandaEtiqueta`: a `Etiqueta` vinculada precisa pertencer ao mesmo `id_condominio` da demanda — mesmo tipo de regra do `StatusKanban`.
  - Um funcionário só consegue de fato logar e agir num condomínio se: `Funcionario.situacao = ativo` **e** existir um `FuncionarioCondominio` daquele condomínio com `perfil != null` e `situacao = ativo`. `senha_hash` preenchido sem nenhum vínculo com perfil é um estado "órfão" que vale validar no cadastro.

## Autenticação: login por CPF/senha + escolha de contexto

Requisito do Romulo: como uma `Pessoa` pode ter vários vínculos (`FuncionarioCondominio`/`MoradorCondominio` em vários condomínios, e até os dois papéis no *mesmo* condomínio — ex: síndico que também mora lá), o login precisa perguntar **em qual condomínio e como papel** a pessoa quer entrar, quando há mais de uma opção.

**Não precisou de tabela nova** — `FuncionarioCondominio`/`MoradorCondominio` já são exatamente essas combinações escolhíveis. O trabalho foi todo no fluxo de login:

1. `POST /api/auth/login` (cpf + senha) → autentica a `Pessoa` e monta a lista de **contextos ativos** (uma entrada por vínculo `ativo`, cruzando com a situação **global** do papel — `Funcionario.situacao`/`Morador.situacao` — e, no caso de funcionário, só entram vínculos com `perfil != null`).
   - **1 contexto só** → já devolve o token completo (`token`), sem passo extra.
   - **Mais de 1** → devolve a lista de contextos + um `preAuthToken` de vida curta (5 min) que só prova que a senha bateu, mas não dá acesso a nada ainda.
2. `POST /api/auth/contexto` (com o `preAuthToken` no header `Authorization`) + a escolha (`condominioId` + `tipoPapel`) → confere que a pessoa **realmente tem** esse vínculo ativo (não é só "confiar" no que o front mandou) e devolve o token completo.
3. O token completo carrega `pessoaId`, `nome`, `condominioId`, `tipoPapel` (`funcionario`/`morador`) e `perfil` (só quando funcionário) — é isso que todo endpoint protegido usa pra saber "como quem" a requisição está agindo.

**Senha (revisado)**: toda `Pessoa` nova (funcionário/morador/administrador) nasce com uma senha padrão conhecida (`auth.senha-padrao`, hoje `Trocar@123`) e a flag `Pessoa.precisaTrocarSenha = true` — essa senha nunca serve pra logar de verdade, `POST /api/auth/login` barra enquanto a flag estiver ligada (mesmo que a senha digitada bata certinho com a padrão). O único jeito de entrar é o fluxo "Esqueci minha senha": `POST /api/auth/verificar-identidade` (CPF + e-mail, confirma quem é a pessoa) seguido de `POST /api/auth/trocar-senha` (CPF + e-mail + senha atual + nova senha, desliga a flag). Por isso e-mail agora é **obrigatório também pro cadastro de funcionário** (já era pro de morador) — é o que identifica a pessoa nesse fluxo. Substitui o antigo `POST /api/auth/definir-senha` (nunca chegou a ser usado pelo frontend, removido). ⚠️ Mesma ressalva de antes, só que mais branda: CPF + e-mail digitados não é prova real de posse do e-mail (sem link único enviado por e-mail) - mais forte que só CPF, mas ainda não é definitivo. Pendência real que ficou de fora: `Pessoa` já existente sem e-mail cadastrado fica sem caminho pra esse fluxo até alguém preencher (não existe tela de "editar pessoa" ainda).

**`Aviso` é o primeiro endpoint que usa o contexto de verdade** (ver `AvisoController`/`AvisoService`): não recebe `condominioId` nem `funcionarioId` no corpo da requisição — os dois vêm do `ContextoAutenticado` extraído do token. Isso é o padrão que os próximos endpoints (`Demanda` e o resto) deveriam seguir, em vez de continuar pedindo esses IDs no corpo como os 8 endpoints mais antigos (`Condominio`, `Bloco`, etc.) ainda fazem — aqueles foram construídos antes de existir autenticação.

**Implementação**: Spring Security (stateless, sem sessão/cookie) + JWT (`io.jsonwebtoken`, HS256) + BCrypt pra hash de senha. Todo endpoint sob `/api/**` agora exige um token completo válido, exceto `/api/auth/**` e o Swagger — ou seja, **os 8 endpoints de cadastro que já existiam passam a pedir login** a partir de agora (isso já estava sinalizado como pendência: "hoje os endpoints não têm nenhuma proteção"). Pra testar pelo Swagger: fazer login, copiar o `token`, clicar em "Authorize" (canto superior direito) e colar.

CORS já liberado pra `http://localhost:3000` (Next.js em dev) — ajustar quando existir domínio de produção.

## Administrador: papel global, sem condomínio

Até aqui todo papel de `Pessoa` era escopado a um condomínio (síndico etc., via `FuncionarioCondominio`) — não existia um jeito de criar o **primeiro** condomínio, já que todo endpoint de cadastro exige login e login exige um vínculo ativo em algum condomínio já existente. O Romulo pediu pra logar "como administrador" e cadastrar condomínios; perguntado se isso devia ser só o perfil síndico reaproveitado ou um papel de verdade, ele escolheu criar um papel novo.

`Administrador` é um quarto papel de `Pessoa` (mesmo padrão de `Funcionario`/`Morador`: extensão 1:1, `@MapsId`, tabela própria `administradores`), mas **sem** uma tabela `administradores_condominios` — de propósito: é global, não pertence a nenhum condomínio específico. `TipoPessoa` ganhou o valor `administrador`; o contexto de login correspondente tem `condominioId = null` (por isso `ContextoDto`/`SelecionarContextoRequest`/`ContextoAutenticado` tiveram que aceitar isso, e `JwtService` passou a omitir o claim `condominioId` em vez de gravar `null`).

Duas regras de autorização reais nasceram junto (as primeiras do projeto além de "está logado?"):
- `POST /api/condominios` exige token de administrador.
- `POST /api/administradores` também — só quem já é administrador cria outro.

**Bootstrap**: o primeiro administrador não passa pela API (não existe autocadastro, senão qualquer autenticado viraria admin) — é um `INSERT` direto no banco em `pessoas` + `administradores` (exemplo no README). Foi assim que o Romulo entrou.

**CPF sem normalização era um bug latente**: o Romulo tinha inserido sua própria `Pessoa` via SQL com o CPF formatado (`043.539.215-84`), mas a tela de login manda só dígitos. Corrigido com um utilitário `Cpf.normalizar()` aplicado em todo ponto de entrada (login, senha, cadastro de funcionário/morador/administrador) + um `@PrePersist`/`@PreUpdate` na entidade `Pessoa` como rede de segurança.

## Pontos ainda em aberto (não bloqueiam a criação do schema, mas vão afetar o backend)

- **Notificações por e-mail**: em quais eventos exatamente? Confirmado: reprovação de demanda. Sugestão minha: também ao aprovar, ao mudar de status no Kanban, e ao atribuir responsável. Você decide o escopo do MVP.
- **Fluxo de troca de senha por e-mail de verdade**: resolvido com senha padrão + `precisaTrocarSenha` + "Esqueci minha senha" (CPF+e-mail) - mais forte que o `/api/auth/definir-senha` antigo, mas ainda não é um link único enviado por e-mail (mesma pendência de notificação por e-mail acima). Vale trocar por isso antes de produção de verdade.
- **Categoria/prioridade de demanda**: com `Etiqueta` agora cadastrada, isso já cobre boa parte da necessidade de categorização livre (ex: etiquetas "Urgente", "Financeiro", "Manutenção"). Ainda pode fazer sentido um campo `prioridade` fixo (ex: baixa/média/alta) separado das etiquetas, se quiser ordenar/filtrar por urgência de forma estruturada — mas isso é outra decisão, não bloqueante agora.
- **LGPD**: CPF e e-mail são dados pessoais. Vale (fora do escopo do schema, mas registrando): política de retenção quando morador/funcionário fica `inativo`, e log de acesso a dados sensíveis.

## Próximos passos sugeridos

1. Resetar o banco local (`docker compose down -v` + `docker compose up -d`) e rodar `mvn spring-boot:run` de novo — o schema mudou (tabela `Pessoa`, autenticação, e agora `Aviso`) desde o último teste de ponta a ponta.
2. Fazer o cluster de `Demanda` (repository → DTO → service → controller): `Demanda`, `DemandaEtapa`, `DemandaDocumento`, `DemandaStatusKanbanHistorico`, `DemandaEtiqueta`, `DemandaAcessoSigiloso` — seguindo o padrão do `Aviso` (dados do contexto, não do corpo da requisição).
3. Autorização por perfil (ex: só Síndico aprova demanda) — a autenticação já entrega "quem" e "com qual perfil", falta usar isso pra travar endpoint por endpoint.
4. Revisitar os 8 endpoints mais antigos (`Condominio`, `Bloco`, `StatusKanban`, `Etiqueta`, `Funcionario`, `FuncionarioCondominio`, `Morador`, `MoradorCondominio`) pra usar o contexto de autenticação onde fizer sentido, em vez de receber `condominioId` solto no corpo.
