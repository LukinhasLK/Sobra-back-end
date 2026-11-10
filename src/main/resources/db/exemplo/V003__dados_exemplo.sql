-- Dados de exemplo que reproduzem o prototipo (usuario de teste: Lucas)

-- Senha ficticia: o hash abaixo NAO e valido; serve apenas para preencher a coluna.
INSERT INTO usuario (nome_completo, email, senha_hash, avatar_url, criado_em, atualizado_em)
VALUES ('Lucas Carvalho', 'lucas@exemplo.com',
        '$argon2id$v=19$m=19456,t=2,p=1$ZXhlbXBsbw$ZXhlbXBsby1oYXNoLWZpY3RpY2lv',
        'https://exemplo.com/avatares/lucas.png',
        TIMESTAMP WITH TIME ZONE '2026-01-10 12:00:00+00',
        TIMESTAMP WITH TIME ZONE '2026-01-10 12:00:00+00');

-- Renda: 5.200 vale desde junho/2026 (a linha de janeiro e extra, para testar o historico)
INSERT INTO renda_vigencia (usuario_id, mes_inicio, valor)
SELECT id, DATE '2026-01-01', 4800.00 FROM usuario WHERE email = 'lucas@exemplo.com';
INSERT INTO renda_vigencia (usuario_id, mes_inicio, valor)
SELECT id, DATE '2026-06-01', 5200.00 FROM usuario WHERE email = 'lucas@exemplo.com';

-- Estimativas por categoria (passo 2 da configuracao); Saude fica sem valor (inativa)
INSERT INTO orcamento_categoria (usuario_id, categoria_id, valor_estimado)
SELECT u.id, c.id, v.valor
FROM usuario u
JOIN (VALUES ('MORADIA', 1800.00), ('ALIMENTACAO', 900.00), ('TRANSPORTE', 450.00),
             ('SAUDE', NULL), ('EDUCACAO', 350.00), ('LAZER', 300.00),
             ('OUTROS', 200.00)) AS v (codigo, valor) ON 1 = 1
JOIN categoria c ON c.codigo = v.codigo
WHERE u.email = 'lucas@exemplo.com';

-- Despesas de setembro/2026: somam 1.200 / 850 / 430 / 350 / 350 / 300 = 3.480
INSERT INTO despesa (usuario_id, categoria_id, descricao, valor, data)
SELECT u.id, c.id, v.descricao, v.valor, v.data

FROM usuario u
JOIN (VALUES
    ('MORADIA',     'Aluguel',               1200.00, DATE '2026-09-01'),
    ('ALIMENTACAO', 'Supermercado',           420.00, DATE '2026-09-03'),
    ('TRANSPORTE', 'Uber',                     85.00, DATE '2026-09-05'),
    ('SAUDE',       'Farmácia',               120.00, DATE '2026-09-07'),
    ('ALIMENTACAO', 'Feira e padaria',        274.00, DATE '2026-09-08'),
    ('TRANSPORTE', 'Combustível',             345.00, DATE '2026-09-09'),
    ('SAUDE',       'Consulta médica',        230.00, DATE '2026-09-11'),
    ('ALIMENTACAO', 'Restaurante',            156.00, DATE '2026-09-12'),
    ('OUTROS',      'Presente de aniversário', 300.00, DATE '2026-09-14'),
    ('LAZER',       'Netflix',                 39.90, DATE '2026-09-15'),
    ('LAZER',       'Cinema e jantar',        310.10, DATE '2026-09-17'),
    -- fora de setembro: servem para testar as fronteiras do filtro de mes
    ('EDUCACAO',    'Academia',                89.00, DATE '2026-08-10'),
    ('ALIMENTACAO', 'Mercado',                200.00, DATE '2026-08-31'),
    ('MORADIA',     'Aluguel (outubro)',     1200.00, DATE '2026-10-01')
) AS v (codigo, descricao, valor, data) ON 1 = 1
JOIN categoria c ON c.codigo = v.codigo
WHERE u.email = 'lucas@exemplo.com';

