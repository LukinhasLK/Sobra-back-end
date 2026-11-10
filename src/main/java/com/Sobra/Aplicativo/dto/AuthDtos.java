package com.Sobra.Aplicativo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Corpos de requisição e resposta de /auth (OP-01 a OP-05).
 * Campos de entrada são Object para a validação devolver 422 no campo certo
 * mesmo quando o tipo vem errado. Campos desconhecidos (ex.: "usuario_id") são ignorados.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CadastroRequest(
            @JsonProperty("nome_completo") Object nomeCompleto,
            @JsonProperty("email") Object email,
            @JsonProperty("senha") Object senha,
            @JsonProperty("confirmacao_senha") Object confirmacaoSenha) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(
            @JsonProperty("email") Object email,
            @JsonProperty("senha") Object senha) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecuperarSenhaRequest(@JsonProperty("email") Object email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RedefinirSenhaRequest(
            @JsonProperty("token") Object token,
            @JsonProperty("nova_senha") Object novaSenha,
            @JsonProperty("confirmacao_senha") Object confirmacaoSenha) {
    }

    public record UsuarioResumo(
            @JsonProperty("id") Long id,
            @JsonProperty("nome_completo") String nomeCompleto,
            @JsonProperty("email") String email,
            @JsonProperty("iniciais") String iniciais) {
    }

    public record SessaoResposta(
            @JsonProperty("token") String token,
            @JsonProperty("expira_em") String expiraEm) {
    }

    public record AuthResposta(
            @JsonProperty("usuario") UsuarioResumo usuario,
            @JsonProperty("sessao") SessaoResposta sessao,
            @JsonProperty("configuracao_concluida") boolean configuracaoConcluida) {
    }

    public record MensagemResposta(@JsonProperty("mensagem") String mensagem) {
    }
}
