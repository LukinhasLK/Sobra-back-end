package com.Sobra.Aplicativo.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Seção 10.3 e RN-06: um usuário nunca lê nem altera dados de outro. */
class IsolamentoApiTest extends ApiTeste {

    @Test
    void usuarioBNaoEnxergaNemAlteraDadosDeA() {
        String a = novoUsuario();
        String b = novoUsuario();
        Long idA = usuarioDoToken(a);

        put("/api/v1/configuracao/renda", "{\"valor\": \"4000.00\"}", a);
        post("/api/v1/despesas", HomeEGastosApiTest.despesa("Despesa de A", "LAZER", "2026-09-10", "77.00"), a);
        post("/api/v1/simulacoes", SimulacaoApiTest.ENTRADA, a);
        String objetivoA = post("/api/v1/objetivos", "{\"nome\": \"Objetivo de A\", \"valor_meta\": \"1000.00\", \"prazo_meses\": 10}", a)
                .texto("id");
        post("/api/v1/objetivos/" + objetivoA + "/aportes", "{\"tipo\": \"EXTRA\", \"valor\": \"10.00\", \"data\": \"2026-09-01\"}", a);

        // Listas de B não trazem nada de A
        assertTrue(get("/api/v1/objetivos", b).lista("objetivos").isEmpty());
        assertTrue(get("/api/v1/despesas?mes=2026-09", b).lista("despesas").isEmpty());
        assertTrue(get("/api/v1/simulacoes", b).lista("simulacoes").isEmpty());
        Resposta homeB = get("/api/v1/home?mes=2026-09", b);
        assertEquals(null, homeB.em("renda"));
        assertEquals("0.00", homeB.texto("gastos"));

        // Ids de A: 404, igual a inexistente
        Resposta alheio = erro(get("/api/v1/objetivos/" + objetivoA, b), 404, "NAO_ENCONTRADO");
        Resposta inexistente = erro(get("/api/v1/objetivos/999999999", b), 404, "NAO_ENCONTRADO");
        assertEquals(inexistente.texto("erro.mensagem"), alheio.texto("erro.mensagem"));

        // B tenta criar aporte no objetivo de A: 404 e nada é criado
        erro(post("/api/v1/objetivos/" + objetivoA + "/aportes", "{\"tipo\": \"EXTRA\", \"valor\": \"5.00\", \"data\": \"2026-09-01\"}", b),
                404, "NAO_ENCONTRADO");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM aporte WHERE objetivo_id = ?", Integer.class,
                Long.parseLong(objetivoA)));

        // usuario_id enviado pelo cliente é ignorado
        Resposta d = post("/api/v1/despesas", """
                {"descricao": "Tentativa", "categoria_codigo": "OUTROS", "data": "2026-09-02", "valor": "1.00", "usuario_id": %d}
                """.formatted(idA), b);
        assertEquals(201, d.status());
        Long dono = jdbc.queryForObject("SELECT usuario_id FROM despesa WHERE id = ?", Long.class,
                ((java.math.BigDecimal) d.em("id")).longValue());
        assertEquals(usuarioDoToken(b), dono);
        assertEquals(1, get("/api/v1/despesas?mes=2026-09", a).lista("despesas").size());

        // Token de A depois do logout
        post("/api/v1/auth/logout", null, a);
        erro(get("/api/v1/objetivos", a), 401, "SESSAO_EXPIRADA");
    }

    @Test
    void dadosDoLucasNaoAparecemParaOutroUsuario() {
        String token = novoUsuario();
        Long reserva = jdbc.queryForObject("SELECT id FROM objetivo WHERE nome = 'Reserva de emergência'", Long.class);
        erro(get("/api/v1/objetivos/" + reserva, token), 404, "NAO_ENCONTRADO");
        assertTrue(get("/api/v1/despesas?mes=2026-09", token).lista("despesas").isEmpty());
    }
}
