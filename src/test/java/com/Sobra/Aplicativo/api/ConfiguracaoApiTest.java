package com.Sobra.Aplicativo.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Etapa E3: RF-07, RF-08, RF-09 e RF-22. */
class ConfiguracaoApiTest extends ApiTeste {

    static final String SETE = """
            {"categorias": [
              {"codigo": "MORADIA", "valor_estimado": "1800.00"},
              {"codigo": "ALIMENTACAO", "valor_estimado": "900.00"},
              {"codigo": "TRANSPORTE", "valor_estimado": "450.00"},
              {"codigo": "SAUDE", "valor_estimado": null},
              {"codigo": "EDUCACAO", "valor_estimado": "350.00"},
              {"codigo": "LAZER", "valor_estimado": "300.00"},
              {"codigo": "OUTROS", "valor_estimado": "200.00"}
            ]}""";

    @Test
    void fluxoCompletoDaConfiguracao() {
        String token = novoUsuario();
        Resposta antes = get("/api/v1/configuracao", token);
        assertEquals(200, antes.status());
        assertNull(antes.em("renda_mensal"));
        assertEquals(false, antes.em("etapa1_concluida"));
        assertEquals(false, antes.em("etapa2_concluida"));
        assertEquals(false, antes.em("concluida"));
        assertEquals(7, antes.lista("categorias").size());

        Resposta renda = put("/api/v1/configuracao/renda", "{\"valor\": \"5200.00\"}", token);
        assertEquals(200, renda.status(), renda.bruta().body());
        dinheiro("5200.00", renda.em("renda_mensal"));
        assertEquals("2026-09", renda.texto("vigente_desde"));

        Resposta cat = put("/api/v1/configuracao/categorias", SETE, token);
        assertEquals(200, cat.status(), cat.bruta().body());
        assertEquals(7, cat.lista("categorias").size());
        assertEquals("Moradia", cat.texto("categorias.0.nome"));
        assertNull(cat.em("categorias.3.valor_estimado"));      // Saúde inativa = nulo

        Resposta depois = get("/api/v1/configuracao", token);
        assertEquals(true, depois.em("etapa1_concluida"));
        assertEquals(true, depois.em("etapa2_concluida"));
        assertEquals(true, depois.em("concluida"));
        dinheiro("5200.00", depois.em("renda_mensal"));

        Long id = usuarioDoToken(token);
        String email = jdbc.queryForObject("SELECT email FROM usuario WHERE id = ?", String.class, id);
        Resposta login = post("/api/v1/auth/login", "{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, SENHA), null);
        assertEquals(true, login.em("configuracao_concluida"));
    }

    @Test
    void rendaNoMesmoMesSubstituiEMesAnteriorNaoMuda() {
        String token = novoUsuario();
        Long id = usuarioDoToken(token);
        jdbc.update("INSERT INTO renda_vigencia (usuario_id, mes_inicio, valor) VALUES (?, DATE '2026-03-01', 3000.00)", id);

        put("/api/v1/configuracao/renda", "{\"valor\": \"5000.00\"}", token);
        put("/api/v1/configuracao/renda", "{\"valor\": \"5500.00\"}", token);

        List<Map<String, Object>> linhas = jdbc.queryForList(
                "SELECT mes_inicio::text AS mes, valor::text AS valor FROM renda_vigencia WHERE usuario_id = ? ORDER BY mes_inicio", id);
        assertEquals(2, linhas.size());
        assertEquals("2026-03-01", linhas.get(0).get("mes"));
        assertEquals("3000.00", linhas.get(0).get("valor"));
        assertEquals("2026-09-01", linhas.get(1).get("mes"));
        assertEquals("5500.00", linhas.get(1).get("valor"));

        // Home de um mês antigo continua usando a renda antiga
        dinheiro("3000.00", get("/api/v1/home?mes=2026-05", token).em("renda"));
    }

    @Test
    void rendaInvalidaDevolve422() {
        String token = novoUsuario();
        erroDeCampo(put("/api/v1/configuracao/renda", "{\"valor\": \"0\"}", token), "valor", "DEVE_SER_POSITIVO");
        erroDeCampo(put("/api/v1/configuracao/renda", "{\"valor\": \"-10.00\"}", token), "valor", "DEVE_SER_POSITIVO");
        erroDeCampo(put("/api/v1/configuracao/renda", "{}", token), "valor", "OBRIGATORIO");
        erroDeCampo(put("/api/v1/configuracao/renda", "{\"valor\": \"10.123\"}", token), "valor", "FORMATO_INVALIDO");
    }

    @Test
    void categoriasIncompletasRepetidasOuInvalidasNaoGravamNada() {
        String token = novoUsuario();
        Long id = usuarioDoToken(token);

        String seis = SETE.replace("{\"codigo\": \"OUTROS\", \"valor_estimado\": \"200.00\"}", "")
                .replace("\"300.00\"},\n", "\"300.00\"}\n");
        erroDeCampo(put("/api/v1/configuracao/categorias", seis, token), "categorias", "CATEGORIAS_INCOMPLETAS");

        String repetida = SETE.replace("\"OUTROS\"", "\"LAZER\"");
        erroDeCampo(put("/api/v1/configuracao/categorias", repetida, token), "categorias", "CATEGORIAS_INCOMPLETAS");

        String oito = SETE.replace("]}", ", {\"codigo\": \"MORADIA\", \"valor_estimado\": \"1.00\"}]}");
        erroDeCampo(put("/api/v1/configuracao/categorias", oito, token), "categorias", "CATEGORIAS_INCOMPLETAS");

        String desconhecida = SETE.replace("\"OUTROS\"", "\"PETS\"");
        erroDeCampo(put("/api/v1/configuracao/categorias", desconhecida, token), "categorias[6].codigo", "CATEGORIA_DESCONHECIDA");

        String zero = SETE.replace("\"1800.00\"", "\"0.00\"");
        erroDeCampo(put("/api/v1/configuracao/categorias", zero, token), "categorias[0].valor_estimado", "DEVE_SER_POSITIVO");

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM orcamento_categoria WHERE usuario_id = ?", Integer.class, id));
    }

    @Test
    void categoriasDeReferenciaNaOrdemDasTelas() {
        Resposta r = get("/api/v1/categorias", TOKEN_LUCAS);
        assertEquals(200, r.status());
        List<?> lista = r.lista("categorias");
        assertEquals(7, lista.size());
        String[] nomes = {"Moradia", "Alimentação", "Transporte", "Saúde", "Educação", "Lazer", "Outros"};
        for (int i = 0; i < nomes.length; i++) {
            assertEquals(nomes[i], r.texto("categorias." + i + ".nome"));
        }
        assertEquals("EDUCACAO", r.texto("categorias.4.codigo"));
    }

    @Test
    void configuracaoDoLucasEstaCompleta() {
        Resposta r = get("/api/v1/configuracao", TOKEN_LUCAS);
        assertEquals(true, r.em("concluida"));
        dinheiro("5200.00", r.em("renda_mensal"));
        dinheiro("1800.00", r.em("categorias.0.valor_estimado"));
        assertNull(r.em("categorias.3.valor_estimado"));
    }
}
