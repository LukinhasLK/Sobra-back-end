package com.Sobra.Aplicativo.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * RN-07 / D-01: juros compostos mensais, aporte no fim de cada mês.
 * saldo(0) = inicial; saldo(t) = saldo(t-1) × (1 + taxa) + aporte.
 * Nenhum arredondamento no meio do caminho: só a saída é arredondada (2 casas, HALF_UP).
 * Arredondar mês a mês daria 14680.66 no caso de teste, em vez de 14680.67.
 */
public final class Simulador {

    private Simulador() {
    }

    public record Ponto(int mes, BigDecimal comRendimento, BigDecimal semRendimento) {
    }

    public record Resultado(BigDecimal taxaMensal,
                            BigDecimal totalInvestido,
                            BigDecimal totalAportes,
                            BigDecimal estimativaFinal,
                            BigDecimal rendimentoEstimado,
                            List<Ponto> serie) {
    }

    public static Resultado calcular(BigDecimal valorInicial, BigDecimal aporteMensal,
                                     int prazoMeses, BigDecimal taxaMensal) {
        BigDecimal fator = BigDecimal.ONE.add(taxaMensal);
        BigDecimal saldo = valorInicial;          // exato, sem arredondar
        List<Ponto> serie = new ArrayList<>(prazoMeses + 1);
        serie.add(new Ponto(0, Dinheiro.arredondar(valorInicial), Dinheiro.arredondar(valorInicial)));

        for (int mes = 1; mes <= prazoMeses; mes++) {
            saldo = saldo.multiply(fator).add(aporteMensal);
            BigDecimal semRendimento = valorInicial.add(aporteMensal.multiply(BigDecimal.valueOf(mes)));
            serie.add(new Ponto(mes, Dinheiro.arredondar(saldo), Dinheiro.arredondar(semRendimento)));
        }

        BigDecimal totalAportes = Dinheiro.arredondar(aporteMensal.multiply(BigDecimal.valueOf(prazoMeses)));
        BigDecimal totalInvestido = Dinheiro.arredondar(valorInicial.add(totalAportes));
        BigDecimal estimativaFinal = Dinheiro.arredondar(saldo);
        BigDecimal rendimento = estimativaFinal.subtract(totalInvestido);

        return new Resultado(taxaMensal, totalInvestido, totalAportes, estimativaFinal, rendimento, serie);
    }
}
