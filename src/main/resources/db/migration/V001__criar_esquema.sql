-- ============================================================================
-- Sobra - Esquema do banco de dados, versao V001
-- SQL no padrao ANSI, validado em PostgreSQL 16. Ordem: pais antes dos filhos.
-- ============================================================================

-- 1) usuario ----------------------------------------------------------------
CREATE TABLE usuario (
    id                   BIGINT        GENERATED ALWAYS AS IDENTITY,
    nome_completo        VARCHAR(150) NOT NULL,
    email                VARCHAR(254) NOT NULL,
    senha_hash           VARCHAR(255) NOT NULL,
    avatar_url           VARCHAR(500),
    notificacoes_ativas BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = LOWER(email)),
    CONSTRAINT ck_usuario_nome_preenchido CHECK (CHAR_LENGTH(TRIM(nome_completo)) > 0)
);

-- 2) categoria (tabela de referencia, 7 linhas fixas) ----------------------
CREATE TABLE categoria (
    id      SMALLINT    NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    nome    VARCHAR(40) NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id),
    CONSTRAINT uq_categoria_codigo UNIQUE (codigo),
    CONSTRAINT ck_categoria_codigo_maiusculo CHECK (codigo = UPPER(codigo))
);

-- 3) renda_vigencia (historico de renda: "vale a partir do mes") -----------
CREATE TABLE renda_vigencia (
    usuario_id BIGINT         NOT NULL,
    mes_inicio DATE           NOT NULL,
    valor       DECIMAL(12,2) NOT NULL,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_renda_vigencia PRIMARY KEY (usuario_id, mes_inicio),
    CONSTRAINT fk_renda_vigencia_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT ck_renda_vigencia_dia1 CHECK (EXTRACT(DAY FROM mes_inicio) = 1),
    CONSTRAINT ck_renda_vigencia_valor CHECK (valor > 0)
);

-- 4) orcamento_categoria (usuario N:N categoria) ----------------------------
CREATE TABLE orcamento_categoria (
    usuario_id      BIGINT         NOT NULL,
    categoria_id    SMALLINT       NOT NULL,
    valor_estimado DECIMAL(12,2),
    CONSTRAINT pk_orcamento_categoria PRIMARY KEY (usuario_id, categoria_id),
    CONSTRAINT fk_orcamento_categoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT fk_orcamento_categoria_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria (id) ON DELETE RESTRICT ON UPDATE NO ACTION,
    CONSTRAINT ck_orcamento_categoria_valor
        CHECK (valor_estimado IS NULL OR valor_estimado > 0)
);

