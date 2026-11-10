package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.dto.PerfilDtos.SaudeResposta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** OP-22 · RNF-12: 200 se o banco responde, 503 se não. Público. */
@RestController
public class SaudeController {

    private final JdbcTemplate jdbc;

    public SaudeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/saude")
    public SaudeResposta saude() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return new SaudeResposta("ok");
        } catch (RuntimeException e) {
            throw ErroApi.servicoIndisponivel();
        }
    }
}
