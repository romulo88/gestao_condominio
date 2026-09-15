# condominio-gestao

Sistema de gestão de demandas de condomínios. Ver [`HANDOFF.md`](HANDOFF.md) para o estado geral do projeto e [`docs/modelo-dados.md`](docs/modelo-dados.md) para o rationale do modelo de dados.

## Stack

Frontend Next.js (React) · Backend Java (Spring Boot) + Spring Data JPA/Hibernate · PostgreSQL · MinIO (S3-compatível, storage de anexos).

## Estrutura

```
condominio-gestao/
├── docker-compose.yml   # Postgres + MinIO (infra local) + frontend (containerizado)
├── backend/             # API Java/Spring Boot (roda fora do compose, via Maven)
└── frontend/            # Next.js (App Router) — Dockerfile próprio (build "standalone")
```

## Rodando localmente

Pré-requisitos: [Docker Desktop](https://www.docker.com/products/docker-desktop/), **JDK 21+** e **Maven 3.9+**. Node só é necessário se for rodar o frontend fora do Docker (opção B abaixo).

```bash
docker compose up -d              # sobe Postgres + MinIO (cria o bucket de anexos) + frontend em localhost:3000
cd backend
mvn spring-boot:run                # aplica as migrations do Flyway automaticamente e sobe a API em localhost:8080
```

O frontend já sobe pelo `docker compose up -d`, buildado com `NEXT_PUBLIC_API_URL=http://localhost:8080` (o `fetch` roda no navegador, então é `localhost`, não `host.docker.internal`, mesmo com o front containerizado — só o backend precisa estar de pé, rodando ou não em container). Se mudar essa URL (`docker-compose.yml`) ou o código do front, é preciso rebuildar a imagem, já que `NEXT_PUBLIC_*` é embutido no build:
```bash
docker compose build frontend && docker compose up -d frontend
```

**Opção B — desenvolvimento com hot reload** (mais rápido pra mexer no front): pare o container (`docker compose stop frontend`) e rode direto:
```bash
cd frontend
npm install     # só na primeira vez
npm run dev     # sobe em localhost:3000
```

Isso deixa disponíveis:
- **Postgres** em `localhost:5432` (usuário/senha/banco: `condominio` / `condominio` / `condominio_gestao`)
- **MinIO** — API S3 em `localhost:9000`, console web em `localhost:9001` (login `condominio` / `condominio123`)
- **Frontend** em `localhost:3000` (redireciona pra `/login`)
- **API** em `localhost:8080`
- **Swagger UI** em `localhost:8080/swagger-ui.html` — lista e permite testar todos os endpoints (spec JSON crua em `/v3/api-docs`). Todo endpoint (exceto `/api/auth/**`) exige login — faça `POST /api/auth/login`, copie o `token` da resposta e cole no botão **Authorize** (canto superior direito) pra testar o resto.

Para derrubar tudo: `docker compose down` (os dados ficam guardados nos volumes `postgres_data`/`minio_data`; use `docker compose down -v` para apagar de vez).

## Autenticação

Login é por CPF/senha. Se a pessoa tiver vínculo em mais de um condomínio (ou mais de um papel no mesmo condomínio), o login pede pra escolher qual usar.

```bash
# 1. primeiro acesso: definir uma senha (provisório - ver aviso de segurança em docs/modelo-dados.md)
curl -X POST localhost:8080/api/auth/definir-senha \
  -H "Content-Type: application/json" \
  -d '{"cpf": "12345678900", "novaSenha": "senha-forte-123"}'

# 2. login
curl -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf": "12345678900", "senha": "senha-forte-123"}'
# -> se só tiver 1 vínculo ativo, já vem "token" pronto
# -> se tiver mais de 1, vem "contextos" (lista) + "preAuthToken"

# 3. só se teve mais de 1 contexto: escolher qual usar
curl -X POST localhost:8080/api/auth/contexto \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <preAuthToken>" \
  -d '{"condominioId": 1, "tipoPapel": "funcionario"}'
# -> pra escolher o contexto de administrador, condominioId vai null: {"condominioId": null, "tipoPapel": "administrador"}

# 4. usar o token em qualquer outro endpoint
curl localhost:8080/api/condominios -H "Authorization: Bearer <token>"
```

## Administrador (papel global) e bootstrap

`administrador` é um papel de `Pessoa` **sem vínculo com nenhum condomínio** (diferente de síndico, que é um perfil de `Funcionario` escopado a um condomínio) — é quem cadastra os condomínios do sistema. `POST /api/condominios` e `POST /api/administradores` só funcionam com um token de administrador.

Não existe autocadastro do primeiro administrador (senão qualquer um poderia virar admin) — a primeira pessoa precisa ser inserida direto no banco:

```sql
INSERT INTO pessoas (nome, cpf, email, created_at, updated_at)
VALUES ('Seu Nome', '12345678900', 'voce@exemplo.com', now(), now())
RETURNING id_pessoa; -- anota o id gerado, ex: 1

INSERT INTO administradores (id_administrador, created_at, updated_at)
VALUES (1, now(), now());
```

Depois disso, o fluxo é o normal: `POST /api/auth/definir-senha` com esse CPF, depois `POST /api/auth/login` (contexto único → já vem o token completo). A partir daí, esse administrador pode criar outros via `POST /api/administradores`.

## Quadro de avisos

`condominioId` e o autor vêm do token — não precisa (nem dá pra) informar no corpo.

```bash
# criar um aviso (token de funcionário)
curl -X POST localhost:8080/api/avisos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"descricao": "Piscina interditada devido a vazamentos", "dataExpiracao": "2026-09-01T00:00:00"}'

# ver os avisos visíveis agora (qualquer pessoa autenticada - funcionário ou morador)
curl localhost:8080/api/avisos -H "Authorization: Bearer <token>"
```

## Migrations

O Flyway é o dono do schema (`backend/src/main/resources/db/migration/`). A migration inicial (`V1__init.sql`) cria as 16 tabelas do modelo. Mudanças futuras: criar um novo arquivo `V2__descricao.sql` na mesma pasta - o Flyway aplica automaticamente na próxima subida da aplicação.

As entidades JPA (`backend/src/main/java/com/condominiogestao/`) usam `ddl-auto: validate` - o Hibernate confere que as classes batem com o schema real, mas nunca gera/altera DDL sozinho. Se mudar uma entidade, é preciso escrever a migration SQL correspondente à mão.
