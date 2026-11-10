package com.Sobra.Aplicativo.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Etapas E4 e E5: RF-10, RF-11, RF-12 e RN-02 / RN-05. */
class HomeEGastosApiTest extends ApiTeste {

    @Test
    void homeDeSetembroComOsNumerosDoPrototipo() {
        Resposta r = get("/api/v1/home?mes=2026-09", TOKEN_LUCAS);
        assertEquals(200, r.status(), r.bruta().body());
        assertEquals("2026-09", r.texto("mes"));
        assertEquals("Lucas", r.texto("usuario.primeiro_nome"));
        assertEquals("https://exemplo.com/avatares/lucas.png", r.texto("usuario.avatar_url"));
        dinheiro("5200.00", r.em("renda"));
        dinheiro("3480.00", r.em("gastos"));
        dinheiro("1720.00", r.em("disponivel"));

        String[] codigos = {"MORADIA", "ALIMENTACAO", "TRANSPORTE", "SAUDE", "LAZER", "OUTROS"};
        String[] valores = {"1200.00", "850.00", "430.00", "350.00", "350.00", "300.00"};
        assertEquals(6, r.lista("categorias").size());
        for (int i = 0; i < codigos.length; i++) {
            assertEquals(codigos[i], r.texto("categorias." + i + ".codigo"));  // Saúde antes de Lazer no empate
            dinheiro(valores[i], r.em("categorias." + i + ".valor"));
        }
    }

    @Test
    void homeSemMesUsaOMesCorrenteDeSaoPaulo() {
        assertEquals("2026-09", get("/api/v1/home", TOKEN_LUCAS).texto("mes"));
    }

    @Test
    void homeDeMesSemDespesasESemRenda() {
        Resposta semDespesas = get("/api/v1/home?mes=2026-07", TOKEN_LUCAS);
        dinheiro("5200.00", semDespesas.em("renda"));
        dinheiro("0.00", semDespesas.em("gastos"));
        dinheiro("5200.00", semDespesas.em("disponivel"));
        assertTrue(semDespesas.lista("categorias").isEmpty());

        Resposta semRenda = get("/api/v1/home?mes=2025-12", TOKEN_LUCAS);
        assertNull(semRenda.em("renda"));
        assertNull(semRenda.em("disponivel"));
        dinheiro("0.00", semRenda.em("gastos"));

        dinheiro("4800.00", get("/api/v1/home?mes=2026-05", TOKEN_LUCAS).em("renda")); // histórico da renda
    }

    @Test
    void disponivelPodeSerNegativo() {
        String token = novoUsuario();
        put("/api/v1/configuracao/renda", "{\"valor\": \"3000.00\"}", token);
        post("/api/v1/despesas", despesa("Aluguel", "MORADIA", "2026-09-01", "3480.00"), token);
        dinheiro("-480.00", get("/api/v1/home?mes=2026-09", token).em("disponivel"));
    }

    @Test
    void mesInvalidoDevolve400() {
        erro(get("/api/v1/home?mes=2026-13", TOKEN_LUCAS), 400, "REQUISICAO_INVALIDA");
        erro(get("/api/v1/home?mes=setembro", TOKEN_LUCAS), 400, "REQUISICAO_INVALIDA");
        erro(get("/api/v1/despesas?mes=2026-9", TOKEN_LUCAS), 400, "REQUISICAO_INVALIDA");
    }

