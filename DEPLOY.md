# Deploy

Ambiente de producao: VPS Hostinger, tudo em Docker, Traefik na frente.
Os arquivos de infraestrutura (`docker-compose.yml`, config do Traefik, `.env` com os
segredos) vivem **na propria VPS**, em `/opt/condominio` — nao neste repositorio.

## Como o trafego chega

```
navegador
   |
   v  HTTPS (Let's Encrypt, renovacao automatica)
Traefik  :80 -> redireciona pra :443
   |
   +-- romtechsolucoes.com.br / www  -->  frontend (Next.js, porta 3000)
   |                                          |
   |                                          |  rewrites() de /api/* — servidor a servidor,
   |                                          |  pela rede interna do Docker
   |                                          v
   +-- api.romtechsolucoes.com.br    -->  backend (Spring Boot, porta 8080)
       (basic auth do Traefik — so pro Swagger)      |
                                                     +-- postgres  (rede interna)
                                                     +-- minio     (rede interna)
```

Postgres, MinIO e a API **nao publicam porta nenhuma no host**. De fora da VPS eles nao
existem: o unico processo escutando na internet e o Traefik. O navegador so conversa com
`romtechsolucoes.com.br` — mesma origem pra tudo, o que tambem elimina CORS.

`api.romtechsolucoes.com.br` existe so pra alcancar o Swagger em caso de investigacao, e
por isso fica atras de basic auth (usuario/senha em `/opt/condominio/CREDENCIAIS-GERADAS.txt`).

## Como o deploy acontece

Push na `main` -> GitHub Actions -> build da imagem -> push pro GHCR -> SSH na VPS ->
`docker compose pull` + `up -d` do servico -> smoke test contra o dominio.

| Repositorio             | Servico    | Imagem                                       |
|-------------------------|------------|----------------------------------------------|
| `gestao-condominio`     | `backend`  | `ghcr.io/romulo88/gestao-condominio-backend` |
| `gestao-condominio-web` | `frontend` | `ghcr.io/romulo88/gestao-condominio-web`     |

Cada imagem recebe duas tags: `latest` (que o compose da VPS referencia) e o SHA do commit
(historico, pra voltar a uma versao especifica sem rebuildar).

O login no GHCR feito na VPS usa o `GITHUB_TOKEN` efemero do proprio job — valido so
enquanto o deploy roda. Nao existe token de longa duracao guardado no servidor.

### Secrets necessarios em cada repositorio

| Secret        | Valor                                        |
|---------------|----------------------------------------------|
| `VPS_HOST`    | IP da VPS                                    |
| `VPS_USER`    | `deploy`                                     |
| `VPS_SSH_KEY` | chave privada de deploy (ed25519, sem senha) |

O usuario `deploy` nao e root: pertence ao grupo `docker` e e dono de `/opt/condominio`.
Se a chave privada guardada no GitHub vazar, o alcance e mexer nos containers — nao no
servidor inteiro.

## Runbook

```bash
ssh deploy@<ip-da-vps>
cd /opt/condominio

docker compose ps                    # o que esta de pe
docker compose logs -f backend       # logs ao vivo (frontend/traefik igual)
docker compose restart backend       # reiniciar sem trocar de imagem
docker compose up -d --force-recreate frontend
```

**Voltar pra uma versao anterior** (a tag do SHA continua no GHCR):

```bash
cd /opt/condominio
sed -i 's|gestao-condominio-backend:latest|gestao-condominio-backend:<sha>|' docker-compose.yml
docker compose up -d backend
```

**Backup do banco**:

```bash
docker compose exec -T postgres pg_dump -U condominio condominio_gestao | gzip > backup-$(date +%F).sql.gz
```

**Certificado**: o Traefik renova sozinho. O estado fica no volume `condominio_traefik_acme`
— nao apagar esse volume sem necessidade (o Let's Encrypt tem limite de emissoes por semana).

## A pegadinha do BACKEND_URL

O proxy `/api/*` do frontend (`rewrites()` em `next.config.ts`) e resolvido pelo Next
**durante o `npm run build`** e gravado em `.next/routes-manifest.json`. O `server.js` do
build standalone nunca mais le `process.env.BACKEND_URL`.

Consequencia pratica: **definir `BACKEND_URL` no `environment:` do compose nao tem efeito
nenhum**. O valor de producao entra como build arg, no workflow do frontend:

```yaml
build-args: |
  BACKEND_URL=http://backend:8080
```

Trocar a URL do backend exige rebuildar a imagem do frontend, nao so recriar o container.
Se um dia isso incomodar, a saida e substituir o `rewrites()` por um Route Handler
catch-all (`src/app/api/[...path]/route.ts`) que le a env a cada request — aí vira
configuracao de runtime de verdade.

Sintoma de ter errado isso: o site sobe, `/login` renderiza normal, o container fica
`healthy`, e **toda** chamada de API falha — porque a imagem ficou apontando pro default de
desenvolvimento (`host.docker.internal`), que nao resolve dentro de um container Linux.

## Migrations

O Flyway roda no start do backend (`spring.flyway.enabled=true`). Uma migration nova entra
em producao pelo mesmo deploy do codigo — nao existe passo manual. Como `ddl-auto` e
`validate`, se as entidades divergirem das tabelas o backend **nao sobe**, e o smoke test
do workflow reprova o deploy em vez de deixar a aplicacao meio quebrada no ar.
