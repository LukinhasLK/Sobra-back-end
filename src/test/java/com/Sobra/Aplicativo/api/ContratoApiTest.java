package com.Sobra.Aplicativo.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Seção 6 e 10.4: formato único de erro, saúde, corpo grande, JSON malformado, limite geral. */
class ContratoApiTest extends ApiTeste {

    @Test
    void saudeEhPublicaERespondeOk() {
        Resposta r = get("/api/v1/saude", null);
        assertEquals(200, r.status());
        assertEquals("ok", r.texto("status"));
        assertNotNull(r.cabecalho("X-Id-Requisicao"));
    }

    @Test
    void jsonMalformadoDevolve400() {
        erro(post("/api/v1/despesas", "{\"descricao\": ", TOKEN_LUCAS), 400, "REQUISICAO_INVALIDA");
        erro(post("/api/v1/auth/login", "nao e json", null), 400, "REQUISICAO_INVALIDA");
    }

    @Test
    void corpoMaiorQue10KbDevolve400() {
        String grande = "{\"descricao\": \"" + "x".repeat(11 * 1024) + "\"}";
        erro(post("/api/v1/despesas", grande, TOKEN_LUCAS), 400, "REQUISICAO_INVALIDA");
    }

    @Test
    void rotaInexistenteDevolve404NoFormatoUnico() {
        erro(get("/api/v1/nao-existe", TOKEN_LUCAS), 404, "NAO_ENCONTRADO");
    }

    @Test
    void tipoErradoNoCampoViraValidacaoNoCampo() {
        erroDeCampo(put("/api/v1/perfil/notificacoes", "{\"notificacoes_ativas\": \"sim\"}", TOKEN_LUCAS),
                "notificacoes_ativas", "FORMATO_INVALIDO");
        erroDeCampo(post("/api/v1/despesas",
                "{\"descricao\": \"x\", \"categoria_codigo\": \"OUTROS\", \"data\": \"2026-09-01\", \"valor\": true}", TOKEN_LUCAS),
                "valor", "FORMATO_INVALIDO");
    }

    @Test
    void notificacoesLigaDesligaEPersiste() {
        String token = novoUsuario();
        assertEquals(true, get("/api/v1/perfil", token).em("notificacoes_ativas"));
        Resposta r = put("/api/v1/perfil/notificacoes", "{\"notificacoes_ativas\": false}", token);
        assertEquals(200, r.status());
        assertEquals(false, r.em("notificacoes_ativas"));
        assertEquals(200, put("/api/v1/perfil/notificacoes", "{\"notificacoes_ativas\": false}", token).status());
        assertEquals(false, get("/api/v1/perfil", token).em("notificacoes_ativas"));
    }

    @Test
    void limiteGeralDe120RequisicoesPorMinutoPorToken() {
        String token = novoUsuario();
        for (int i = 0; i < 120; i++) {
            assertEquals(200, get("/api/v1/categorias", token).status());
        }
        Resposta r = erro(get("/api/v1/categorias", token), 429, "LIMITE_REQUISICOES");
        assertNotNull(r.cabecalho("Retry-After"));
    }
}