    @Test
    void despesasDeSetembroRespeitamAsFronteirasDoMes() {
        Resposta r = get("/api/v1/despesas?mes=2026-09", TOKEN_LUCAS);
        assertEquals(200, r.status());
        assertEquals("2026-09", r.texto("mes"));
        dinheiro("3480.00", r.em("total"));
        List<?> lista = r.lista("despesas");
        assertEquals(11, lista.size());
        assertEquals("Aluguel", r.texto("despesas.0.descricao"));
        assertEquals("2026-09-01", r.texto("despesas.0.data"));
        assertEquals("MORADIA", r.texto("despesas.0.categoria.codigo"));
        assertEquals("Moradia", r.texto("despesas.0.categoria.nome"));
        dinheiro("1200.00", r.em("despesas.0.valor"));
        assertEquals("Cinema e jantar", r.texto("despesas.10.descricao"));
        String corpo = r.bruta().body();
        assertTrue(!corpo.contains("2026-08-31") && !corpo.contains("2026-10-01"), "vazou despesa de fora do mês");

        // total igual à soma da lista
        java.math.BigDecimal soma = java.math.BigDecimal.ZERO;
        for (int i = 0; i < lista.size(); i++) {
            soma = soma.add(new java.math.BigDecimal(r.texto("despesas." + i + ".valor")));
        }
        assertEquals(new java.math.BigDecimal("3480.00"), soma);
    }

    @Test
    void mesSemDespesasDevolve200ComListaVazia() {
        Resposta r = get("/api/v1/despesas?mes=2025-01", TOKEN_LUCAS);
        assertEquals(200, r.status());
        dinheiro("0.00", r.em("total"));
        assertTrue(r.lista("despesas").isEmpty());
    }

    @Test
    void criarDespesaApareceNaListagem() {
        String token = novoUsuario();
        Resposta r = post("/api/v1/despesas", despesa("Supermercado Mensal", "ALIMENTACAO", "2026-09-18", "120.00"), token);
        assertEquals(201, r.status(), r.bruta().body());
        assertEquals("Supermercado Mensal", r.texto("descricao"));
        assertEquals("Alimentação", r.texto("categoria.nome"));
        assertEquals("2026-09-18", r.texto("data"));
        dinheiro("120.00", r.em("valor"));

        Resposta lista = get("/api/v1/despesas?mes=2026-09", token);
        assertEquals(1, lista.lista("despesas").size());
        assertEquals(r.em("id"), lista.em("despesas.0.id"));
        dinheiro("120.00", lista.em("total"));
    }

    @Test
    void despesaInvalidaDevolve422() {
        String token = novoUsuario();
        erroDeCampo(post("/api/v1/despesas", despesa("X", "MORADIA", "2026-09-01", "0"), token), "valor", "DEVE_SER_POSITIVO");
        erroDeCampo(post("/api/v1/despesas", despesa("X", "MORADIA", "2026-09-01", "-5.00"), token), "valor", "DEVE_SER_POSITIVO");
        erroDeCampo(post("/api/v1/despesas", despesa("X", "MORADIA", "2026-09-01", "1.999"), token), "valor", "FORMATO_INVALIDO");
        erroDeCampo(post("/api/v1/despesas", despesa("X", "MORADIA", "2026-09-01", "100000000.00"), token), "valor", "FORA_DO_LIMITE");
        erroDeCampo(post("/api/v1/despesas", despesa("X", "PETS", "2026-09-01", "10.00"), token), "categoria_codigo", "CATEGORIA_DESCONHECIDA");
        erroDeCampo(post("/api/v1/despesas", despesa("X", "MORADIA", "2026-09-19", "10.00"), token), "data", "DATA_INVALIDA");
        erroDeCampo(post("/api/v1/despesas", despesa("X", "MORADIA", "1999-12-31", "10.00"), token), "data", "DATA_INVALIDA");
        erroDeCampo(post("/api/v1/despesas", despesa("X", "MORADIA", "18/09/2026", "10.00"), token), "data", "DATA_INVALIDA");
        erroDeCampo(post("/api/v1/despesas", despesa("   ", "MORADIA", "2026-09-01", "10.00"), token), "descricao", "OBRIGATORIO");
        erroDeCampo(post("/api/v1/despesas", despesa("x".repeat(121), "MORADIA", "2026-09-01", "10.00"), token), "descricao", "FORA_DO_LIMITE");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM despesa WHERE usuario_id = ?", Integer.class, usuarioDoToken(token)));
    }

    static String despesa(String descricao, String categoria, String data, String valor) {
        return """
                {"descricao": "%s", "categoria_codigo": "%s", "data": "%s", "valor": "%s"}
                """.formatted(descricao, categoria, data, valor);
    }
}
