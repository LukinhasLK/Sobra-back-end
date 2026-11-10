-- =============================================================================
-- Sobra - Secao 10.2: 38 tentativas de violar as restricoes do banco.
-- Rode DEPOIS de V001, V002 e V003, num banco de TESTE:
--   psql -h localhost -U sobra -d sobra_verif -f scripts/verificar_restricoes.sql
-- Cada linha deve sair como "OK  #nn ... recusada por <restricao>".
-- Nada e gravado: cada tentativa roda e e desfeita.
-- =============================================================================
CREATE OR REPLACE FUNCTION pg_temp.tentar(num INT, descricao TEXT, comando TEXT, esperado TEXT)
RETURNS TEXT LANGUAGE plpgsql AS $$
DECLARE restricao TEXT; estado TEXT;
BEGIN
    BEGIN
        EXECUTE comando;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS restricao = CONSTRAINT_NAME, estado = RETURNED_SQLSTATE;
        restricao := COALESCE(NULLIF(restricao, ''), 'sqlstate ' || estado);
        RETURN CASE WHEN restricao = ANY (string_to_array(esperado, '|')) THEN 'OK   ' ELSE 'FALHA' END
               || ' #' || LPAD(num::TEXT, 2, '0') || ' ' || descricao || ' -> recusada por ' || restricao;
    END;
    RETURN 'FALHA #' || LPAD(num::TEXT, 2, '0') || ' ' || descricao || ' -> o banco ACEITOU';
END $$;

-- Atalhos para ids do exemplo
CREATE TEMP TABLE ref AS
SELECT (SELECT id FROM usuario WHERE email = 'lucas@exemplo.com') AS u,
       (SELECT id FROM objetivo WHERE nome = 'Reserva de emergência') AS o;

