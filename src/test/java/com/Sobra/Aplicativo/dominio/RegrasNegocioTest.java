package com.Sobra.Aplicativo.dominio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Seção 7.1: todos os casos numéricos, sem Spring nem banco. */
class RegrasNegocioTest {

    private static final YearMonth REFERENCIA = YearMonth.of(2026, 9);

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @Test
    void rn01_decimalExato() {
        assertEquals(d("0.30"), d("0.10").add(d("0.20")));
        assertTrue(0.1 + 0.2 != 0.3, "ponto flutuante erra; por isso BigDecimal");
    }

    @ParameterizedTest(name = "RN-03 {0} de {1} = {2}")
    @CsvSource({
            "8400.00, 15000.00, 56.00",
            "3200.00, 12000.00, 26.67",
            "1800.00, 4500.00, 40.00",
            "16000.00, 15000.00, 100.00",
            "0, 15000.00, 0.00"
    })
    void rn03_percentual(String acumulado, String meta, String esperado) {
        assertEquals(esperado, RegrasObjetivo.percentual(d(acumulado), d(meta)).toPlainString());
    }

    @ParameterizedTest(name = "RN-04 {0} ÷ {1} = {2}")
    @CsvSource({
            "15000.00, 18, 833.33",
            "1.00, 8, 0.13",
            "10000.00, 3, 3333.33",
            "100.00, 7, 14.29"
    })
    void rn04_aporteSugeridoHalfUp(String meta, int prazo, String esperado) {
        assertEquals(esperado, RegrasObjetivo.aporteSugerido(d(meta), prazo).toPlainString());
    }

    @ParameterizedTest(name = "RN-08 meta {0}, acumulado {1}, aporte {2} → {3}")
    @CsvSource({
            "15000, 8400, 500, 2027-11",     // Reserva: 13,2 → 14
            "12000, 3200, 500, 2028-03",     // Viagem: 17,6 → 18
            "4500, 1800, 450, 2027-03",      // Notebook: 6,00 → 6
            "15000, 0, 833.33, 2028-03",     // novo: 18,000072 → 18,00 → 18 (sem arredondar daria 2028-04)
            "15000, 8700, 500, 2027-10"      // após aporte extra de 300: 12,6 → 13
    })
    void rn08_previsao(String meta, String acumulado, String aporte, String esperado) {
        assertEquals(YearMonth.parse(esperado),
                RegrasObjetivo.previsaoConclusao(d(meta), d(acumulado), d(aporte), REFERENCIA));
        assertFalse(RegrasObjetivo.concluido(d(acumulado), d(meta)));
    }

    @Test
    void rn08_concluido() {
        assertNull(RegrasObjetivo.previsaoConclusao(d("15000"), d("16000"), d("500"), REFERENCIA));
        assertTrue(RegrasObjetivo.concluido(d("16000"), d("15000")));
        assertTrue(RegrasObjetivo.concluido(d("15000"), d("15000")));
    }

    @Test
    void rn07_simulacao() {
        Simulador.Resultado r = Simulador.calcular(d("1000.00"), d("500.00"), 24, d("0.009600"));
        assertEquals(d("14680.67"), r.estimativaFinal());
        assertEquals(d("13000.00"), r.totalInvestido());
        assertEquals(d("12000.00"), r.totalAportes());
        assertEquals(d("1680.67"), r.rendimentoEstimado());
        assertEquals(25, r.serie().size());

        int[] meses = {0, 1, 2, 12, 24};
        String[] com = {"1000.00", "1509.60", "2024.09", "7448.64", "14680.67"};
        String[] sem = {"1000.00", "1500.00", "2000.00", "7000.00", "13000.00"};
        for (int i = 0; i < meses.length; i++) {
            assertEquals(d(com[i]), r.serie().get(meses[i]).comRendimento(), "com rendimento, mês " + meses[i]);
            assertEquals(d(sem[i]), r.serie().get(meses[i]).semRendimento(), "sem rendimento, mês " + meses[i]);
        }
    }

    @Test
    void rn07_arredondarMesAMesDariaOutroValor() {
        BigDecimal saldo = d("1000.00");
        for (int i = 0; i < 24; i++) {
            saldo = Dinheiro.arredondar(saldo.multiply(d("1.009600")).add(d("500.00")));
        }
        assertEquals(d("14680.66"), saldo);   // por isso o Simulador só arredonda na saída
    }

    @ParameterizedTest(name = "RN-09 \"{0}\" → {1} / {2}")
    @CsvSource({
            "Lucas Carvalho, LC, Lucas",
            "lucas, L, lucas",
            "Ana Maria de Souza, AS, Ana",
            "'  élida   álvares ', ÉÁ, élida"
    })
    void rn09_iniciaisEPrimeiroNome(String nome, String iniciais, String primeiro) {
        assertEquals(iniciais, Nomes.iniciais(nome));
        assertEquals(primeiro, Nomes.primeiroNome(nome));
    }

    @Test
    void dinheiroSempreComDuasCasas() {
        assertEquals("0.00", Dinheiro.texto(BigDecimal.ZERO));
        assertEquals("-480.00", Dinheiro.texto(d("-480")));
        assertEquals("0.009600", Dinheiro.taxa(d("0.0096")));
        assertNull(Dinheiro.texto(null));
    }
}
