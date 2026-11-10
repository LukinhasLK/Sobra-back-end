package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.dto.AuthDtos.AuthResposta;
import com.Sobra.Aplicativo.dto.AuthDtos.CadastroRequest;
import com.Sobra.Aplicativo.dto.AuthDtos.LoginRequest;
import com.Sobra.Aplicativo.dto.AuthDtos.MensagemResposta;
import com.Sobra.Aplicativo.dto.AuthDtos.RecuperarSenhaRequest;
import com.Sobra.Aplicativo.dto.AuthDtos.RedefinirSenhaRequest;
import com.Sobra.Aplicativo.seguranca.AutenticacaoFilter;
import com.Sobra.Aplicativo.service.AuthService;
import com.Sobra.Aplicativo.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OP-01 a OP-05. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final SessaoService sessoes;

    public AuthController(AuthService auth, SessaoService sessoes) {
        this.auth = auth;
        this.sessoes = sessoes;
    }

    /** OP-01 · 201. */
    @PostMapping("/cadastro")
    public ResponseEntity<AuthResposta> cadastro(@RequestBody(required = false) CadastroRequest corpo,
                                                 HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.cadastrar(corpo, req.getRemoteAddr()));
    }

    /** OP-02 · 200. */
    @PostMapping("/login")
    public AuthResposta login(@RequestBody(required = false) LoginRequest corpo, HttpServletRequest req) {
        return auth.login(corpo, req.getRemoteAddr());
    }

    /** OP-03 · 204, idempotente. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization", required = false) String autorizacao) {
        sessoes.encerrar(AutenticacaoFilter.extrairToken(autorizacao));
        return ResponseEntity.noContent().build();
    }

    /** OP-04 · 202, mesma resposta exista ou não o e-mail. */
    @PostMapping("/recuperar-senha")
    public ResponseEntity<MensagemResposta> recuperarSenha(@RequestBody(required = false) RecuperarSenhaRequest corpo,
                                                           HttpServletRequest req) {
        auth.pedirRecuperacao(corpo, req.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensagemResposta(
                "Se o e-mail estiver cadastrado, você receberá as instruções em instantes."));
    }

    /** OP-05 · 204. */
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@RequestBody(required = false) RedefinirSenhaRequest corpo,
                                               HttpServletRequest req) {
        auth.redefinirSenha(corpo, req.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
