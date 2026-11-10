package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.CalculoResposta;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.SimulacaoRequest;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.SimulacaoSalva;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.SimulacoesResposta;
import com.Sobra.Aplicativo.seguranca.UsuarioLogado;
import com.Sobra.Aplicativo.service.SimulacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OP-17 a OP-19. */
@RestController
@RequestMapping("/api/v1/simulacoes")
public class SimulacaoController {

    private final SimulacaoService simulacoes;

    public SimulacaoController(SimulacaoService simulacoes) {
        this.simulacoes = simulacoes;
    }

    @PostMapping("/calcular")
    public CalculoResposta calcular(@RequestBody(required = false) SimulacaoRequest corpo) {
        return simulacoes.calcular(corpo);
    }

    @PostMapping
    public ResponseEntity<SimulacaoSalva> salvar(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                                 @RequestBody(required = false) SimulacaoRequest corpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulacoes.salvar(usuarioId, corpo));
    }

    @GetMapping
    public SimulacoesResposta listar(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                     @RequestParam(name = "limite", required = false) String limite) {
        return new SimulacoesResposta(simulacoes.listar(usuarioId, Parametros.limite(limite, 50)));
    }
}
