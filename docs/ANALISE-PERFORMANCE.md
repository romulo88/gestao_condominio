# Análise de performance — condominio-gestao

Revisão estática de `backend/` (Spring Boot 3 + JPA/Hibernate + PostgreSQL + MinIO) e `frontend/` (Next.js App Router). 177 arquivos Java, 36 arquivos de frontend, 16 migrations Flyway. Cada achado abaixo foi confirmado abrindo o arquivo citado.

## Resumo

O sistema não tem nenhum gargalo de algoritmo — o problema está em **quantidade de round-trips**: ao banco (índices ausentes + lazy load em cadeia) e à API (o frontend faz uma requisição por item de lista). Hoje, com pouco volume, isso não aparece; a partir de algumas centenas de demandas e algumas dezenas de moradores por condomínio, aparece de uma vez.

Coisas que já estão certas e vale registrar para não mexer: `open-in-view: false`, todos os `@ManyToOne`/`@OneToOne` em `LAZY`, `@Transactional(readOnly = true)` consistente, upload/download em streaming (sem `byte[]` inteiro em memória), `MinioClient` como bean singleton, `DemandaService.listar` já carrega etiquetas/anexos/notas/responsáveis em lote, e o `package.json` do frontend está enxuto (sem dependência pesada).

**Os 6 itens que valem o esforço, em ordem:**

| # | Onde | Ganho | Esforço |
|---|------|-------|---------|
| 1 | Índices ausentes no Postgres | Alto | ~30 min |
| 2 | `default_batch_fetch_size` no Hibernate | Alto | 1 linha |
| 3 | Fan-out de requisições em `/demandas` | Alto | Médio |
| 4 | N+1 de rede no roster / abas de condomínio | Alto | Médio |
| 5 | I/O do MinIO dentro de `@Transactional` | Médio-alto (risco de travar tudo) | Baixo |
| 6 | Kanban sem memoização | Médio (UX) | Médio |

---

## 1. Banco: 4 índices em 16 migrations — as tabelas quentes não têm nenhum

**Severidade: crítica.** As migrations criam índice apenas em `demanda_notas`, `demanda_acompanhamentos`, `mensagens_rapidas` e `tarefas_agendadas`. O PostgreSQL **não cria índice automático para chave estrangeira** — só para PK e UNIQUE. Ou seja, `demandas` (a tabela central, `V1__init.sql:146-167`) hoje só tem o índice da PK, e toda consulta do Kanban é seq scan.

Consultas afetadas, todas em caminho quente:

- `DemandaRepository.findByCondominioId` — roda a cada carregamento do Kanban e da lista de demandas
- `DemandaRepository.findByMoradorSolicitanteIdAndDataAprovacaoAfter` — roda a cada login de morador
- `DemandaEtapaRepository.findByDemandaIdIn` / `DemandaDocumentoRepository.findByDemandaIdIn` — rodam em toda listagem
- `MoradorCondominioRepository.countAtivosPorCondominio` / `existsByMoradorIdAndCondominioId` — caminho de autenticação

Migration criada em `backend/src/main/resources/db/migration/V17__indices_performance.sql` — 9 índices, apenas onde alguma consulta real precisa e nenhum UNIQUE/PK já cobre:

```sql
CREATE INDEX idx_demandas_condominio_created_at        ON demandas (id_condominio, created_at DESC);
CREATE INDEX idx_demandas_morador_data_aprovacao       ON demandas (id_morador_solicitante, data_aprovacao);
CREATE INDEX idx_demanda_etapas_demanda_ordem          ON demanda_etapas (id_demanda, ordem);
CREATE INDEX idx_demanda_documentos_demanda            ON demanda_documentos (id_demanda);
CREATE INDEX idx_demanda_hist_kanban_demanda_created_at ON demanda_status_kanban_historico (id_demanda, created_at);
CREATE INDEX idx_moradores_condominios_morador_condominio ON moradores_condominios (id_morador, id_condominio);
CREATE INDEX idx_moradores_condominios_condominio      ON moradores_condominios (id_condominio);
CREATE INDEX idx_funcionarios_condominios_condominio   ON funcionarios_condominios (id_condominio);
CREATE INDEX idx_avisos_condominio_created_at          ON avisos (id_condominio, created_at DESC);
```