-- Objetivos (aporte_mensal = valor_meta / prazo_meses)
INSERT INTO objetivo (usuario_id, nome, valor_meta, prazo_meses, aporte_mensal,
                      imagem_url, criado_em)
SELECT u.id, v.nome, v.meta, v.prazo, v.aporte, v.img, v.criado
FROM usuario u
JOIN (VALUES
    ('Reserva de emergência', 15000.00, 30, 500.00, 'https://exemplo.com/img/reserva.png',
        TIMESTAMP WITH TIME ZONE '2026-01-10 12:30:00+00'),
    ('Viagem Europa',         12000.00, 24, 500.00, 'https://exemplo.com/img/viagem.png',
        TIMESTAMP WITH TIME ZONE '2026-06-01 12:00:00+00'),
    ('Notebook novo',          4500.00, 10, 450.00, 'https://exemplo.com/img/notebook.png',
        TIMESTAMP WITH TIME ZONE '2026-08-01 12:00:00+00')
) AS v (nome, meta, prazo, aporte, img, criado) ON 1 = 1
WHERE u.email = 'lucas@exemplo.com';

-- Aportes. Reserva de emergencia: os 3 aportes do prototipo somam 2.200; para o
-- acumulado ser 8.400 (regra: acumulado = soma dos aportes) ha 6 aportes antigos (6.200).
INSERT INTO aporte (objetivo_id, tipo, valor, data)
SELECT o.id, v.tipo, v.valor, v.data
FROM objetivo o
JOIN (VALUES
    ('Reserva de emergência', 'EXTRA', 3700.00, DATE '2026-01-20'),
    ('Reserva de emergência', 'MENSAL', 500.00, DATE '2026-02-10'),
    ('Reserva de emergência', 'MENSAL', 500.00, DATE '2026-03-10'),
    ('Reserva de emergência', 'MENSAL', 500.00, DATE '2026-04-10'),
    ('Reserva de emergência', 'MENSAL', 500.00, DATE '2026-05-10'),
    ('Reserva de emergência', 'MENSAL', 500.00, DATE '2026-06-10'),
    ('Reserva de emergência', 'EXTRA', 1200.00, DATE '2026-07-15'),
    ('Reserva de emergência', 'MENSAL', 500.00, DATE '2026-08-10'),
    ('Reserva de emergência', 'MENSAL', 500.00, DATE '2026-09-15'),
    ('Viagem Europa',         'EXTRA', 1200.00, DATE '2026-06-05'),
    ('Viagem Europa',         'MENSAL', 500.00, DATE '2026-07-10'),
    ('Viagem Europa',         'MENSAL', 500.00, DATE '2026-08-10'),
    ('Viagem Europa',         'EXTRA', 1000.00, DATE '2026-09-12'),
    ('Notebook novo',         'EXTRA', 1000.00, DATE '2026-08-05'),
    ('Notebook novo',         'MENSAL', 800.00, DATE '2026-09-05')
) AS v (nome, tipo, valor, data) ON v.nome = o.nome
JOIN usuario u ON u.id = o.usuario_id AND u.email = 'lucas@exemplo.com';

-- Simulacoes salvas (valores copiados do prototipo; ver nota sobre a taxa no documento)
INSERT INTO simulacao (usuario_id, valor_inicial, aporte_mensal, prazo_meses, taxa_mensal,
                       resultado, criado_em)
SELECT u.id, v.ini, v.ap, v.prazo, 0.009600, v.res, v.criado
FROM usuario u
JOIN (VALUES
    (1500.00, 300.00, 12, 5340.12, TIMESTAMP WITH TIME ZONE '2023-10-18 17:30:00+00'),
    (10000.00, 500.00, 24, 24120.44, TIMESTAMP WITH TIME ZONE '2023-10-04 17:30:00+00'),
    (500.00,   100.00, 6, 1135.80, TIMESTAMP WITH TIME ZONE '2023-09-15 17:30:00+00')
) AS v (ini, ap, prazo, res, criado) ON 1 = 1
WHERE u.email = 'lucas@exemplo.com';
