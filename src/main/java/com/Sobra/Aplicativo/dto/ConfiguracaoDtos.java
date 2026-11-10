package com.Sobra.Aplicativo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** OP-06 a OP-09: configuração inicial e categorias de referência. */
public final class ConfiguracaoDtos {

    private ConfiguracaoDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RendaRequest(@JsonProperty("valor") Object valor) {
    }

    /** "categorias" chega como Object: lista de objetos {codigo, valor_estimado}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoriasRequest(@JsonProperty("categorias") Object categorias) {
    }

    public record CategoriaRef(
            @JsonProperty("codigo") String codigo,
            @JsonProperty("nome") String nome) {
    }

    public record CategoriasResposta(@JsonProperty("categorias") List<CategoriaRef> categorias) {
    }

    public record CategoriaEstimativa(
            @JsonProperty("codigo") String codigo,
            @JsonProperty("nome") String nome,
            @JsonProperty("valor_estimado") String valorEstimado) {
    }

    public record EstimativasResposta(@JsonProperty("categorias") List<CategoriaEstimativa> categorias) {
    }

    public record ConfiguracaoResposta(
            @JsonProperty("renda_mensal") String rendaMensal,
            @JsonProperty("categorias") List<CategoriaEstimativa> categorias,
            @JsonProperty("etapa1_concluida") boolean etapa1Concluida,
            @JsonProperty("etapa2_concluida") boolean etapa2Concluida,
            @JsonProperty("concluida") boolean concluida) {
    }

    public record RendaResposta(
            @JsonProperty("renda_mensal") String rendaMensal,
            @JsonProperty("vigente_desde") String vigenteDesde) {
    }
}