Ficaram **deliberadamente de fora**, porque o prefixo de um UNIQUE/PK existente já resolve — índice redundante custa escrita e espaço sem devolver nada:

| Tabela | Já coberta por |
|---|---|
| `demanda_responsaveis` | `UNIQUE (id_demanda, id_funcionario)` |
| `demanda_etiquetas` | `PRIMARY KEY (id_demanda, id_etiqueta)` |
| `demanda_acesso_sigiloso` | `UNIQUE (id_demanda, tipo_pessoa, ...)` |
| `demanda_acompanhamentos` | `UNIQUE (id_demanda, id_morador)` + índice do V16 |
| `demanda_notas` | `idx_demanda_notas_demanda` (V14) |
| `blocos`, `status_kanban`, `etiquetas` | `UNIQUE (id_condominio, nome\|descricao)` |
| `funcionarios_condominios` (por funcionário) | `UNIQUE (id_funcionario, id_condominio)` |

Também ficaram de fora as FKs de `demandas` para funcionário (`solicitante`, `aprovador`, `responsavel`, `marcou_sigilo`) e `id_status_kanban`: nenhuma consulta do sistema filtra por elas hoje — só serviriam para acelerar a checagem de integridade ao excluir um funcionário ou uma coluna do Kanban, o que praticamente não acontece. Se um dia surgir uma tela "demandas atribuídas a mim", aí `id_funcionario_responsavel` passa a valer.

Esta migration é **independente de todo o resto do relatório**: índice não muda contrato de API nem comportamento do código, o planner do Postgres passa a usá-lo sozinho, e reverter é um `DROP INDEX`. Pode rodar sozinha.

## 2. N+1 em cadeia: `Funcionario`/`Morador` → `Pessoa`

**Severidade: crítica. Correção de 1 linha.**

`Morador.getNome()` (`Morador.java:61-63`) e `Funcionario.getNome()` (`Funcionario.java:61-63`) delegam para `pessoa.getNome()`, e `pessoa` é `@OneToOne(LAZY)`. Como `DemandaResponse.from()` (`DemandaResponse.java:104-120`) é chamado dentro de um `.map()` por linha e acessa solicitante, responsável, aprovador e status, cada demanda dispara uma cadeia de SELECTs — e não há `@BatchSize` nem `default_batch_fetch_size` configurado, então o Hibernate resolve um por um.

O mesmo padrão está em `AvisoResponse.from()` (`AvisoResponse.java:19-27`) e nas três listagens de pessoas:

- `FuncionarioService.listar` (`FuncionarioService.java:41-45`) — pior caso do projeto: além do lazy load da `Pessoa`, chama `pessoaFotoService.buscarUrl(id)` **individualmente** por funcionário, quando o próprio `PessoaFotoService` já expõe `buscarUrls(ids)` em lote (usado corretamente em `DemandaService`). 200 funcionários ≈ 401 queries numa requisição.
- `MoradorService.listar` (`MoradorService.java:41-42`) e `AdministradorService.listar` (`AdministradorService.java:46-47`) — mesmo lazy load.

**Correção global (faça esta primeiro):**

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 50
```

Isso transforma "N selects individuais" em "N/50 selects com `IN (...)`" em todo o projeto, sem tocar em nenhum service.

**Correção cirúrgica onde ainda doer** (`DemandaRepository`) — todos são `@ManyToOne`/`@OneToOne`, então múltiplos `join fetch` são seguros aqui (não geram produto cartesiano):

```java
@Query("""
    select d from Demanda d
      left join fetch d.moradorSolicitante ms      left join fetch ms.pessoa
      left join fetch d.funcionarioSolicitante fs  left join fetch fs.pessoa
      left join fetch d.funcionarioResponsavel fr  left join fetch fr.pessoa
      left join fetch d.funcionarioAprovador fa    left join fetch fa.pessoa
      left join fetch d.statusKanban
    where d.condominio.id = :condominioId
    order by d.createdAt desc
    """)
