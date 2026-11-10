package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.AporteCriadoResposta;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.AporteRequest;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivoCriado;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivoDetalhe;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivoRequest;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivosResposta;
import com.Sobra.Aplicativo.seguranca.UsuarioLogado;
import com.Sobra.Aplicativo.service.ObjetivoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OP-13 a OP-16. */
@RestController
@RequestMapping("/api/v1/objetivos")
public class ObjetivoController {

    private final ObjetivoService objetivos;

    public ObjetivoController(ObjetivoService objetivos) {
        this.objetivos = objetivos;
    }

    @GetMapping
    public ObjetivosResposta listar(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId) {
        return new ObjetivosResposta(objetivos.listar(usuarioId));
    }

    @PostMapping
    public ResponseEntity<ObjetivoCriado> criar(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                                @RequestBody(required = false) ObjetivoRequest corpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objetivos.criar(usuarioId, corpo));
    }

    @GetMapping("/{id}")
    public ObjetivoDetalhe detalhe(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                   @PathVariable("id") String id) {
        return objetivos.detalhe(usuarioId, Parametros.id(id));
    }

    @PostMapping("/{id}/aportes")
    public ResponseEntity<AporteCriadoResposta> aporte(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                                       @PathVariable("id") String id,
                                                       @RequestBody(required = false) AporteRequest corpo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(objetivos.registrarAporte(usuarioId, Parametros.id(id), corpo));
    }
}
