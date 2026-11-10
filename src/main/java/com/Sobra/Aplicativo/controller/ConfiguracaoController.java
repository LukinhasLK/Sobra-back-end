package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.CategoriasRequest;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.CategoriasResposta;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.ConfiguracaoResposta;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.EstimativasResposta;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.RendaRequest;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.RendaResposta;
import com.Sobra.Aplicativo.seguranca.UsuarioLogado;
import com.Sobra.Aplicativo.service.ConfiguracaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OP-06 a OP-09. */
@RestController
@RequestMapping("/api/v1")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracao;

    public ConfiguracaoController(ConfiguracaoService configuracao) {
        this.configuracao = configuracao;
    }

    @GetMapping("/configuracao")
    public ConfiguracaoResposta estado(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId) {
        return configuracao.estado(usuarioId);
    }

    @PutMapping("/configuracao/renda")
    public RendaResposta renda(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                               @RequestBody(required = false) RendaRequest corpo) {
        return configuracao.registrarRenda(usuarioId, corpo);
    }

    @PutMapping("/configuracao/categorias")
    public EstimativasResposta categorias(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                          @RequestBody(required = false) CategoriasRequest corpo) {
        return configuracao.registrarCategorias(usuarioId, corpo);
    }

    @GetMapping("/categorias")
    public CategoriasResposta referencia() {
        return new CategoriasResposta(configuracao.categorias());
    }
}