List<Demanda> findByCondominioIdComAssociacoes(@Param("condominioId") Integer condominioId);
```

E em `FuncionarioService.listar`, trocar a busca de foto individual pela em lote:

```java
List<Funcionario> funcionarios = repository.findAllComPessoa(); // @Query com join fetch f.pessoa
Map<Integer, String> fotos = pessoaFotoService.buscarUrls(
        funcionarios.stream().map(Funcionario::getId).toList());
return funcionarios.stream()
        .map(f -> FuncionarioResponse.from(f, fotos.get(f.getId())))
        .toList();
```

## 3. `CondominioService.listar`: 4 queries extras por condomínio

**Severidade: alta.** `CondominioService.java:45-53` faz `findAll().stream().map(this::paraResponse)`, e `paraResponse` (`:142-152`) dispara por condomínio: `blocoRepository.countByCondominioId`, dois `countAtivosPorCondominio` e um `gifService.buscarUrl(id)` — que faz **outro** `findById` do condomínio que já está carregado em mãos.

O `findById` redundante é ganho grátis:

```java
public String buscarUrl(Condominio condominio) {
    return condominio.getGifUrl() != null ? caminhoGif(condominio.getId()) : null;
}
```

Para as contagens, uma query agregada por lista em vez de uma por linha:

```java
@Query("""
    select fc.condominio.id, count(fc) from FuncionarioCondominio fc
    where fc.condominio.id in :ids and fc.situacao = 'ativo'
    group by fc.condominio.id
    """)
