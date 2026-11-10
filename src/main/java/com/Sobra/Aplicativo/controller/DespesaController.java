package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.dto.DespesaDtos.DespesaRequest;
import com.Sobra.Aplicativo.dto.DespesaDtos.DespesaResposta;
import com.Sobra.Aplicativo.dto.DespesaDtos.DespesasMesResposta;
import com.Sobra.Aplicativo.seguranca.UsuarioLogado;
import com.Sobra.Aplicativo.service.DespesaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OP-11 e OP-12. */
@RestController
@RequestMapping("/api/v1/despesas")
public class DespesaController {

    private final DespesaService despesas;
    private final Relogio relogio;

    public DespesaController(DespesaService despesas, Relogio relogio) {
        this.despesas = despesas;
        this.relogio = relogio;
    }

    @GetMapping
    public DespesasMesResposta doMes(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                     @RequestParam(name = "mes", required = false) String mes) {
        return despesas.doMes(usuarioId, Parametros.mes(mes, relogio.mesCorrente()));
    }

    @PostMapping
    public ResponseEntity<DespesaResposta> criar(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                                 @RequestBody(required = false) DespesaRequest corpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(despesas.criar(usuarioId, corpo));
    }
}
