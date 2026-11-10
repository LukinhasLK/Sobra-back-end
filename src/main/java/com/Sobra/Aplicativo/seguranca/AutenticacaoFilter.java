package com.Sobra.Aplicativo.seguranca;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.config.SobraProperties;
import com.Sobra.Aplicativo.service.SessaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Autenticação por token opaco (cabeçalho Authorization: Bearer &lt;token&gt;).
 * Para rotas protegidas:
 *   sem token ou token desconhecido → 401 NAO_AUTENTICADO
 *   token vencido ou revogado       → 401 SESSAO_EXPIRADA (+ WWW-Authenticate)
 *   válido                          → guarda o usuarioId na requisição e aplica o limite geral (RNF-06)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AutenticacaoFilter extends OncePerRequestFilter {

    public static final String PREFIXO_API = "/api/v1/";

    /** Rotas públicas. O logout trata o token no próprio controller (precisa ser idempotente, RF-03). */
    private static final Set<String> PUBLICAS = Set.of(
            "/api/v1/auth/cadastro",
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/recuperar-senha",
            "/api/v1/auth/redefinir-senha",
            "/api/v1/saude");

    private final SessaoService sessaoService;
    private final LimiteTaxa limiteTaxa;
    private final SobraProperties props;

    public AutenticacaoFilter(SessaoService sessaoService, LimiteTaxa limiteTaxa, SobraProperties props) {
        this.sessaoService = sessaoService;
        this.limiteTaxa = limiteTaxa;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String caminho = caminho(req);
        return !caminho.startsWith(PREFIXO_API)
                || PUBLICAS.contains(caminho)
                || "OPTIONS".equalsIgnoreCase(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        try {
            String token = extrairToken(req.getHeader("Authorization"));
            if (token == null) {
                throw ErroApi.naoAutenticado();
            }
            Long usuarioId = sessaoService.validar(token);
            req.setAttribute(UsuarioLogado.ATRIBUTO, usuarioId);
            limiteTaxa.consumir("token:" + Tokens.hash(token),
                    props.getLimiteTaxa().getGeralPorToken(),
                    Duration.ofSeconds(props.getLimiteTaxa().getGeralJanelaSegundos()));
        } catch (ErroApi erro) {
            RespostaErro.escrever(req, resp, erro);
            return;
        } catch (DataAccessException | TransactionException e) {
            RespostaErro.escrever(req, resp, ErroApi.servicoIndisponivel());
            return;
        }
        chain.doFilter(req, resp);
    }

    /** "Bearer abc" → "abc"; qualquer outro formato → null. */
    public static String extrairToken(String cabecalho) {
        if (cabecalho == null || cabecalho.length() < 8 || !cabecalho.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = cabecalho.substring(7).trim();
        return token.isEmpty() || token.length() > 200 ? null : token;
    }

    private static String caminho(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contexto = req.getContextPath();
        return (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)) ? uri.substring(contexto.length()) : uri;
    }
}