List<Object[]> contarAtivosPorCondominios(@Param("ids") List<Integer> ids);
```

## 4. Frontend: `/demandas` dispara 3 requisições por demanda

**Severidade: crítica.** `demandas/page.tsx:255-315` — três `useEffect` separados percorrem a lista e, para **cada** demanda, chamam `listarEtapas`, `listarDocumentosDemanda` e `listarNotasDemanda`.

30 demandas = **91 requisições** no load da tela, todas competindo pelo limite de ~6 conexões simultâneas por domínio do navegador. Do lado do backend é o mesmo pico multiplicado pelo item 1 (seq scan por requisição).

Duas saídas, na ordem de preferência:

1. **Endpoints em lote** — `GET /api/demanda-etapas?demandaIds=1,2,3`, idem para documentos e notas. O backend já tem os `findByDemandaIdIn` prontos nos três repositories; falta só expor. Isso derruba 90 requisições para 3.
2. **Carregar sob demanda**, só ao expandir a linha — é o que o Kanban já faz corretamente em `abrirModalDetalhe`.

## 5. Frontend: N+1 de rede ao montar rosters

**Severidade: alta.** Três lugares listam vínculos e depois buscam cada pessoa individualmente:

- `condominios/page.tsx:260-292` (aba Funcionários) e `:294-328` (aba Moradores) — 40 funcionários = 41 requisições, refeitas a cada troca de aba (sem cache)
- `kanban/page.tsx:119-125` (`buscarRosterFuncionarios`)

No Kanban há um agravante: o roster está **dentro do `Promise.all` que libera a tela** (`kanban/page.tsx:350-376`). O quadro só aparece depois que as 21 requisições do roster terminarem, mesmo com colunas e demandas já em mãos.

- **Correção de fundo:** endpoint que devolva o vínculo já com `nome`/`cpf`/`email`/`fotoUrl` (join no backend), ou um `GET /api/funcionarios?ids=1,2,3`.
- **Correção imediata no Kanban (5 minutos):** tirar `buscarRosterFuncionarios` do `Promise.all` principal e movê-lo para um `useEffect` próprio que só alimenta o ícone de ociosos. O quadro passa a renderizar assim que colunas + demandas chegarem.

## 6. Requisição duplicada e polling em aba oculta

**Severidade: média.** Não existe SWR/React Query nem cache no `lib/api.ts` — cada componente busca por conta própria:

- `app-shell.tsx:98` e `demandas/page.tsx:233` disparam o **mesmo** `listarDemandas` em paralelo ao abrir `/demandas`; um dos dois resultados é descartado.
- Como não há um `layout.tsx` autenticado, o `AppShell` **remonta a cada navegação** e refaz `listarDemandas` + `listarTarefasAgendadas` a cada troca de tela.
- O polling de 60s (`app-shell.tsx:111-124`) não pausa com a aba em segundo plano — o `visibilitychange` só adiciona um refetch extra ao voltar.

Correção mínima, sem instalar nada:

```tsx
const intervalo = setInterval(() => {
  if (document.visibilityState === "visible") atualizar();
}, INTERVALO_ATUALIZACAO_ALERTAS_MS);
```

Correção de fundo: adotar SWR (`revalidateOnFocus` + `refreshInterval` já pausam sozinhos e deduplicam requisições iguais) ou um Provider único num `layout.tsx` de rotas autenticadas.

## 7. Listagem de demandas sem paginação, sem filtro de arquivadas, ordenada em memória

**Severidade: média, crescente.** Nenhum repository do projeto usa `Pageable` (confirmado: zero ocorrências de `Page<`/`Pageable` no backend). Em `DemandaService.java:125-145`, a listagem carrega **todas** as demandas do condomínio, filtra e ordena com `Comparator` em Java.

Três consequências: (a) o custo cresce indefinidamente porque `listar()` não filtra `arquivada = false`; (b) a ordenação em memória desperdiça o índice do item 1; (c) o campo `descricao` é `TEXT` sem limite (`Demanda.java:56-57`) e vai **inteiro** em toda linha da listagem, embora o frontend já o trunque no card (`kanban/page.tsx:87-88`) e só use completo no detalhe.

Correções, em ordem de custo-benefício:

1. `findByCondominioIdOrderByCreatedAtDesc(...)` e remover o `.sorted(...)` do Java.
2. Filtrar `arquivada = false` por padrão, com parâmetro explícito para incluir arquivadas.
3. Um `DemandaResumoResponse` sem `descricao` (ou `left(d.descricao, 200)`) para a listagem, mantendo o campo completo só no endpoint de detalhe.
4. `Pageable` no endpoint de demandas quando o volume justificar.

## 8. I/O do MinIO dentro de transação de banco

**Severidade: média-alta — é o achado com maior risco de indisponibilidade.**

`DemandaDocumentoService.upload` (`:95-118`), `PessoaFotoService.atualizar` (`:79-105`) e `CondominioGifService.atualizar` (`:62-85`) são `@Transactional`, e o proxy do Spring abre a transação (reservando uma conexão do pool) **antes** do corpo do método rodar. O `minioClient.putObject(...)` acontece com a conexão JDBC presa.

Com R2 em produção (latência bem maior que o MinIO local) e o pool default do Hikari em 10 conexões, poucos uploads concorrentes bastam para esgotar o pool e travar requisições que nada têm a ver com upload — login, Kanban, tudo.

```java
public DemandaDocumentoResponse upload(ContextoAutenticado contexto, Integer demandaId, MultipartFile arquivo) {
    Demanda demanda = buscarDemanda(demandaId);
    exigirPodeMexerAnexos(contexto, demanda);
    // ...validações...
    String chave = "demandas/%d/%s%s".formatted(demandaId, UUID.randomUUID(), extensaoPara(tipoMime));
    try (var entrada = arquivo.getInputStream()) {
        minioClient.putObject(/* ... */);   // rede FORA de qualquer transação
    }
    return salvarRegistro(demanda, contexto, chave, tipoMime, arquivo);  // @Transactional curto, só grava
}
```

Chame `salvarRegistro` por um bean separado (ou via `TransactionTemplate`) — auto-invocação dentro da mesma classe não passa pelo proxy do Spring e a anotação seria ignorada.

## 9. Configuração: 4 ajustes de arquivo, ganho desproporcional

Do `application.yml` atual falta:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # default é 10; alinhar com max_connections do Postgres
      connection-timeout: 10000    # falhar rápido em vez de segurar a thread do Tomcat
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 50   # item 2
        jdbc:
          batch_size: 50               # preventivo, para importações em massa
        format_sql: false              # em produção: custa CPU e polui log

server:
  compression:
    enabled: true
    mime-types: application/json,text/plain,text/css,application/javascript
    min-response-size: 1024
```

