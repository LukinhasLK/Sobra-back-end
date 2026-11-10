package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.dto.HomeDtos.HomeResposta;
import com.Sobra.Aplicativo.seguranca.UsuarioLogado;
import com.Sobra.Aplicativo.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OP-10. */
@RestController
@RequestMapping("/api/v1")
public class HomeController {

    private final HomeService home;
    private final Relogio relogio;

    public HomeController(HomeService home, Relogio relogio) {
        this.home = home;
        this.relogio = relogio;
    }

    @GetMapping("/home")
    public HomeResposta resumo(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                               @RequestParam(name = "mes", required = false) String mes) {
        return home.resumo(usuarioId, Parametros.mes(mes, relogio.mesCorrente()));
    }
}
