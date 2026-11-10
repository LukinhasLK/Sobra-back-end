package com.Sobra.Aplicativo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** OP-20, OP-21 e OP-22. */
public final class PerfilDtos {

    private PerfilDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NotificacoesRequest(@JsonProperty("notificacoes_ativas") Object notificacoesAtivas) {
    }

    public record NotificacoesResposta(@JsonProperty("notificacoes_ativas") boolean notificacoesAtivas) {
    }

    public record PerfilResposta(
            @JsonProperty("nome_completo") String nomeCompleto,
            @JsonProperty("email") String email,
            @JsonProperty("iniciais") String iniciais,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("notificacoes_ativas") boolean notificacoesAtivas) {
    }

    public record SaudeResposta(@JsonProperty("status") String status) {
    }
}
