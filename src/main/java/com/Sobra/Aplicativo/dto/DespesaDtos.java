package com.Sobra.Aplicativo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.CategoriaRef;

import java.util.List;

/** OP-11 e OP-12. */
public final class DespesaDtos {

    private DespesaDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DespesaRequest(
            @JsonProperty("descricao") Object descricao,
            @JsonProperty("categoria_codigo") Object categoriaCodigo,
            @JsonProperty("data") Object data,
            @JsonProperty("valor") Object valor) {
    }

    public record DespesaResposta(
            @JsonProperty("id") Long id,
            @JsonProperty("descricao") String descricao,
            @JsonProperty("categoria") CategoriaRef categoria,
            @JsonProperty("data") String data,
            @JsonProperty("valor") String valor) {
    }

    public record DespesasMesResposta(
            @JsonProperty("mes") String mes,
            @JsonProperty("total") String total,
            @JsonProperty("despesas") List<DespesaResposta> despesas) {
    }
}