`format_sql: true` está ligado hoje — em produção isso reformata cada SQL logado à toa. Não inclua `image/*` na compressão: JPEG/PNG/GIF já vêm comprimidos e recomprimir só queima CPU nas rotas de arquivo.

**MinIO sem timeout** (`StorageConfig.java:16-22`): o `MinioClient` usa o OkHttp default, sem timeout de conexão/leitura/escrita. Combinado com o item 8, um storage lento pendura a thread do servlet indefinidamente.

```java
OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(5))
        .writeTimeout(Duration.ofSeconds(20))
        .readTimeout(Duration.ofSeconds(20))
        .build();
return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey)
        .httpClient(httpClient).build();
```

**`fetch` sem timeout no frontend** (`lib/api.ts`, todas as ~60 funções): o `fetch` nativo não tem timeout. Se o backend travar — inclusive pelos itens acima — a promise fica pendurada para sempre e o usuário só vê o spinner. Um helper central com `AbortController` resolve para todas de uma vez.

## 10. Cache ausente para dados quase estáticos

**Severidade: média.** Zero ocorrências de `@Cacheable`/`@EnableCaching` no projeto. Candidatos claros, consultados a cada carregamento do Kanban e praticamente nunca alterados: `StatusKanbanService.listarPorCondominio` (`:39-45`), `EtiquetaService.listarPorCondominio` (`:38-43`) e os dados do condomínio no cabeçalho.

`spring-boot-starter-cache` + Caffeine, TTL de 5 min, com `@CacheEvict` nos métodos de criação/atualização — resolve sem infraestrutura nova.

## 11. Kanban: 2160 linhas num componente, zero memoização

**Severidade: média (percebida como travamento).** `kanban/page.tsx` não tem um único `React.memo`/`useMemo`/`useCallback`. Colunas e cards são `.map()` inline dentro de `KanbanPageInner`, então qualquer `setState` local — passar o mouse sobre o relógio de **um** card (`handleHoverRelogio`, ~linha 587) — recria o JSX de todas as colunas e todos os cards.

Extrair `CardKanban` e `ColunaKanban` como componentes em `React.memo`, com callbacks estáveis via `useCallback`. Com 50+ cards a diferença é visível.

## 12. Itens menores, mas baratos

- **Cache-Control de 5 minutos em imagens imutáveis** (`ArquivoStorageService.java:57`): a chave no bucket é um UUID novo a cada upload, então o conteúdo de uma URL nunca muda. `CacheControl.maxAge(Duration.ofHours(12)).cachePrivate().immutable()` acompanha a validade do JWT e elimina o redownload de avatares e do GIF do condomínio a cada 5 min. (A rota `pwa-icon/[size]/route.tsx` já faz isso certo.)
- **`<img>` em vez de `next/image`** em avatares e thumbnails (`kanban/page.tsx:1422, 1492, 2046`) — sem lazy-loading, sem resize, com risco de layout shift. O GIF do condomínio (linha 1155) é exceção legítima e já está documentada no código.
- **Upload sem redimensionar no cliente** (`lib/imagem-upload.ts`): aceita até 8 MB por arquivo e envia o original. Três fotos de celular ≈ 15-20 MB em rede móvel. Um resize por `<canvas>` (máx. 1600 px no lado maior, JPEG 0.8) antes do `FormData.append` corta isso em ~10×.
- **Sem `loading.tsx`/`error.tsx` em nenhuma rota** e todas as páginas são `"use client"` — em conexão lenta a tela fica **branca** (não "Carregando...") durante download e hidratação do bundle da rota.
- **`docker-compose.yml`**: sem `mem_limit`/`cpus` em nenhum serviço e sem healthcheck no `frontend` (Postgres e MinIO já têm). Um pico em qualquer container degrada os outros; se o Next.js travar sem crashar, o `restart: unless-stopped` não age.

---

## Plano sugerido

