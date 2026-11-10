package com.Sobra.Aplicativo.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Fonte única de "agora". Instantes em UTC; datas de calendário e "mês corrente"
 * no fuso America/Sao_Paulo (seção 2.3). Nos testes o relógio é fixo (2026-09-18),
 * para os casos da especificação darem sempre o mesmo resultado.
 */
@Component
public class Relogio {

    private final Clock clock;
    private final ZoneId fuso;

    public Relogio(SobraProperties props) {
        this.fuso = ZoneId.of(props.getFusoHorario());
        String fixo = props.getRelogioFixo();
        this.clock = (fixo == null || fixo.isBlank())
                ? Clock.systemUTC()
                : Clock.fixed(Instant.parse(fixo.trim()), ZoneOffset.UTC);
    }

    public Instant agora() {
        return Instant.now(clock);
    }

    public LocalDate hoje() {
        return LocalDate.now(clock.withZone(fuso));
    }

    public YearMonth mesCorrente() {
        return YearMonth.from(hoje());
    }
}
