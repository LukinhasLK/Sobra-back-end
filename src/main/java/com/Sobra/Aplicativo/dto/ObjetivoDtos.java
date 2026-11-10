package com.Sobra.Aplicativo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** OP-13 a OP-16. */
public final class ObjetivoDtos {

    private ObjetivoDtos() {
    }

    /** aporte_mensal enviado pelo cliente é ignorado: o servidor calcula (RN-04). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjetivoRequest(
            @JsonProperty("nome") Object nome,
            @JsonProperty("valor_meta") Object valorMeta,
            @JsonProperty("prazo_meses") Object prazoMeses) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AporteRequest(
            @JsonProperty("tipo") Object tipo,
            @JsonProperty("valor") Object valor,
            @JsonProperty("data") Object data) {
    }

    public record ObjetivoItem(
            @JsonProperty("id") Long id,
            @JsonProperty("nome") String nome,
            @JsonProperty("valor_acumulado") String valorAcumulado,
            @JsonProperty("valor_meta") String valorMeta,
            @JsonProperty("percentual_concluido") String percentualConcluido,
            @JsonProperty("imagem_url") String imagemUrl) {
    }

    public record ObjetivosResposta(@JsonProperty("objetivos") List<ObjetivoItem> objetivos) {
    }

    /** Resposta do POST /objetivos (OP-14): igual ao detalhe, sem a lista de aportes. */
    public record ObjetivoCriado(
            @JsonProperty("id") Long id,
            @JsonProperty("nome") String nome,
            @JsonProperty("valor_meta") String valorMeta,
            @JsonProperty("valor_acumulado") String valorAcumulado,
            @JsonProperty("percentual_concluido") String percentualConcluido,
            @JsonProperty("prazo_meses") Integer prazoMeses,
            @JsonProperty("aporte_mensal") String aporteMensal,
            @JsonProperty("previsao_conclusao") String previsaoConclusao,
            @JsonProperty("concluido") boolean concluido,
            @JsonProperty("imagem_url") String imagemUrl) {
    }

    public record AporteResposta(
            @JsonProperty("id") Long id,
            @JsonProperty("tipo") String tipo,
            @JsonProperty("titulo") String titulo,
            @JsonProperty("data") String data,
            @JsonProperty("valor") String valor) {
    }

    public record ObjetivoDetalhe(
            @JsonProperty("id") Long id,
            @JsonProperty("nome") String nome,
            @JsonProperty("valor_meta") String valorMeta,
            @JsonProperty("valor_acumulado") String valorAcumulado,
            @JsonProperty("percentual_concluido") String percentualConcluido,
            @JsonProperty("prazo_meses") Integer prazoMeses,
            @JsonProperty("aporte_mensal") String aporteMensal,
            @JsonProperty("previsao_conclusao") String previsaoConclusao,
            @JsonProperty("concluido") boolean concluido,
            @JsonProperty("imagem_url") String imagemUrl,
            @JsonProperty("aportes") List<AporteResposta> aportes) {
    }

    public record ProgressoObjetivo(
            @JsonProperty("valor_acumulado") String valorAcumulado,
            @JsonProperty("percentual_concluido") String percentualConcluido,
            @JsonProperty("previsao_conclusao") String previsaoConclusao,
            @JsonProperty("concluido") boolean concluido) {
    }

    public record AporteCriadoResposta(
            @JsonProperty("aporte") AporteResposta aporte,
            @JsonProperty("objetivo") ProgressoObjetivo objetivo) {
    }
}