SELECT t FROM (
SELECT 1 n, pg_temp.tentar(1, 'usuario: e-mail repetido', format($q$INSERT INTO usuario (nome_completo,email,senha_hash) VALUES ('X','lucas@exemplo.com','h')$q$), 'uq_usuario_email') t
UNION ALL SELECT 2, pg_temp.tentar(2, 'usuario: e-mail com maiusculas', $q$INSERT INTO usuario (nome_completo,email,senha_hash) VALUES ('Ana','Ana@Exemplo.com','h')$q$, 'ck_usuario_email_minusculo')
UNION ALL SELECT 3, pg_temp.tentar(3, 'usuario: nome so com espacos', $q$INSERT INTO usuario (nome_completo,email,senha_hash) VALUES ('   ','x@x.com','h')$q$, 'ck_usuario_nome_preenchido')
UNION ALL SELECT 4, pg_temp.tentar(4, 'usuario: sem senha_hash', $q$INSERT INTO usuario (nome_completo,email) VALUES ('X','y@x.com')$q$, 'sqlstate 23502')
UNION ALL SELECT 5, pg_temp.tentar(5, 'categoria: codigo LAZER repetido', $q$INSERT INTO categoria VALUES (8,'LAZER','Lazer 2')$q$, 'uq_categoria_codigo')
UNION ALL SELECT 6, pg_temp.tentar(6, 'categoria: codigo minusculo', $q$INSERT INTO categoria VALUES (9,'pets','Pets')$q$, 'ck_categoria_codigo_maiusculo')
UNION ALL SELECT 7, pg_temp.tentar(7, 'renda: mes_inicio dia 15', format($q$INSERT INTO renda_vigencia (usuario_id,mes_inicio,valor) VALUES (%s,DATE '2026-09-15',1)$q$, (SELECT u FROM ref)), 'ck_renda_vigencia_dia1')
UNION ALL SELECT 8, pg_temp.tentar(8, 'renda: valor 0', format($q$INSERT INTO renda_vigencia (usuario_id,mes_inicio,valor) VALUES (%s,DATE '2026-10-01',0)$q$, (SELECT u FROM ref)), 'ck_renda_vigencia_valor')
UNION ALL SELECT 9, pg_temp.tentar(9, 'renda: duas no mesmo mes', format($q$INSERT INTO renda_vigencia (usuario_id,mes_inicio,valor) VALUES (%s,DATE '2026-06-01',10)$q$, (SELECT u FROM ref)), 'pk_renda_vigencia')
UNION ALL SELECT 10, pg_temp.tentar(10, 'renda: usuario 999', $q$INSERT INTO renda_vigencia (usuario_id,mes_inicio,valor) VALUES (999999,DATE '2026-10-01',10)$q$, 'fk_renda_vigencia_usuario')
UNION ALL SELECT 11, pg_temp.tentar(11, 'orcamento: valor 0', format($q$UPDATE orcamento_categoria SET valor_estimado = 0 WHERE usuario_id = %s AND categoria_id = 1$q$, (SELECT u FROM ref)), 'ck_orcamento_categoria_valor')
UNION ALL SELECT 12, pg_temp.tentar(12, 'orcamento: par repetido', format($q$INSERT INTO orcamento_categoria VALUES (%s,1,10)$q$, (SELECT u FROM ref)), 'pk_orcamento_categoria')
UNION ALL SELECT 13, pg_temp.tentar(13, 'orcamento: categoria 99', format($q$INSERT INTO orcamento_categoria VALUES (%s,99,10)$q$, (SELECT u FROM ref)), 'fk_orcamento_categoria_categoria')
UNION ALL SELECT 14, pg_temp.tentar(14, 'despesa: valor 0', format($q$INSERT INTO despesa (usuario_id,categoria_id,descricao,valor,data) VALUES (%s,1,'x',0,DATE '2026-09-01')$q$, (SELECT u FROM ref)), 'ck_despesa_valor')
UNION ALL SELECT 15, pg_temp.tentar(15, 'despesa: valor -5', format($q$INSERT INTO despesa (usuario_id,categoria_id,descricao,valor,data) VALUES (%s,1,'x',-5,DATE '2026-09-01')$q$, (SELECT u FROM ref)), 'ck_despesa_valor')
UNION ALL SELECT 16, pg_temp.tentar(16, 'despesa: descricao so espaco', format($q$INSERT INTO despesa (usuario_id,categoria_id,descricao,valor,data) VALUES (%s,1,' ',1,DATE '2026-09-01')$q$, (SELECT u FROM ref)), 'ck_despesa_descricao')
UNION ALL SELECT 17, pg_temp.tentar(17, 'despesa: data 31/12/1999', format($q$INSERT INTO despesa (usuario_id,categoria_id,descricao,valor,data) VALUES (%s,1,'x',1,DATE '1999-12-31')$q$, (SELECT u FROM ref)), 'ck_despesa_data')
UNION ALL SELECT 18, pg_temp.tentar(18, 'despesa: categoria 99', format($q$INSERT INTO despesa (usuario_id,categoria_id,descricao,valor,data) VALUES (%s,99,'x',1,DATE '2026-09-01')$q$, (SELECT u FROM ref)), 'fk_despesa_categoria')
UNION ALL SELECT 19, pg_temp.tentar(19, 'despesa: usuario 999', $q$INSERT INTO despesa (usuario_id,categoria_id,descricao,valor,data) VALUES (999999,1,'x',1,DATE '2026-09-01')$q$, 'fk_despesa_usuario')
UNION ALL SELECT 20, pg_temp.tentar(20, 'despesa: valor 10 bilhoes', format($q$INSERT INTO despesa (usuario_id,categoria_id,descricao,valor,data) VALUES (%s,1,'x',10000000000.00,DATE '2026-09-01')$q$, (SELECT u FROM ref)), 'sqlstate 22003')
UNION ALL SELECT 21, pg_temp.tentar(21, 'despesa: id informado a mao', format($q$INSERT INTO despesa (id,usuario_id,categoria_id,descricao,valor,data) VALUES (500,%s,1,'x',1,DATE '2026-09-01')$q$, (SELECT u FROM ref)), 'sqlstate 428C9')
UNION ALL SELECT 22, pg_temp.tentar(22, 'categoria: apagar Moradia (tem despesas)', $q$DELETE FROM categoria WHERE codigo = 'MORADIA'$q$, 'fk_despesa_categoria|fk_orcamento_categoria_categoria')
UNION ALL SELECT 23, pg_temp.tentar(23, 'objetivo: prazo 0', format($q$INSERT INTO objetivo (usuario_id,nome,valor_meta,prazo_meses,aporte_mensal) VALUES (%s,'x',1,0,1)$q$, (SELECT u FROM ref)), 'ck_objetivo_prazo')
UNION ALL SELECT 24, pg_temp.tentar(24, 'objetivo: prazo 601', format($q$INSERT INTO objetivo (usuario_id,nome,valor_meta,prazo_meses,aporte_mensal) VALUES (%s,'x',1,601,1)$q$, (SELECT u FROM ref)), 'ck_objetivo_prazo')
UNION ALL SELECT 25, pg_temp.tentar(25, 'objetivo: meta 0', format($q$INSERT INTO objetivo (usuario_id,nome,valor_meta,prazo_meses,aporte_mensal) VALUES (%s,'x',0,10,1)$q$, (SELECT u FROM ref)), 'ck_objetivo_valor_meta')
UNION ALL SELECT 26, pg_temp.tentar(26, 'objetivo: aporte_mensal 0', format($q$INSERT INTO objetivo (usuario_id,nome,valor_meta,prazo_meses,aporte_mensal) VALUES (%s,'x',1,10,0)$q$, (SELECT u FROM ref)), 'ck_objetivo_aporte_mensal')
UNION ALL SELECT 27, pg_temp.tentar(27, 'aporte: tipo X', format($q$INSERT INTO aporte (objetivo_id,tipo,valor,data) VALUES (%s,'X',1,DATE '2026-09-01')$q$, (SELECT o FROM ref)), 'ck_aporte_tipo')
UNION ALL SELECT 28, pg_temp.tentar(28, 'aporte: valor -1', format($q$INSERT INTO aporte (objetivo_id,tipo,valor,data) VALUES (%s,'EXTRA',-1,DATE '2026-09-01')$q$, (SELECT o FROM ref)), 'ck_aporte_valor')
UNION ALL SELECT 29, pg_temp.tentar(29, 'aporte: objetivo 999', $q$INSERT INTO aporte (objetivo_id,tipo,valor,data) VALUES (999999,'EXTRA',1,DATE '2026-09-01')$q$, 'fk_aporte_objetivo')
UNION ALL SELECT 30, pg_temp.tentar(30, 'simulacao: inicial 0 e aporte 0', format($q$INSERT INTO simulacao (usuario_id,valor_inicial,aporte_mensal,prazo_meses,taxa_mensal,resultado) VALUES (%s,0,0,12,0.0096,0)$q$, (SELECT u FROM ref)), 'ck_simulacao_valores')
UNION ALL SELECT 31, pg_temp.tentar(31, 'simulacao: prazo 0', format($q$INSERT INTO simulacao (usuario_id,valor_inicial,aporte_mensal,prazo_meses,taxa_mensal,resultado) VALUES (%s,1,1,0,0.0096,1)$q$, (SELECT u FROM ref)), 'ck_simulacao_prazo')
UNION ALL SELECT 32, pg_temp.tentar(32, 'simulacao: taxa 1,5', format($q$INSERT INTO simulacao (usuario_id,valor_inicial,aporte_mensal,prazo_meses,taxa_mensal,resultado) VALUES (%s,1,1,12,1.5,1)$q$, (SELECT u FROM ref)), 'ck_simulacao_taxa')
UNION ALL SELECT 33, pg_temp.tentar(33, 'simulacao: resultado -1', format($q$INSERT INTO simulacao (usuario_id,valor_inicial,aporte_mensal,prazo_meses,taxa_mensal,resultado) VALUES (%s,1,1,12,0.0096,-1)$q$, (SELECT u FROM ref)), 'ck_simulacao_resultado')
UNION ALL SELECT 34, pg_temp.tentar(34, 'sessao: expira antes de criar', format($q$INSERT INTO sessao (usuario_id,token_hash,expira_em,criado_em) VALUES (%s,REPEAT('a',64),TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2026-02-01 00:00:00+00')$q$, (SELECT u FROM ref)), 'ck_sessao_expiracao')
UNION ALL SELECT 35, pg_temp.tentar(35, 'sessao: token_hash repetido', format($q$INSERT INTO sessao (usuario_id,token_hash,expira_em,criado_em) VALUES (%1$s,REPEAT('b',64),TIMESTAMP WITH TIME ZONE '2027-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00'),(%1$s,REPEAT('b',64),TIMESTAMP WITH TIME ZONE '2027-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00')$q$, (SELECT u FROM ref)), 'uq_sessao_token')
UNION ALL SELECT 36, pg_temp.tentar(36, 'sessao: revogada antes de criada', format($q$INSERT INTO sessao (usuario_id,token_hash,expira_em,revogada_em,criado_em) VALUES (%s,REPEAT('c',64),TIMESTAMP WITH TIME ZONE '2027-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2025-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00')$q$, (SELECT u FROM ref)), 'ck_sessao_revogacao')
UNION ALL SELECT 37, pg_temp.tentar(37, 'recuperacao: expira antes de criar', format($q$INSERT INTO recuperacao_senha (usuario_id,token_hash,expira_em,criado_em) VALUES (%s,REPEAT('d',64),TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2026-02-01 00:00:00+00')$q$, (SELECT u FROM ref)), 'ck_recuperacao_senha_expiracao')
UNION ALL SELECT 38, pg_temp.tentar(38, 'recuperacao: usada antes de criada', format($q$INSERT INTO recuperacao_senha (usuario_id,token_hash,expira_em,usado_em,criado_em) VALUES (%s,REPEAT('e',64),TIMESTAMP WITH TIME ZONE '2027-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2025-01-01 00:00:00+00',TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00')$q$, (SELECT u FROM ref)), 'ck_recuperacao_senha_uso')
) x ORDER BY n;

-- Cascatas (feitas dentro de transacao desfeita no fim)
BEGIN;
DELETE FROM objetivo WHERE id = (SELECT o FROM ref);
SELECT CASE WHEN COUNT(*) = 0 THEN 'OK    cascata objetivo -> aportes' ELSE 'FALHA cascata objetivo' END
FROM aporte WHERE objetivo_id = (SELECT o FROM ref);
DELETE FROM usuario WHERE id = (SELECT u FROM ref);
SELECT CASE WHEN (SELECT COUNT(*) FROM despesa) + (SELECT COUNT(*) FROM objetivo) + (SELECT COUNT(*) FROM aporte)
              + (SELECT COUNT(*) FROM simulacao) + (SELECT COUNT(*) FROM renda_vigencia)
              + (SELECT COUNT(*) FROM orcamento_categoria) = 0
            THEN 'OK    cascata usuario -> 6 tabelas filhas vazias' ELSE 'FALHA cascata usuario' END;
ROLLBACK;
