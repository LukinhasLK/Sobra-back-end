package com.Sobra.Aplicativo.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Etapa E6: RF-13 a RF-16 com os casos RN-03, RN-04 e RN-08 (referência 2026-09). */
class ObjetivosApiTest extends ApiTeste {

    @Test
    void listaDoLucasComAcumuladoEPercentual() {
        Resposta r = get("/api/v1/objetivos", TOKEN_LUCAS);
        assertEquals(200, r.status());
        assertEquals(3, r.lista("objetivos").size());
        String[] nomes = {"Reserva de emergência", "Viagem Europa", "Notebook novo"};
        String[] acumulados = {"8400.00", "3200.00", "1800.00"};
        String[] metas = {"15000.00", "12000.00", "4500.00"};
        String[] percentuais = {"56.00", "26.67", "40.00"};
        for (int i = 0; i < 3; i++) {
            assertEquals(nomes[i], r.texto("objetivos." + i + ".nome"));
            dinheiro(acumulados[i], r.em("objetivos." + i + ".valor_acumulado"));
            dinheiro(metas[i], r.em("objetivos." + i + ".valor_meta"));
            assertEquals(percentuais[i], r.texto("objetivos." + i + ".percentual_concluido"));
        }
        assertEquals("https://exemplo.com/img/reserva.png", r.texto("objetivos.0.imagem_url"));
    }

    @Test
    void detalheDaReservaComHistoricoEPrevisao() {
        Long id = jdbc.queryForObject("SELECT id FROM objetivo WHERE nome = 'Reserva de emergência'", Long.class);
        Resposta r = get("/api/v1/objetivos/" + id, TOKEN_LUCAS);
        assertEquals(200, r.status(), r.bruta().body());
        dinheiro("8400.00", r.em("valor_acumulado"));
        assertEquals("56.00", r.texto("percentual_concluido"));
        assertEquals(30, ((java.math.BigDecimal) r.em("prazo_meses")).intValue());
        dinheiro("500.00", r.em("aporte_mensal"));
        assertEquals("2027-11", r.texto("previsao_conclusao"));     // 6600 ÷ 500 = 13,2 → 14 meses
        assertEquals(false, r.em("concluido"));
        assertEquals(9, r.lista("aportes").size());
        assertEquals("2026-09-15", r.texto("aportes.0.data"));
        assertEquals("MENSAL", r.texto("aportes.0.tipo"));
        assertEquals("Aporte Mensal", r.texto("aportes.0.titulo"));
        assertEquals("Aporte Extra", r.texto("aportes.2.titulo"));
        assertEquals("2026-01-20", r.texto("aportes.8.data"));
    }

    @Test
    void previsoesDosOutrosObjetivosDoLucas() {
        Long viagem = jdbc.queryForObject("SELECT id FROM objetivo WHERE nome = 'Viagem Europa'", Long.class);
        Long notebook = jdbc.queryForObject("SELECT id FROM objetivo WHERE nome = 'Notebook novo'", Long.class);
        assertEquals("2028-03", get("/api/v1/objetivos/" + viagem, TOKEN_LUCAS).texto("previsao_conclusao"));
        assertEquals("2027-03", get("/api/v1/objetivos/" + notebook, TOKEN_LUCAS).texto("previsao_conclusao"));
    }

    @Test
    void criarObjetivoCalculaAporteEPrevisao() {
        String token = novoUsuario();
        Resposta r = post("/api/v1/objetivos",
                "{\"nome\": \"Reserva de Emergência\", \"valor_meta\": \"15000.00\", \"prazo_meses\": 18, \"aporte_mensal\": \"1.00\"}",
                token);
        assertEquals(201, r.status(), r.bruta().body());
        dinheiro("833.33", r.em("aporte_mensal"));                // ignora o 1.00 enviado
        assertEquals("2028-03", r.texto("previsao_conclusao"));   // 18,000072 → 18,00 → 18
        dinheiro("0.00", r.em("valor_acumulado"));
        assertEquals("0.00", r.texto("percentual_concluido"));
        assertEquals(false, r.em("concluido"));
        assertNull(r.em("imagem_url"));

        Resposta lista = get("/api/v1/objetivos", token);
        assertEquals(1, lista.lista("objetivos").size());
    }