**Primeira leva (algumas horas, sem risco):** migration de índices (1) · `default_batch_fetch_size` + compressão + Hikari (2, 9) · tirar o roster do `Promise.all` do Kanban (5) · pausar polling em aba oculta (6) · timeout no MinIO e no `fetch` (9).

**Segunda leva (dias):** endpoints em lote para etapas/documentos/notas (4) · endpoint de vínculo com dados da pessoa (5) · `join fetch` + foto em lote nas listagens de pessoas (2) · tirar o MinIO de dentro das transações (8).

**Terceira leva (quando o volume pedir):** paginação e filtro de arquivadas (7) · DTO de resumo sem `descricao` (7) · cache com Caffeine (10) · quebrar o Kanban em componentes memoizados (11).

## Como medir, e não adivinhar

Nada disso substitui medição. Dois passos baratos que dão números reais:

```yaml
# application.yml — só em dev: mostra o SQL real e denuncia o N+1 na hora
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

E, no Postgres, a extensão `pg_stat_statements` para ver quais queries realmente dominam o tempo depois de aplicar os índices:

```sql
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
SELECT calls, mean_exec_time, query FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 20;
```

---

# Adendo — código novo (V18 + anexo de vídeo)

Revisão da diferença entre o estado analisado acima e o código de 07/09: migration `V18__etiqueta_visivel_morador.sql`, suporte a **anexo de vídeo** (antes só imagem), exclusão de coluna do Kanban, e `Content-Length` no download de arquivo.

O vídeo é a mudança de peso. Ele multiplica por ~2 o tamanho do maior anexo (8 MB → 15 MB) e, mais importante, muda o **padrão de acesso**: imagem é baixada uma vez e fica; vídeo é acessado em pedaços, várias vezes, com o usuário arrastando a barra. A arquitetura atual de download não foi feita para isso.

## A1. Sem suporte a HTTP Range — o mais importante do adendo

**Severidade: crítica, e é bug funcional antes de ser de performance.**

`ArquivoStorageService.baixar` (`:55-73`) sempre responde `200 OK` com o corpo inteiro. Não há `HttpRange`, `ResourceRegion`, `Accept-Ranges` nem leitura do header `Range` em lugar nenhum do backend (confirmado por busca em todo `src/main/java`: zero ocorrências).

O `Content-Length` que você adicionou resolve metade do problema — o player passa a saber a duração. Mas sem `Accept-Ranges` o navegador não consegue pedir "os bytes 5.000.000 a 6.000.000":

- **Arrastar a barra não funciona** de forma confiável. O player não tem como pular para o meio do arquivo.
- **Safari/iOS costuma recusar reproduzir** vídeo servido sem suporte a Range. Como o sistema é usado como PWA no celular, esse é o caso de uso principal, não um canto obscuro.
- Cada tentativa de seek que o navegador faz vira um download novo do começo, pelo caminho MinIO → JVM → navegador.

**Correção** — o SDK do MinIO já suporta faixa, então dá para atender o Range de verdade:

```java
// no controller: @RequestHeader(value = "Range", required = false) String range

// no ArquivoStorageService, quando range != null:
minioClient.getObject(GetObjectArgs.builder()
        .bucket(bucket).object(chave)
        .offset(inicio).length(fim - inicio + 1)
        .build());

return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .header(HttpHeaders.CONTENT_RANGE, "bytes %d-%d/%d".formatted(inicio, fim, tamanhoTotal))
        .contentLength(fim - inicio + 1)
        .contentType(MediaType.parseMediaType(tipoMime))
        .body(new InputStreamResource(conteudo));
