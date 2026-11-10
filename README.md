# Sobra — back end

API REST do app Android **Sobra** (finanças pessoais), feita a partir da
*Especificação de Trabalho de Back End v1.0*. Implementa o banco de dados (10 tabelas),
as **22 operações** do contrato (`/api/v1`), as regras de negócio RN-01 a RN-09 e os
requisitos não funcionais que cabem no código.

**Stack:** Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate) · PostgreSQL 16 · Flyway · Argon2id.

---

## Como rodar

**1. Banco** (PostgreSQL 16). Com Docker:

```bash
docker compose up -d          # cria os bancos "sobra" (dev) e "sobra_test" (testes), usuário/senha sobra/sobra
```

Sem Docker, crie os dois bancos à mão e aponte as variáveis abaixo para eles.

**2. API** (perfil `dev` por padrão, porta 8084):

```bash
./mvnw spring-boot:run
curl http://localhost:8084/api/v1/saude      # {"status":"ok"}
```

Ao subir, o Flyway aplica `V001` (esquema), `V002` (7 categorias) e, só em dev/teste, `V003` (dados de exemplo).
O Hibernate **não** cria nem altera tabelas (`ddl-auto: none`): o esquema é só das migrações.

**Variáveis de ambiente** (nada de senha no código):

| Variável | Padrão |
|---|---|
| `SOBRA_DB_URL` | `jdbc:postgresql://localhost:5432/sobra` |
| `SOBRA_DB_USUARIO` / `SOBRA_DB_SENHA` | `sobra` / `sobra` |
| `SOBRA_DB_URL_TESTE` | `jdbc:postgresql://localhost:5432/sobra_test` |
| `SOBRA_URL_PUBLICA` | `http://localhost:8084` (vai no link de recuperação de senha) |
| `PORT` | `8084` |

Se você já usa o banco antigo (porta 5434, usuário `lucas`), basta:
`SOBRA_DB_URL=jdbc:postgresql://localhost:5434/sobra SOBRA_DB_USUARIO=lucas SOBRA_DB_SENHA=... ./mvnw spring-boot:run`
— mas use um banco **vazio**: as tabelas antigas (`tb_*`) não são as da especificação.

**Testar à mão:** `docs/exemplos.http` tem todas as rotas prontas (IntelliJ ou extensão REST Client do VS Code).

O usuário de exemplo (Lucas) tem um hash de senha fictício e não faz login. Para ver os dados dele em dev,
crie uma sessão com um token conhecido e use `Authorization: Bearer token-dev-lucas`:

```sql
INSERT INTO sessao (usuario_id, token_hash, expira_em)
SELECT id, '19ca2805ca88fbbabb81fbf85213e4021e4d23610173015ba20c6b18d1ba086e', CURRENT_TIMESTAMP + INTERVAL '30 days'
FROM usuario WHERE email = 'lucas@exemplo.com';
```

(Os dados do Lucas são de setembro/2026, então use `?mes=2026-09` na Home e em Gastos.)

---

## Testes

```bash
./mvnw test      # precisa do banco sobra_test no ar
```

| Arquivo | O que cobre (seção 10 da especificação) |
|---|---|
| `dominio/RegrasNegocioTest` | Todos os casos numéricos da seção 7.1 (RN-01, 03, 04, 07, 08, 09). Sem banco. |
| `comum/ValidacaoTest` | Regras de validação (D-03) e o formato único de erro. Sem banco. |
| `api/AutenticacaoApiTest` | E2 e E8: cadastro, login, sessão expirada × token inventado, logout idempotente, sessão deslizante, recuperação e redefinição de senha, limites de taxa. |
| `api/ConfiguracaoApiTest` | E3: renda (substitui no mesmo mês, preserva meses anteriores), 7 categorias tudo-ou-nada. |
| `api/HomeEGastosApiTest` | E4 e E5: números da Home (5200 / 3480 / 1720), fronteiras de mês (31/08 e 01/10 ficam de fora), disponível negativo, validações. |
| `api/ObjetivosApiTest` | E6: percentuais, aporte sugerido, previsões (2027-11, 2028-03, 2027-03, 2027-10), objetivo concluído, lista vazia. |
| `api/SimulacaoApiTest` | E7: 14680.67 e a série (mês 0, 1, 2, 12, 24), resultado do cliente ignorado, taxa gravada. |
| `api/IsolamentoApiTest` | 10.3: usuário B nunca lê nem altera dados de A (404 igual a inexistente), `usuario_id` do cliente ignorado. |
| `api/ContratoApiTest` | 10.4: formato de erro, `/saude`, JSON malformado, corpo > 10 KB, 120 req/min por token. |

Os testes de API sobem a aplicação inteira contra o PostgreSQL com **relógio fixo em 2026-09-18**
(`sobra.relogio-fixo` no perfil `test`), para os casos da especificação darem sempre o mesmo resultado.
Eles só leem os dados do Lucas; o que grava usa usuários novos, então a suíte pode rodar várias vezes no mesmo banco.

**Restrições do banco (10.2):** `scripts/verificar_restricoes.sql` faz as 38 tentativas de violação e as duas
cascatas, e imprime `OK` ou `FALHA` em cada uma (nada é gravado):