-- 5) despesa -----------------------------------------------------------------
CREATE TABLE despesa (
    id             BIGINT        GENERATED ALWAYS AS IDENTITY,
    usuario_id     BIGINT        NOT NULL,
    categoria_id SMALLINT        NOT NULL,
    descricao      VARCHAR(120) NOT NULL,
    valor          DECIMAL(12,2) NOT NULL,
    data           DATE          NOT NULL,
    criado_em      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_despesa PRIMARY KEY (id),
    CONSTRAINT fk_despesa_usuario FOREIGN KEY (usuario_id)
         REFERENCES usuario (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT fk_despesa_categoria FOREIGN KEY (categoria_id)
         REFERENCES categoria (id) ON DELETE RESTRICT ON UPDATE NO ACTION,
    CONSTRAINT ck_despesa_valor CHECK (valor > 0),
    CONSTRAINT ck_despesa_descricao CHECK (CHAR_LENGTH(TRIM(descricao)) > 0),
    CONSTRAINT ck_despesa_data CHECK (data >= DATE '2000-01-01')
);

-- 6) objetivo ----------------------------------------------------------------
CREATE TABLE objetivo (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY,
    usuario_id    BIGINT        NOT NULL,
    nome          VARCHAR(80)   NOT NULL,
    valor_meta    DECIMAL(12,2) NOT NULL,
    prazo_meses   INTEGER       NOT NULL,
    aporte_mensal DECIMAL(12,2) NOT NULL,                 -- [D-05 recomendada]
    imagem_url    VARCHAR(500),
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_objetivo PRIMARY KEY (id),
    CONSTRAINT fk_objetivo_usuario FOREIGN KEY (usuario_id)
         REFERENCES usuario (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT ck_objetivo_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_objetivo_valor_meta CHECK (valor_meta > 0),
    CONSTRAINT ck_objetivo_prazo CHECK (prazo_meses BETWEEN 1 AND 600),
    CONSTRAINT ck_objetivo_aporte_mensal CHECK (aporte_mensal > 0)
);

-- 7) aporte ------------------------------------------------------------------
CREATE TABLE aporte (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY,
    objetivo_id BIGINT          NOT NULL,
    tipo          VARCHAR(10)   NOT NULL,                  -- [D-06 recomendada]
    valor         DECIMAL(12,2) NOT NULL,
    data          DATE          NOT NULL,
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_aporte PRIMARY KEY (id),
    CONSTRAINT fk_aporte_objetivo FOREIGN KEY (objetivo_id)
         REFERENCES objetivo (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT ck_aporte_tipo CHECK (tipo IN ('MENSAL', 'EXTRA')),
    CONSTRAINT ck_aporte_valor CHECK (valor > 0),
    CONSTRAINT ck_aporte_data CHECK (data >= DATE '2000-01-01')
);

-- 8) simulacao -----------------------------------------------------------------
CREATE TABLE simulacao (
    id             BIGINT        GENERATED ALWAYS AS IDENTITY,
    usuario_id     BIGINT        NOT NULL,
    valor_inicial DECIMAL(12,2) NOT NULL,
    aporte_mensal DECIMAL(12,2) NOT NULL,
    prazo_meses    INTEGER       NOT NULL,
    taxa_mensal    DECIMAL(8,6) NOT NULL,                -- [D-01 recomendada]
    resultado      DECIMAL(15,2) NOT NULL,
    criado_em      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_simulacao PRIMARY KEY (id),
    CONSTRAINT fk_simulacao_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT ck_simulacao_valores CHECK (valor_inicial >= 0 AND aporte_mensal >= 0
                                           AND (valor_inicial > 0 OR aporte_mensal > 0)),
    CONSTRAINT ck_simulacao_prazo CHECK (prazo_meses BETWEEN 1 AND 600),
    CONSTRAINT ck_simulacao_taxa CHECK (taxa_mensal >= 0 AND taxa_mensal <= 1),
    CONSTRAINT ck_simulacao_resultado CHECK (resultado >= 0)
);

-- 9) sessao ----------------------------------------------------------------------
CREATE TABLE sessao (
    id           BIGINT   GENERATED ALWAYS AS IDENTITY,
    usuario_id   BIGINT   NOT NULL,
    token_hash   CHAR(64) NOT NULL,
    expira_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    revogada_em TIMESTAMP WITH TIME ZONE,
    criado_em    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sessao PRIMARY KEY (id),
    CONSTRAINT uq_sessao_token UNIQUE (token_hash),
    CONSTRAINT fk_sessao_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT ck_sessao_expiracao CHECK (expira_em > criado_em),
    CONSTRAINT ck_sessao_revogacao CHECK (revogada_em IS NULL OR revogada_em >= criado_em)
);

-- 10) recuperacao_senha ------------------------------------------------------------
CREATE TABLE recuperacao_senha (
    id          BIGINT   GENERATED ALWAYS AS IDENTITY,
    usuario_id BIGINT    NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expira_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em    TIMESTAMP WITH TIME ZONE,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recuperacao_senha PRIMARY KEY (id),
    CONSTRAINT uq_recuperacao_senha_token UNIQUE (token_hash),
    CONSTRAINT fk_recuperacao_senha_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT ck_recuperacao_senha_expiracao CHECK (expira_em > criado_em),
    CONSTRAINT ck_recuperacao_senha_uso CHECK (usado_em IS NULL OR usado_em >= criado_em)
);

-- Indices (alem dos criados automaticamente por PRIMARY KEY e UNIQUE) --------------
CREATE INDEX ix_despesa_usuario_data     ON despesa (usuario_id, data);
CREATE INDEX ix_objetivo_usuario_criado ON objetivo (usuario_id, criado_em);

CREATE INDEX ix_aporte_objetivo_data     ON aporte (objetivo_id, data DESC, id DESC);
CREATE INDEX ix_simulacao_usuario_criado ON simulacao (usuario_id, criado_em DESC, id DESC);
CREATE INDEX ix_sessao_usuario           ON sessao (usuario_id);
CREATE INDEX ix_recuperacao_senha_usuario ON recuperacao_senha (usuario_id);
