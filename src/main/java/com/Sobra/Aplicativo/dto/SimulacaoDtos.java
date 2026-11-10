package com.Sobra.Aplicativo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** OP-17 a OP-19. "resultado" enviado pelo cliente é ignorado: o servidor recalcula. */
public final class SimulacaoDtos {

    private SimulacaoDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SimulacaoRequest(
            @JsonProperty("valor_inicial") Object valorInicial,
            @JsonProperty("aporte_mensal") Object aporteMensal,
            @JsonProperty("prazo_meses") Object prazoMeses) {
    }

    public record PontoSerie(
            @JsonProperty("mes") int mes,
            @JsonProperty("com_rendimento") String comRendimento,
            @JsonProperty("sem_rendimento") String semRendimento) {
    }

    public record CalculoResposta(
            @JsonProperty("taxa_mensal") String taxaMensal,
            @JsonProperty("total_investido") String totalInvestido,
            @JsonProperty("total_aportes") String totalAportes,
            @JsonProperty("estimativa_final") String estimativaFinal,
            @JsonProperty("rendimento_estimado") String rendimentoEstimado,
            @JsonProperty("serie") List<PontoSerie> serie) {
    }

    public record SimulacaoSalva(
            @JsonProperty("id") Long id,
            @JsonProperty("criado_em") String criadoEm,
            @JsonProperty("valor_inicial") String valorInicial,
            @JsonProperty("aporte_mensal") String aporteMensal,
            @JsonProperty("prazo_meses") Integer prazoMeses,
            @JsonProperty("taxa_mensal") String taxaMensal,
            @JsonProperty("resultado") String resultado) {
    }

    public record SimulacoesResposta(@JsonProperty("simulacoes") List<SimulacaoSalva> simulacoes) {
    }
}
