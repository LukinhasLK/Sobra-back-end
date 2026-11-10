package com.Sobra.Aplicativo.seguranca;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.JsonErro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Escreve um erro no formato único direto na resposta (usado pelos filtros, antes do Spring MVC). */
public final class RespostaErro {

    private RespostaErro() {
    }

    public static void escrever(HttpServletRequest req, HttpServletResponse resp, ErroApi erro) throws IOException {
        if (resp.isCommitted()) {
            return;
        }
        resp.resetBuffer();
        resp.setStatus(erro.getStatus());
        resp.setContentType("application/json;charset=UTF-8");
        String cabecalho = cabecalhoAutenticacao(erro);
        if (cabecalho != null) {
            resp.setHeader("WWW-Authenticate", cabecalho);
        }
        if (erro.getRetryAfterSegundos() != null) {
            resp.setHeader("Retry-After", String.valueOf(erro.getRetryAfterSegundos()));
        }
        byte[] corpo = JsonErro.corpo(erro, idRequisicao(req));
        resp.setContentLength(corpo.length);
        resp.getOutputStream().write(corpo);
        resp.flushBuffer();
    }

    /** Seção 6.4: sessão expirada leva error="invalid_token"; sem token, só "Bearer". */
    public static String cabecalhoAutenticacao(ErroApi erro) {
        return switch (erro.getCodigo()) {
            case "SESSAO_EXPIRADA" -> "Bearer error=\"invalid_token\"";
            case "NAO_AUTENTICADO" -> "Bearer";
            default -> null;
        };
    }

    public static String idRequisicao(HttpServletRequest req) {
        Object id = req.getAttribute(UsuarioLogado.ID_REQUISICAO);
        return id == null ? null : id.toString();
    }
}