```bash
psql -h localhost -U sobra -d sobra -f scripts/verificar_restricoes.sql
```

---

## Organização do código

```
com.Sobra.Aplicativo
├── controller   rotas HTTP (uma classe por grupo do contrato) + TratadorErros (formato único de erro)
├── service      regras de cada tela; toda consulta filtra pelo usuário da sessão
├── repository   Spring Data; as consultas Q1–Q8 da seção 5.11 estão aqui, com o SQL do documento
├── model        uma entidade por tabela, com os mesmos nomes do V001
├── dto          corpos de requisição/resposta (snake_case, dinheiro como string)
├── dominio      regras puras e testáveis: Simulador, RegrasObjetivo, Nomes, Dinheiro
├── comum        ErroApi, Validacao, Parametros, JsonErro
├── seguranca    filtro de autenticação, filtro de requisição (log, id, 10 KB), tokens, Argon2id, limite de taxa
└── config       SobraProperties (application.yml → sobra.*) e Relogio
```

Fluxo de uma requisição autenticada: `RequisicaoFilter` (id da requisição, limite de corpo, log) →
`AutenticacaoFilter` (valida o token, coloca o `usuarioId` na requisição, limite de 120/min) → controller
(`@RequestAttribute` com o `usuarioId`) → service → repository.
O id do usuário **nunca** vem do corpo ou da rota (regra 1 da seção 5.12).

---

## Decisões pendentes: o que foi adotado

Todas seguem a recomendação da seção 8. Para mudar, os pontos estão indicados.

| ID | Adotado | Onde mudar |
|---|---|---|
| D-01 | Juros compostos de 0,96% a.m., aporte no fim do mês, arredondamento só na saída; taxa gravada em cada simulação | `sobra.taxa-simulacao`, `dominio/Simulador` |
| D-02 | Simulação 1–120 meses; objetivo 1–360 (CHECK do banco aceita até 600 como trava) | `sobra.limites.*` |
| D-03 | Validação moderada (senha 8–64 sem exigir símbolos, recusa senhas comuns; valores 0,01–99.999.999,99; datas de 2000-01-01 até hoje) | `comum/Validacao` |
| D-04 | Previsão = restante ÷ aporte mensal planejado (RN-08) | `dominio/RegrasObjetivo` |
| D-05 | `objetivo.aporte_mensal` gravado na criação = meta ÷ prazo | `service/ObjetivoService` |
| D-06 | Tipo fixo MENSAL/EXTRA; o servidor monta o título | `model/Aporte#getTitulo` |
| D-07 | Sessão deslizante: 30 dias sem uso, renovação no máximo 1×/hora, teto de 90 dias | `sobra.sessao.*`, `service/SessaoService` |
| D-08 | Link por e-mail para a página web `/redefinir-senha.html` (token no `#`, válido 30 min, uso único; revoga sessões e avisa por e-mail) | `service/AuthService`, `static/redefinir-senha.html` |
| D-09 | Estimativas por categoria só como referência | — |
| D-10 | Catálogo de URLs; objetivo novo nasce com `imagem_url` nulo (o app mostra o padrão) | `service/ObjetivoService#criar` |
| D-11 | Só criar e listar (sem editar/apagar) | — |

---

## O que ainda falta para produção

- **Envio real de e-mail.** `service/EnvioEmail` só registra no log que um e-mail seria enviado (sem endereço nem token).
  Troque pelo `spring-boot-starter-mail` (`JavaMailSender`), de preferência assíncrono.
- **HTTPS (RNF-01).** O perfil `prod` liga `sobra.exigir-https` e espera um proxy/balanceador que termine o TLS
  e envie `X-Forwarded-Proto`. Sem proxy, configure `server.ssl.*`.
- **Limite de taxa** é em memória: vale para uma instância só. Com mais de uma, use um armazenamento compartilhado (ex.: Redis).
- **Backup diário com teste de restauração (RNF-09)** é infraestrutura, fora do código.
- **Exclusão de conta (LGPD, RNF-10):** o banco já apaga tudo em cascata a partir de `usuario`, mas o contrato
  não tem rota para isso. Se o produto pedir, é um `DELETE /perfil` que apaga a linha de `usuario`.
- **P-12 (aceite dos Termos)** continua como pergunta para o dono do produto.
- Simulações do V003 usam os números do protótipo, que não batem com a fórmula (nota da seção 5.10); são só dados de tela.

## Mudanças em relação ao repositório anterior

- Entidades refeitas com os nomes da especificação (`usuario`, `despesa`, … em vez de `tb_users`, `tb_despesas`, …);
  `Categoria` passou a ter id fixo e código texto (`MORADIA`), `RendaVigencia` e `OrcamentoCategoria` usam chave composta.
- Esquema criado pelo Flyway (`ddl-auto: update` → `none`), com as restrições e índices da seção 5.9.
- Saíram Lombok, MapStruct e o starter do Spring Security (a autenticação por token opaco é um filtro próprio;
  do Spring Security ficou só o módulo de criptografia, para o Argon2id).
- Senha do banco saiu do `application.yml` e foi para variáveis de ambiente.