    @Test
    void aporteAtualizaAcumuladoPercentualEPrevisao() {
        String token = novoUsuario();
        Resposta obj = post("/api/v1/objetivos", "{\"nome\": \"Reserva\", \"valor_meta\": \"15000.00\", \"prazo_meses\": 30}", token);
        String id = obj.texto("id");
        dinheiro("500.00", obj.em("aporte_mensal"));

        assertEquals(201, post("/api/v1/objetivos/" + id + "/aportes",
                "{\"tipo\": \"EXTRA\", \"valor\": \"8400.00\", \"data\": \"2026-09-01\"}", token).status());
        Resposta r = post("/api/v1/objetivos/" + id + "/aportes",
                "{\"tipo\": \"EXTRA\", \"valor\": \"300.00\", \"data\": \"2026-09-18\"}", token);
        assertEquals(201, r.status(), r.bruta().body());
        assertEquals("EXTRA", r.texto("aporte.tipo"));
        assertEquals("Aporte Extra", r.texto("aporte.titulo"));
        assertEquals("2026-09-18", r.texto("aporte.data"));
        dinheiro("300.00", r.em("aporte.valor"));
        dinheiro("8700.00", r.em("objetivo.valor_acumulado"));
        assertEquals("58.00", r.texto("objetivo.percentual_concluido"));
        assertEquals("2027-10", r.texto("objetivo.previsao_conclusao"));   // 6300 ÷ 500 = 12,6 → 13
        assertEquals(false, r.em("objetivo.concluido"));

        Resposta detalhe = get("/api/v1/objetivos/" + id, token);
        assertEquals("2026-09-18", detalhe.texto("aportes.0.data"));      // mais recente primeiro
        assertEquals(2, detalhe.lista("aportes").size());
    }

    @Test
    void objetivoConcluidoTemPrevisaoNulaE100PorCento() {
        String token = novoUsuario();
        String id = post("/api/v1/objetivos", "{\"nome\": \"Meta\", \"valor_meta\": \"15000.00\", \"prazo_meses\": 10}", token).texto("id");
        Resposta r = post("/api/v1/objetivos/" + id + "/aportes",
                "{\"tipo\": \"MENSAL\", \"valor\": \"16000.00\", \"data\": \"2026-09-10\"}", token);
        assertEquals("100.00", r.texto("objetivo.percentual_concluido"));
        assertEquals(true, r.em("objetivo.concluido"));
        assertNull(r.em("objetivo.previsao_conclusao"));
        assertEquals("100.00", get("/api/v1/objetivos", token).texto("objetivos.0.percentual_concluido"));
    }

    @Test
    void listaVaziaDevolve200() {
        Resposta r = get("/api/v1/objetivos", novoUsuario());
        assertEquals(200, r.status());
        assertTrue(r.lista("objetivos").isEmpty());
        assertEquals("{\"objetivos\":[]}", r.bruta().body().replace(" ", ""));
    }

    @Test
    void validacoesDeObjetivoEAporte() {
        String token = novoUsuario();
        erroDeCampo(post("/api/v1/objetivos", "{\"nome\": \"X\", \"valor_meta\": \"100.00\", \"prazo_meses\": 361}", token),
                "prazo_meses", "FORA_DO_LIMITE");
        erroDeCampo(post("/api/v1/objetivos", "{\"nome\": \"X\", \"valor_meta\": \"100.00\", \"prazo_meses\": 0}", token),
                "prazo_meses", "FORA_DO_LIMITE");
        erroDeCampo(post("/api/v1/objetivos", "{\"nome\": \"X\", \"valor_meta\": \"0\", \"prazo_meses\": 5}", token),
                "valor_meta", "DEVE_SER_POSITIVO");
        erroDeCampo(post("/api/v1/objetivos", "{\"nome\": \"\", \"valor_meta\": \"10.00\", \"prazo_meses\": 5}", token),
                "nome", "OBRIGATORIO");
        erroDeCampo(post("/api/v1/objetivos", "{\"nome\": \"X\", \"valor_meta\": \"10.00\", \"prazo_meses\": 2.5}", token),
                "prazo_meses", "FORMATO_INVALIDO");

        String id = post("/api/v1/objetivos", "{\"nome\": \"Ok\", \"valor_meta\": \"100.00\", \"prazo_meses\": 5}", token).texto("id");
        erroDeCampo(post("/api/v1/objetivos/" + id + "/aportes", "{\"tipo\": \"X\", \"valor\": \"1.00\", \"data\": \"2026-09-01\"}", token),
                "tipo", "FORMATO_INVALIDO");
        erroDeCampo(post("/api/v1/objetivos/" + id + "/aportes", "{\"tipo\": \"EXTRA\", \"valor\": \"0\", \"data\": \"2026-09-01\"}", token),
                "valor", "DEVE_SER_POSITIVO");
        erroDeCampo(post("/api/v1/objetivos/" + id + "/aportes", "{\"tipo\": \"EXTRA\", \"valor\": \"1.00\", \"data\": \"2027-01-01\"}", token),
                "data", "DATA_INVALIDA");
    }

    @Test
    void objetivoInexistenteDevolve404() {
        erro(get("/api/v1/objetivos/999999999", TOKEN_LUCAS), 404, "NAO_ENCONTRADO");
        erro(get("/api/v1/objetivos/abc", TOKEN_LUCAS), 404, "NAO_ENCONTRADO");
        erro(post("/api/v1/objetivos/999999999/aportes", "{\"tipo\": \"EXTRA\", \"valor\": \"1.00\", \"data\": \"2026-09-01\"}",
                TOKEN_LUCAS), 404, "NAO_ENCONTRADO");
    }
}
