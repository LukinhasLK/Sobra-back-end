package com.Sobra.Aplicativo.api;

import com.Sobra.Aplicativo.seguranca.LimiteTaxa;
import com.Sobra.Aplicativo.seguranca.Tokens;
import com.Sobra.Aplicativo.service.EnvioEmail;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base dos testes de API: sobe a aplicação inteira numa porta aleatória, contra o banco
 * sobra_test (PostgreSQL), com V001 + V002 + V003 aplicados e relógio fixo em 2026-09-18.
 *
 * Os dados do Lucas (V003) são só lidos. Testes que gravam criam usuários novos,
 * com e-mail aleatório, então a suíte pode rodar várias vezes no mesmo banco.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class ApiTeste {

    /** Token fixo de uma sessão do Lucas, criada direto no banco (o hash do V003 é fictício e não serve para login). */
    protected static final String TOKEN_LUCAS = "token-de-teste-do-lucas-nao-use-fora-dos-testes";
    protected static final String SENHA = "UmaSenhaBoa#2026";

    private static final HttpClient CLIENTE = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Autowired
    protected Environment env;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected LimiteTaxa limiteTaxa;
    @Autowired
    protected EnvioEmail envioEmail;

    protected record Resposta(int status, Object corpo, HttpResponse<String> bruta) {

        public Object em(String caminho) {
            return Json.em(corpo, caminho);
        }

        public String texto(String caminho) {
            Object v = em(caminho);
            return v == null ? null : v.toString();
        }

        public List<?> lista(String caminho) {
            return (List<?>) em(caminho);
        }

        public String cabecalho(String nome) {
            return bruta.headers().firstValue(nome).orElse(null);
        }
    }

    @BeforeEach
    void prepararBase() {
        limiteTaxa.zerar();
        Long lucas = idLucas();
        jdbc.update("DELETE FROM sessao WHERE token_hash = ?", Tokens.hash(TOKEN_LUCAS));
        jdbc.update("""
                INSERT INTO sessao (usuario_id, token_hash, expira_em, criado_em)
                VALUES (?, ?, TIMESTAMP WITH TIME ZONE '2026-10-18 15:00:00+00', TIMESTAMP WITH TIME ZONE '2026-09-18 15:00:00+00')
                """, lucas, Tokens.hash(TOKEN_LUCAS));
    }

    protected Long idLucas() {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'lucas@exemplo.com'", Long.class);
    }

    // ------------------------------------------------------------------ HTTP

    protected Resposta get(String caminho, String token) {
        return enviar("GET", caminho, null, token);
    }

    protected Resposta post(String caminho, String json, String token) {
        return enviar("POST", caminho, json, token);
    }

    protected Resposta put(String caminho, String json, String token) {
        return enviar("PUT", caminho, json, token);
    }

    protected Resposta enviar(String metodo, String caminho, String json, String token) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + porta() + caminho));
            if (token != null) {
                b.header("Authorization", "Bearer " + token);
            }
            if (json != null) {
                b.header("Content-Type", "application/json");
                b.method(metodo, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            } else {
                b.method(metodo, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> r = CLIENTE.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resposta(r.statusCode(), Json.ler(r.body()), r);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String porta() {
        return env.getProperty("local.server.port");
    }

    // ------------------------------------------------------------------ atalhos

    protected static String emailNovo() {
        return "teste-" + UUID.randomUUID() + "@exemplo.com";
    }

    /** Cadastra um usuário novo e devolve o token. */
    protected String novoUsuario() {
        return novoUsuario("Pessoa de Teste", emailNovo());
    }

    protected String novoUsuario(String nome, String email) {
        Resposta r = post("/api/v1/auth/cadastro", """
                {"nome_completo": "%s", "email": "%s", "senha": "%s", "confirmacao_senha": "%s"}
                """.formatted(nome, email, SENHA, SENHA), null);
        assertEquals(201, r.status(), () -> "cadastro falhou: " + r.bruta().body());
        return r.texto("sessao.token");
    }

    protected Long usuarioDoToken(String token) {
        return jdbc.queryForObject("SELECT usuario_id FROM sessao WHERE token_hash = ?", Long.class, Tokens.hash(token));
    }

    /** Confere o formato único de erro (seção 6.2) e devolve a resposta para mais asserções. */
    protected Resposta erro(Resposta r, int status, String codigo) {
        assertEquals(status, r.status(), () -> "corpo: " + r.bruta().body());
        assertTrue(r.corpo() instanceof Map<?, ?>, "erro sem corpo JSON");
        assertEquals(codigo, r.texto("erro.codigo"));
        assertNotNull(r.texto("erro.mensagem"));
        assertTrue(r.em("erro.campos") instanceof List<?>, "erro.campos deve ser lista");
        assertNotNull(r.texto("erro.id_requisicao"));
        assertTrue(r.bruta().headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        return r;
    }

    /** Erro 422 com o campo e código esperados em "campos". */
    protected void erroDeCampo(Resposta r, String campo, String codigoCampo) {
        erro(r, 422, "VALIDACAO");
        boolean achou = r.lista("erro.campos").stream()
                .map(c -> (Map<?, ?>) c)
                .anyMatch(c -> campo.equals(c.get("campo")) && codigoCampo.equals(c.get("codigo")));
        assertTrue(achou, () -> "esperado " + campo + "/" + codigoCampo + " em " + r.bruta().body());
    }

    /** Dinheiro sempre string com 2 casas (RN-01). */
    protected static void dinheiro(String esperado, Object valor) {
        assertTrue(valor instanceof String, () -> "dinheiro deve ser string: " + valor);
        assertTrue(((String) valor).matches("-?\\d+\\.\\d{2}"), () -> "formato inválido: " + valor);
        assertEquals(0, new BigDecimal(esperado).compareTo(new BigDecimal((String) valor)), () -> esperado + " != " + valor);
        assertEquals(esperado, valor);
    }
}
