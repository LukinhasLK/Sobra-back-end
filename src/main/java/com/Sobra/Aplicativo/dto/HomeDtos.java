package com.Sobra.Aplicativo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** OP-10: resumo da Home. renda e disponivel são nulos quando o mês não tem renda. */
public final class HomeDtos {

    private HomeDtos() {
    }

    public record UsuarioHome(
            @JsonProperty("primeiro_nome") String primeiroNome,
            @JsonProperty("avatar_url") String avatarUrl) {
    }

    public record GastoCategoria(
            @JsonProperty("codigo") String codigo,
            @JsonProperty("nome") String nome,
            @JsonProperty("valor") String valor) {
    }

    public record HomeResposta(
            @JsonProperty("mes") String mes,
            @JsonProperty("usuario") UsuarioHome usuario,
            @JsonProperty("renda") String renda,
            @JsonProperty("gastos") String gastos,
            @JsonProperty("disponivel") String disponivel,
            @JsonProperty("categorias") List<GastoCategoria> categorias) {
    }
}
