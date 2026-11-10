package com.Sobra.Aplicativo.comum;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Parâmetros de consulta (query string). Inválido = 400 REQUISICAO_INVALIDA (seção 6.1). */
public final class Parametros {

    private static final Pattern MES = Pattern.compile("^\\d{4}-\\d{2}$");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("uuuu-MM");

    private Parametros() {
    }

    /** mes=AAAA-MM; ausente = mês corrente (recebido pronto, no fuso America/Sao_Paulo). */
    public static YearMonth mes(String bruto, YearMonth mesCorrente) {
        if (bruto == null || bruto.isBlank()) {
            return mesCorrente;
        }
        if (!MES.matcher(bruto).matches()) {
            throw ErroApi.requisicaoInvalida("Parâmetro mes inválido. Use AAAA-MM.");
        }
        try {
            YearMonth mes = YearMonth.parse(bruto, FORMATO_MES);
            if (mes.getYear() < 2000) {
                throw ErroApi.requisicaoInvalida("Parâmetro mes inválido. Use AAAA-MM.");
            }
            return mes;
        } catch (DateTimeParseException e) {
            throw ErroApi.requisicaoInvalida("Parâmetro mes inválido. Use AAAA-MM.");
        }
    }

    public static String texto(YearMonth mes) {
        return mes == null ? null : mes.format(FORMATO_MES);
    }

    /** limite de 1 a 100; ausente = padrão. */
    public static int limite(String bruto, int padrao) {
        if (bruto == null || bruto.isBlank()) {
            return padrao;
        }
        try {
            int v = Integer.parseInt(bruto.trim());
            if (v < 1 || v > 100) {
                throw ErroApi.requisicaoInvalida("Parâmetro limite deve estar entre 1 e 100.");
            }
            return v;
        } catch (NumberFormatException e) {
            throw ErroApi.requisicaoInvalida("Parâmetro limite deve estar entre 1 e 100.");
        }
    }

    /** Id de rota: não numérico é tratado como inexistente (404), igual a recurso alheio. */
    public static long id(String bruto) {
        try {
            long v = Long.parseLong(bruto);
            if (v <= 0) {
                throw ErroApi.naoEncontrado();
            }
            return v;
        } catch (NumberFormatException e) {
            throw ErroApi.naoEncontrado();
        }
    }

    /** Instante em UTC sem frações: "2026-10-18T17:30:00Z". */
    public static String instante(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /** Dia de calendário: "2026-09-15". */
    public static String dia(LocalDate data) {
        return data == null ? null : data.toString();
    }
}