```

Enquanto isso não existir, mantenha `Accept-Ranges: none` explícito para o navegador não tentar o que o servidor não faz.

## A2. `/demandas` monta um `<video>` por anexo de cada demanda da lista

**Severidade: alta.** Em `demandas/page.tsx:1573-1600`, os anexos de **todas** as demandas visíveis são renderizados inline, e anexo de vídeo vira um `<video src=... preload="metadata">` de 64×64 px.

O Kanban faz isso **certo**: o card mostra só um ícone (`temAnexos`), e a miniatura só aparece dentro do modal de detalhe, um card por vez (`kanban/page.tsx:2076`). A tela `/demandas` não tem esse cuidado — e ela já carrega os documentos de cada demanda separadamente (o fan-out do item 4 do relatório principal).

`preload="metadata"` limita o dano em navegadores que abortam a conexão assim que leem o cabeçalho do arquivo, mas com o servidor ignorando Range esse comportamento fica a critério do navegador. O que é certo é o número de conexões: uma por anexo, todas atravessando o backend, que por sua vez abre uma conexão ao MinIO para cada uma.

**Correção:** não usar `<video>` como miniatura. Renderizar um placeholder com o selo de play e só montar o elemento ao clicar — exatamente o que o Kanban já faz:

```tsx
{ehVideo(doc.tipoMime) ? (
  <button onClick={() => abrirPreviewVideo(doc)} className="relative h-full w-full bg-slate-800">
    <IconePlay className="absolute inset-0 m-auto h-6 w-6 text-white" />
  </button>
) : (
  <img src={urlImagem(doc.url, sessao.token)} alt={doc.nomeArquivo} className="h-full w-full object-cover" />
)}
```

Melhor ainda: gerar um poster por `<canvas>` no momento do upload (o `File` já está em mãos em `upload-imagens.tsx`) e guardá-lo como um segundo objeto no bucket. Aí a miniatura vira um JPEG de alguns kB.

## A3. Thread do Tomcat presa durante todo o streaming

**Severidade: alta, agravada pelo vídeo.** O download é síncrono e bloqueante: a thread da requisição fica ocupada escrevendo o `InputStreamResource` no corpo da resposta durante toda a transferência. Com imagem de alguns kB isso era irrelevante; com vídeo de 15 MB em rede móvel, são dezenas de segundos por download.

O pool default do Tomcat é 200 threads, e não há `server.tomcat.threads.*` no `application.yml`. Downloads de vídeo concorrentes competem com **todas** as outras rotas — login, Kanban, listagem.

Implementar Range (A1) reduz muito o problema, porque cada resposta passa a ser um pedaço pequeno em vez do arquivo inteiro. Para ir além, `StreamingResponseBody` com um `TaskExecutor` dedicado tira esses downloads do pool principal.

**Ponto verificado e correto:** a chamada ao MinIO no download acontece **fora** da transação — `buscarParaBaixar` fecha a transação antes de o controller chamar o storage. Nenhuma conexão de banco fica presa durante o streaming. O problema do item 8 do relatório principal (I/O dentro de `@Transactional`) continua valendo só para o *upload*.

## A4. Mime-type validado apenas pelo header do cliente

**Severidade: média.** `DemandaDocumentoService.java:122-128` decide se é imagem ou vídeo por `arquivo.getContentType()` — o `Content-Type` que o próprio cliente declara no multipart. Nada verifica o conteúdo.

Consequência prática mais imediata: a cota por tipo é contornável. Declarar `video/mp4` numa imagem libera 15 MB em vez de 8 MB. E qualquer binário pode ser armazenado sob um dos seis tipos aceitos.

Uma checagem de magic bytes é barata e resolve o caso óbvio:

```java
byte[] cabecalho = arquivo.getInputStream().readNBytes(16);
// JPEG FF D8 FF · PNG 89 50 4E 47 · GIF "GIF8" · WEBP "RIFF"..."WEBP"
// MP4/MOV "ftyp" no offset 4 · WebM 1A 45 DF A3
if (!assinaturaBateComTipo(cabecalho, tipoMime)) {
    throw new InvalidRequestException("Conteúdo do arquivo não bate com o tipo declarado");
}
```

Sobre o limite de tamanho: ele só é conferido depois que o Spring já recebeu o arquivo inteiro (`arquivo.getSize()` só existe aí). Isso é inerente ao multipart bloqueante do Servlet e não vale reescrever — mas note que subir o teto do Spring de 8 MB para 20 MB aumentou de zero para ~12 MB o desperdício por upload rejeitado. O trade-off está documentado no `application.yml` e faz sentido; só vale saber que existe.

## A5. Upload sem progresso, sem timeout e sem cancelamento

**Severidade: média.** `adicionar-anexo-botao.tsx:27-66` mostra apenas `"…"` enquanto envia. Um vídeo de 15 MB em 4G leva de 60 a 120 segundos — nesse tempo o usuário não sabe se travou, não pode cancelar, e se a conexão cair o `fetch` fica pendurado (não tem timeout).

`fetch` não expõe progresso de upload; `XMLHttpRequest` expõe. Para este caso específico vale a troca:

```ts
const xhr = new XMLHttpRequest();
xhr.upload.onprogress = (e) => e.lengthComputable && setPct(Math.round((e.loaded / e.total) * 100));
xhr.timeout = 180_000;
```

## A6. Exclusão de coluna do Kanban: `deleteBy...` derivado apaga uma linha por vez

**Severidade: média-baixa** (ação de administrador, rara). `DemandaStatusKanbanHistoricoRepository:28` declara `deleteByStatusAnteriorIdOrStatusNovoId`. Método `deleteBy` derivado do Spring Data carrega as entidades com um `SELECT` e remove **uma a uma** — não é um `DELETE ... WHERE` único. Numa coluna antiga com muito histórico, são N round-trips.

```java
@Modifying
@Query("delete from DemandaStatusKanbanHistorico h where h.statusAnterior.id = :id or h.statusNovo.id = :id")
void deletarPorColuna(@Param("id") Integer id);
```

Há também uma janela entre `existsByStatusKanbanId` e o delete (`StatusKanbanService:118-127`) em que uma demanda pode ser movida para a coluna sendo excluída. Com um síndico por condomínio o risco é baixo, mas o FK do banco vai barrar com erro feio em vez de mensagem clara.

## A7. Correção de uma decisão minha no V17

Ao montar o V17 eu deixei `demandas.id_status_kanban` de fora, argumentando que nenhuma consulta filtrava por ela. **O código novo mudou isso**: `DemandaRepository.existsByStatusKanbanId` (`:26`) é chamado justamente no fluxo de exclusão de coluna. Sem índice, cada tentativa de excluir uma coluna faz seq scan em `demandas`.

```sql
-- V19
CREATE INDEX idx_demandas_status_kanban ON demandas (id_status_kanban);
```

## Verificado e correto no código novo

- **Nenhum N+1 novo.** `DemandaService.listar` continua buscando etiquetas em lote (`findByDemandaIdInComEtiqueta`, com `join fetch`), e o filtro `visivelMorador` é aplicado em memória sobre a lista já carregada — não gera consulta por item. O custo novo é a query de etiquetas passar a rodar também para morador, que antes recebia lista vazia; é o preço da funcionalidade, não uma regressão.
- **A regra `visivel_morador` não vaza.** Percorri todos os caminhos que devolvem etiqueta: `EtiquetaController` e `DemandaEtiquetaController` barram morador com 403 antes de qualquer coisa; `DemandaService.listar` filtra; os métodos de demanda isolada (aprovar, reprovar, mover, arquivar) exigem funcionário; `acompanhar`/`deixarDeAcompanhar` devolvem lista vazia. Sem furo de autorização.
- **Validação de tamanho no cliente** antes de enviar (`imagem-upload.ts:50-71`), com limites que batem com os do backend.
- **`URL.revokeObjectURL`** chamado no cleanup do `useEffect` em `upload-imagens.tsx` — sem vazamento de blob URLs.
- **Upload não passa por base64.** `FormData` + o `File` bruto; nada de `FileReader` ou `arrayBuffer()` inflando a heap do navegador.
- **Kanban não carrega mídia em massa** — só o ícone `temAnexos` no card.

## Ordem sugerida

1. **A1 (Range)** — desbloqueia o vídeo no iOS e conserta o seek. É o único item que muda se a funcionalidade *funciona*.
2. **A2 (miniatura de vídeo em `/demandas`)** — meia hora, e alinha a tela com o que o Kanban já faz certo.
3. **A7 (índice)** — uma linha.
4. **A5 (progresso de upload)** — o usuário sente na hora.
5. **A4, A6** — quando sobrar tempo.
