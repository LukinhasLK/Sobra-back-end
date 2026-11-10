package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.dto.PerfilDtos.NotificacoesRequest;
import com.Sobra.Aplicativo.dto.PerfilDtos.NotificacoesResposta;
import com.Sobra.Aplicativo.dto.PerfilDtos.PerfilResposta;
import com.Sobra.Aplicativo.seguranca.UsuarioLogado;
import com.Sobra.Aplicativo.service.PerfilService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OP-20 e OP-21. */
@RestController
@RequestMapping("/api/v1/perfil")
public class PerfilController {

    private final PerfilService perfil;

    public PerfilController(PerfilService perfil) {
        this.perfil = perfil;
    }

    @GetMapping
    public PerfilResposta perfil(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId) {
        return perfil.perfil(usuarioId);
    }

    @PutMapping("/notificacoes")
    public NotificacoesResposta notificacoes(@RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId,
                                             @RequestBody(required = false) NotificacoesRequest corpo) {
        return perfil.alterarNotificacoes(usuarioId, corpo);
    }
}
