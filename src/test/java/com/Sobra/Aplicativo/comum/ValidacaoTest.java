package com.Sobra.Aplicativo.comum;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras de validação da decisão D-03 (opção B) e formato do erro. */
class ValidacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 18);

    @Test
    void valoresValidos() {
        Validacao v = new Validacao();
        assertEquals(new BigDecimal("120.00"), v.valorPositivo("a", "120"));
        assertEquals(new BigDecimal("120.10"), v.valorPositivo("b", 120.1));
        assertEquals(new BigDecimal("2.50"), v.valorPositivo("c", "2.500"));
        assertEquals(new BigDecimal("99999999.99"), v.valorPositivo("d", "99999999.99"));
        assertEquals(HOJE, v.data("e", "2026-09-18", HOJE));
        assertEquals(18, v.inteiro("f", 18, 1, 360));
        assertEquals("ana@x.com", v.email("g", "  Ana@X.com "));
        assertFalse(v.temErros());
    }

    @Test
    void cadaErroNoCampoCertoComOCodigoCerto() {
        Validacao v = new Validacao();
        v.valorPositivo("zero", "0");
        v.valorPositivo("tresCasas", "1.234");
        v.valorPositivo("negativo", "-5");
        v.valorPositivo("booleano", true);
        v.valorPositivo("teto", "100000000.00");
        v.data("futura", "2026-09-19", HOJE);
        v.data("inexistente", "2026-02-30", HOJE);
        v.data("antiga", "1999-12-31", HOJE);
        v.inteiro("prazo", 361, 1, 360);
        v.inteiro("fracao", 1.5, 1, 360);
        v.email("email", "semarroba");
        v.senha("comum", "12345678");
        v.senha("curta", "curta");
        v.texto("vazio", "   ", 1, 120);

        ErroApi e = assertThrows(ErroApi.class, v::lancarSeHouver);
        assertEquals(422, e.getStatus());
        assertEquals("VALIDACAO", e.getCodigo());
        List<String> obtidos = e.getCampos().stream().map(c -> c.campo() + ":" + c.codigo()).toList();
        assertEquals(List.of(
                "zero:DEVE_SER_POSITIVO", "tresCasas:FORMATO_INVALIDO", "negativo:DEVE_SER_POSITIVO",
                "booleano:FORMATO_INVALIDO", "teto:FORA_DO_LIMITE", "futura:DATA_INVALIDA",
                "inexistente:DATA_INVALIDA", "antiga:DATA_INVALIDA", "prazo:FORA_DO_LIMITE",
                "fracao:FORMATO_INVALIDO", "email:FORMATO_INVALIDO", "comum:FORMATO_INVALIDO",
                "curta:FORA_DO_LIMITE", "vazio:OBRIGATORIO"), obtidos);
    }

    @Test
    void parametroMes() {
        YearMonth corrente = YearMonth.of(2026, 9);
        assertEquals(corrente, Parametros.mes(null, corrente));
        assertEquals(YearMonth.of(2026, 1), Parametros.mes("2026-01", corrente));
        assertEquals(400, assertThrows(ErroApi.class, () -> Parametros.mes("2026-13", corrente)).getStatus());
        assertEquals(400, assertThrows(ErroApi.class, () -> Parametros.mes("2026-9", corrente)).getStatus());
    }

    @Test
    void formatoUnicoDeErro() {
        String json = new String(JsonErro.corpo(ErroApi.validacao("valor", "DEVE_SER_POSITIVO", "Informe \"mais\"."), "id-1"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("{\"erro\":{\"codigo\":\"VALIDACAO\",\"mensagem\":\"Há campos inválidos.\",\"campos\":"
                + "[{\"campo\":\"valor\",\"codigo\":\"DEVE_SER_POSITIVO\",\"mensagem\":\"Informe \\\"mais\\\".\"}],"
                + "\"id_requisicao\":\"id-1\"}}", json);
        assertTrue(new String(JsonErro.corpo(ErroApi.naoEncontrado(), null), java.nio.charset.StandardCharsets.UTF_8)
                .contains("\"campos\":[]"));
    }
}
