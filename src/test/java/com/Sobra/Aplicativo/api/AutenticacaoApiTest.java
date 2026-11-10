package com.Sobra.Aplicativo.api;

import com.Sobra.Aplicativo.seguranca.Tokens;
import com.Sobra.Aplicativo.service.EnvioEmail;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Etapas E2 e E8: RF-01 a RF-06 (seção 10.1). */
class AutenticacaoApiTest extends ApiTeste {

    @Test
    void cadastroValidoDevolveUsuarioTokenEConfiguracaoPendente() {
        String email = emailNovo();
        Resposta r = post("/api/v1/auth/cadastro", """
                {"nome_completo": "Ana Maria de Souza", "email": "  %s ", "senha": "%s", "confirmacao_senha": "%s"}
                """.formatted(email.toUpperCase(), SENHA, SENHA), null);
        assertEquals(201, r.status(), r.bruta().body());
        assertEquals(email, r.texto("usuario.email"));            // normalizado: sem espaços, minúsculo
        assertEquals("AS", r.texto("usuario.iniciais"));
        assertNotNull(r.em("usuario.id"));
        assertEquals(43, r.texto("sessao.token").length());       // 32 bytes em Base64 URL
        assertEquals("2026-10-18T15:00:00Z", r.texto("sessao.expira_em"));
        assertEquals(false, r.em("configuracao_concluida"));

        // A senha só existe como hash Argon2id; o token só como SHA-256.
        String hash = jdbc.queryForObject("SELECT senha_hash FROM usuario WHERE email = ?", String.class, email);
        assertTrue(hash.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"), hash);
        assertFalse(hash.contains(SENHA));
        Integer sessoes = jdbc.queryForObject("SELECT COUNT(*) FROM sessao WHERE token_hash = ?", Integer.class,
                Tokens.hash(r.texto("sessao.token")));
        assertEquals(1, sessoes);
        assertFalse(r.bruta().body().contains(SENHA));
    }

    @Test
    void emailRepetidoMesmoComMaiusculasDevolve409ENaoCria() {
        String email = emailNovo();
        novoUsuario("Primeira Pessoa", email);
        Resposta r = post("/api/v1/auth/cadastro", """
                {"nome_completo": "Outra Pessoa", "email": "%s", "senha": "%s", "confirmacao_senha": "%s"}
                """.formatted(email.toUpperCase(), SENHA, SENHA), null);
        erro(r, 409, "EMAIL_JA_CADASTRADO");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE email = ?", Integer.class, email));
    }

    @Test
    void senhaDiferenteDaConfirmacaoDevolve422NoCampoConfirmacao() {
        Resposta r = post("/api/v1/auth/cadastro", """
                {"nome_completo": "Fulano Tal", "email": "%s", "senha": "%s", "confirmacao_senha": "outraSenha99"}
                """.formatted(emailNovo(), SENHA), null);
        erroDeCampo(r, "confirmacao_senha", "SENHAS_DIFERENTES");
    }

    @Test
    void cadastroComCamposInvalidosListaTodosOsCampos() {
        Resposta r = post("/api/v1/auth/cadastro", """
                {"nome_completo": " ", "email": "sem-arroba", "senha": "12345678", "confirmacao_senha": "12345678"}
                """, null);
        erroDeCampo(r, "nome_completo", "OBRIGATORIO");
        erroDeCampo(r, "email", "FORMATO_INVALIDO");
        erroDeCampo(r, "senha", "FORMATO_INVALIDO"); // senha muito comum
    }

    @Test
    void loginCorretoEErradoComMesmaRespostaParaEmailInexistenteESenhaErrada() {
        String email = emailNovo();
        novoUsuario("Beatriz Lima", email);

        Resposta ok = post("/api/v1/auth/login", """
                {"email": "%s", "senha": "%s"}""".formatted(email, SENHA), null);
        assertEquals(200, ok.status(), ok.bruta().body());
        assertEquals("BL", ok.texto("usuario.iniciais"));
        assertEquals(false, ok.em("configuracao_concluida"));
        assertNotNull(ok.texto("sessao.token"));

        Resposta senhaErrada = erro(post("/api/v1/auth/login", """
                {"email": "%s", "senha": "senhaErrada123"}""".formatted(email), null), 401, "CREDENCIAIS_INVALIDAS");
        Resposta inexistente = erro(post("/api/v1/auth/login", """
                {"email": "%s", "senha": "senhaErrada123"}""".formatted(emailNovo()), null), 401, "CREDENCIAIS_INVALIDAS");
        assertEquals(senhaErrada.texto("erro.mensagem"), inexistente.texto("erro.mensagem"));
        assertEquals(senhaErrada.lista("erro.campos"), inexistente.lista("erro.campos"));
    }

    @Test
    void loginSemCampoDevolve422() {
        erroDeCampo(post("/api/v1/auth/login", "{\"email\": \"a@b.com\"}", null), "senha", "OBRIGATORIO");
    }

    @Test
    void tokenInventadoVencidoERevogadoTemCodigosDiferentes() {
        // inventado
        erro(get("/api/v1/perfil", "token-que-nao-existe"), 401, "NAO_AUTENTICADO");
        // sem token
        Resposta semToken = erro(get("/api/v1/perfil", null), 401, "NAO_AUTENTICADO");
        assertEquals("Bearer", semToken.cabecalho("WWW-Authenticate"));

        // vencido
        String token = novoUsuario();
        assertEquals(200, get("/api/v1/perfil", token).status());
        jdbc.update("""
                UPDATE sessao SET criado_em = TIMESTAMP WITH TIME ZONE '2026-08-01 12:00:00+00',
                                  expira_em = TIMESTAMP WITH TIME ZONE '2026-08-31 12:00:00+00'
                WHERE token_hash = ?""", Tokens.hash(token));
        Resposta vencido = erro(get("/api/v1/perfil", token), 401, "SESSAO_EXPIRADA");
        assertEquals("Bearer error=\"invalid_token\"", vencido.cabecalho("WWW-Authenticate"));

        // login errado nunca é SESSAO_EXPIRADA
        erro(post("/api/v1/auth/login", "{\"email\": \"x@y.com\", \"senha\": \"qualquer1\"}", null), 401, "CREDENCIAIS_INVALIDAS");
    }

    @Test
    void logoutInvalidaOTokenEEhIdempotente() {
        String token = novoUsuario();
        assertEquals(204, post("/api/v1/auth/logout", null, token).status());
        erro(get("/api/v1/perfil", token), 401, "SESSAO_EXPIRADA");
        assertEquals(204, post("/api/v1/auth/logout", null, token).status());   // repetir: 204
        erro(post("/api/v1/auth/logout", null, null), 401, "NAO_AUTENTICADO");
        erro(post("/api/v1/auth/logout", null, "inventado"), 401, "NAO_AUTENTICADO");
    }

    @Test
    void sessaoDeslizanteRenovaSoAteOTeto() {
        String token = novoUsuario();
        String hash = Tokens.hash(token);
        // Simula uma sessão criada há 80 dias que vence em 1 dia: o uso empurra até o teto de 90 dias.
        jdbc.update("""
                UPDATE sessao SET criado_em = TIMESTAMP WITH TIME ZONE '2026-06-30 15:00:00+00',
                                  expira_em = TIMESTAMP WITH TIME ZONE '2026-09-19 15:00:00+00'
                WHERE token_hash = ?""", hash);
        assertEquals(200, get("/api/v1/perfil", token).status());
        String expira = jdbc.queryForObject("SELECT to_char(expira_em AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI') FROM sessao WHERE token_hash = ?",
                String.class, hash);
        assertEquals("2026-09-28 15:00", expira); // 2026-06-30 + 90 dias
    }

    @Test
    void recuperacaoTemMesmaRespostaParaEmailExistenteEInexistente() {
        String email = emailNovo();
        novoUsuario("Carla Dias", email);
        Resposta existe = post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(email), null);
        Resposta naoExiste = post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(emailNovo()), null);
        assertEquals(202, existe.status());
        assertEquals(202, naoExiste.status());
        assertEquals(existe.bruta().body(), naoExiste.bruta().body());
        Long id = jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recuperacao_senha WHERE usuario_id = ?", Integer.class, id));
    }

    @Test
    void recuperacaoComMaisDe3PedidosPorHoraDevolve429() {
        String email = emailNovo();
        for (int i = 0; i < 3; i++) {
            assertEquals(202, post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(email), null).status());
        }
        Resposta r = erro(post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(email), null),
                429, "LIMITE_REQUISICOES");
        assertNotNull(r.cabecalho("Retry-After"));
    }

    @Test
    void loginComMaisDe5TentativasEm15MinutosDevolve429() {
        String email = emailNovo();
        for (int i = 0; i < 5; i++) {
            erro(post("/api/v1/auth/login", "{\"email\": \"%s\", \"senha\": \"errada1234\"}".formatted(email), null),
                    401, "CREDENCIAIS_INVALIDAS");
        }
        Resposta r = erro(post("/api/v1/auth/login", "{\"email\": \"%s\", \"senha\": \"errada1234\"}".formatted(email), null),
                429, "LIMITE_REQUISICOES");
        assertTrue(Integer.parseInt(r.cabecalho("Retry-After")) > 0);
    }

    @Test
    void redefinirSenhaTrocaASenhaRevogaSessoesEOTokenEhDeUsoUnico() {
        String email = emailNovo();
        String sessaoAntiga = novoUsuario("Daniel Rocha", email);
        post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(email), null);
        String token = tokenDoUltimoEmail(email);

        String novaSenha = "OutraSenhaForte#1";
        String corpo = "{\"token\": \"%s\", \"nova_senha\": \"%s\", \"confirmacao_senha\": \"%s\"}"
                .formatted(token, novaSenha, novaSenha);
        assertEquals(204, post("/api/v1/auth/redefinir-senha", corpo, null).status());

        erro(get("/api/v1/perfil", sessaoAntiga), 401, "SESSAO_EXPIRADA");                // sessões revogadas
        assertEquals(200, post("/api/v1/auth/login",
                "{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, novaSenha), null).status());
        erro(post("/api/v1/auth/login",
                "{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, SENHA), null), 401, "CREDENCIAIS_INVALIDAS");

        // usado, inventado e vencido: mesma resposta
        Resposta usado = erro(post("/api/v1/auth/redefinir-senha", corpo, null), 400, "TOKEN_RECUPERACAO_INVALIDO");
        Resposta inventado = erro(post("/api/v1/auth/redefinir-senha", corpo.replace(token, "inventado"), null),
                400, "TOKEN_RECUPERACAO_INVALIDO");
        post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(email), null);
        String token2 = tokenDoUltimoEmail(email);
        jdbc.update("""
                UPDATE recuperacao_senha SET criado_em = TIMESTAMP WITH TIME ZONE '2026-09-18 14:00:00+00',
                                             expira_em = TIMESTAMP WITH TIME ZONE '2026-09-18 14:30:00+00'
                WHERE token_hash = ?""", Tokens.hash(token2));
        Resposta vencido = erro(post("/api/v1/auth/redefinir-senha", corpo.replace(token, token2), null),
                400, "TOKEN_RECUPERACAO_INVALIDO");
        assertEquals(usado.texto("erro.mensagem"), inventado.texto("erro.mensagem"));
        assertEquals(usado.texto("erro.mensagem"), vencido.texto("erro.mensagem"));
    }

    @Test
    void novoPedidoInvalidaOAnterior() {
        String email = emailNovo();
        novoUsuario("Elisa Prado", email);
        post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(email), null);
        String primeiro = tokenDoUltimoEmail(email);
        post("/api/v1/auth/recuperar-senha", "{\"email\": \"%s\"}".formatted(email), null);
        String corpo = "{\"token\": \"%s\", \"nova_senha\": \"NovaSenhaBoa#9\", \"confirmacao_senha\": \"NovaSenhaBoa#9\"}"
                .formatted(primeiro);
        erro(post("/api/v1/auth/redefinir-senha", corpo, null), 400, "TOKEN_RECUPERACAO_INVALIDO");
    }

    private String tokenDoUltimoEmail(String email) {
        List<EnvioEmail.Email> emails = envioEmail.enviados().stream().filter(e -> e.para().equals(email)).toList();
        assertFalse(emails.isEmpty(), "nenhum e-mail para " + email);
        Matcher m = Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(emails.get(emails.size() - 1).corpo());
        assertTrue(m.find());
        return m.group(1);
    }

    @Test
    void perfilDevolveIniciaisEOTokenDoLucasFunciona() {
        Resposta r = get("/api/v1/perfil", TOKEN_LUCAS);
        assertEquals(200, r.status());
        assertEquals("Lucas Carvalho", r.texto("nome_completo"));
        assertEquals("lucas@exemplo.com", r.texto("email"));
        assertEquals("LC", r.texto("iniciais"));
        assertEquals("https://exemplo.com/avatares/lucas.png", r.texto("avatar_url"));
        assertFalse(r.bruta().body().contains("senha"));
    }
}
