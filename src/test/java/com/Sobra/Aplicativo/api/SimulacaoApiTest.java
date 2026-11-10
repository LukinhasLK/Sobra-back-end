package com.Sobra.Aplicativo.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Etapa E7: RF-17 a RF-19 e RN-07. */
class SimulacaoApiTest extends ApiTeste {

    static final String ENTRADA = "{\"valor_inicial\": \"1000.00\", \"aporte_mensal\": \"500.00\", \"prazo_meses\": 24}";

    @Test
    void calcularReproduzOCasoDaSecao71() {
        Resposta r = post("/api/v1/simulacoes/calcular", ENTRADA, TOKEN_LUCAS);
        assertEquals(200, r.status(), r.bruta().body());
        assertEquals("0.009600", r.texto("taxa_mensal"));
        dinheiro("13000.00", r.em("total_investido"));
        dinheiro("12000.00", r.em("total_aportes"));
        dinheiro("14680.67", r.em("estimativa_final"));      // arredondar mês a mês daria 14680.66
        dinheiro("1680.67", r.em("rendimento_estimado"));
        assertEquals(25, r.lista("serie").size());

        int[] meses = {0, 1, 2, 12, 24};
        String[] com = {"1000.00", "1509.60", "2024.09", "7448.64", "14680.67"};
        String[] sem = {"1000.00", "1500.00", "2000.00", "7000.00", "13000.00"};
        for (int i = 0; i < meses.length; i++) {
            assertEquals(meses[i], ((java.math.BigDecimal) r.em("serie." + meses[i] + ".mes")).intValue());
            dinheiro(com[i], r.em("serie." + meses[i] + ".com_rendimento"));
            dinheiro(sem[i], r.em("serie." + meses[i] + ".sem_rendimento"));
        }
    }

    @Test
    void calcularNaoGravaNada() {
        String token = novoUsuario();
        post("/api/v1/simulacoes/calcular", ENTRADA, token);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM simulacao WHERE usuario_id = ?", Integer.class,
                usuarioDoToken(token)));
    }

    @Test
    void salvarRecalculaIgnoraResultadoDoClienteEGravaATaxa() {
        String token = novoUsuario();
        Resposta r = post("/api/v1/simulacoes",
                "{\"valor_inicial\": \"1000.00\", \"aporte_mensal\": \"500.00\", \"prazo_meses\": 24, \"resultado\": \"999999.99\"}",
                token);
        assertEquals(201, r.status(), r.bruta().body());
        dinheiro("14680.67", r.em("resultado"));
        assertEquals("0.009600", r.texto("taxa_mensal"));
        assertEquals("2026-09-18T15:00:00Z", r.texto("criado_em"));
        assertEquals(24, ((java.math.BigDecimal) r.em("prazo_meses")).intValue());
        String taxa = jdbc.queryForObject("SELECT taxa_mensal::text FROM simulacao WHERE id = ?", String.class,
                ((java.math.BigDecimal) r.em("id")).longValue());
        assertEquals("0.009600", taxa);
    }

    @Test
    void listarSimulacoesDoLucasDaMaisRecente() {
        Resposta r = get("/api/v1/simulacoes", TOKEN_LUCAS);
        assertEquals(200, r.status());
        assertEquals(3, r.lista("simulacoes").size());
        assertEquals("2023-10-18T17:30:00Z", r.texto("simulacoes.0.criado_em"));
        dinheiro("1500.00", r.em("simulacoes.0.valor_inicial"));
        dinheiro("5340.12", r.em("simulacoes.0.resultado"));
        assertEquals("2023-09-15T17:30:00Z", r.texto("simulacoes.2.criado_em"));

        assertEquals(2, get("/api/v1/simulacoes?limite=2", TOKEN_LUCAS).lista("simulacoes").size());
        erro(get("/api/v1/simulacoes?limite=0", TOKEN_LUCAS), 400, "REQUISICAO_INVALIDA");
        erro(get("/api/v1/simulacoes?limite=101", TOKEN_LUCAS), 400, "REQUISICAO_INVALIDA");
    }

    @Test
    void validacoesDaSimulacao() {
        erroDeCampo(post("/api/v1/simulacoes/calcular",
                "{\"valor_inicial\": \"0\", \"aporte_mensal\": \"0\", \"prazo_meses\": 12}", TOKEN_LUCAS),
                "aporte_mensal", "DEVE_SER_POSITIVO");
        erroDeCampo(post("/api/v1/simulacoes/calcular",
                "{\"valor_inicial\": \"10\", \"aporte_mensal\": \"10\", \"prazo_meses\": 121}", TOKEN_LUCAS),
                "prazo_meses", "FORA_DO_LIMITE");
        erroDeCampo(post("/api/v1/simulacoes/calcular",
                "{\"valor_inicial\": \"-1\", \"aporte_mensal\": \"10\", \"prazo_meses\": 12}", TOKEN_LUCAS),
                "valor_inicial", "FORA_DO_LIMITE");
        // valor inicial zero com aporte é válido
        assertEquals(200, post("/api/v1/simulacoes/calcular",
                "{\"valor_inicial\": \"0\", \"aporte_mensal\": \"100\", \"prazo_meses\": 120}", TOKEN_LUCAS).status());
    }
}
