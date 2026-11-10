package com.Sobra.Aplicativo.seguranca;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.JsonErro;
import com.Sobra.Aplicativo.config.SobraProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Primeiro filtro de toda requisição:
 * - gera o id_requisicao (vai no erro e no log, RNF-07);
 * - recusa HTTP quando sobra.exigir-https=true (RNF-01);
 * - limita o corpo a 10 KB (seção 6.1);
 * - registra um log JSON por requisição, sem dados pessoais (RNF-08);
 * - última rede de segurança: qualquer exceção vira 500 no formato único.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequisicaoFilter extends OncePerRequestFilter {

    public static final int LIMITE_CORPO_BYTES = 10 * 1024;
    private static final Logger LOG = LoggerFactory.getLogger("sobra.requisicao");

    private final SobraProperties props;

    public RequisicaoFilter(SobraProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        String id = UUID.randomUUID().toString();
        req.setAttribute(UsuarioLogado.ID_REQUISICAO, id);
        resp.setHeader("X-Id-Requisicao", id);
        try {
            if (props.isExigirHttps() && !req.isSecure()) {
                RespostaErro.escrever(req, resp, ErroApi.requisicaoInvalida("Use HTTPS."));
                return;
            }
            if (req.getContentLengthLong() > LIMITE_CORPO_BYTES) {
                RespostaErro.escrever(req, resp, ErroApi.requisicaoInvalida("Corpo da requisição maior que 10 KB."));
                return;
            }
            chain.doFilter(new CorpoLimitado(req), resp);
        } catch (Exception e) {
            LOG.error("{\"nivel\":\"ERROR\",\"id_requisicao\":\"{}\",\"excecao\":\"{}\"}", id, e.getClass().getName());
            RespostaErro.escrever(req, resp, ErroApi.erroInterno());
        } finally {
            registrar(req, resp, id, inicio);
        }
    }

    private void registrar(HttpServletRequest req, HttpServletResponse resp, String id, long inicio) {
        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
        Object usuarioId = req.getAttribute(UsuarioLogado.ATRIBUTO);
        int status = resp.getStatus();
        String nivel = status >= 500 ? "ERROR" : (status >= 400 ? "WARN" : "INFO");
        // Só metadados: nunca corpo, senha, token, e-mail, descrição ou valores (RNF-08).
        String linha = "{\"nivel\":\"" + nivel + "\""
                + ",\"id_requisicao\":\"" + id + "\""
                + ",\"usuario_id\":" + (usuarioId == null ? "null" : usuarioId)
                + ",\"metodo\":" + JsonErro.texto(req.getMethod())
                + ",\"rota\":" + JsonErro.texto(req.getRequestURI())
                + ",\"status\":" + status
                + ",\"duracao_ms\":" + duracaoMs + "}";
        if (status >= 500) {
            LOG.error(linha);
        } else if (status >= 400) {
            LOG.warn(linha);
        } else {
            LOG.info(linha);
        }
    }

    /** Corta a leitura do corpo em 10 KB, mesmo sem Content-Length (envio em partes). */
    static class CorpoLimitado extends HttpServletRequestWrapper {

        CorpoLimitado(HttpServletRequest req) {
            super(req);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream original = super.getInputStream();
            return new ServletInputStream() {
                private int lidos = 0;

                @Override
                public int read() throws IOException {
                    int b = original.read();
                    if (b != -1 && ++lidos > LIMITE_CORPO_BYTES) {
                        throw new IOException("Corpo maior que 10 KB");
                    }
                    return b;
                }

                @Override
                public int read(byte[] buf, int off, int len) throws IOException {
                    int n = original.read(buf, off, len);
                    if (n > 0) {
                        lidos += n;
                        if (lidos > LIMITE_CORPO_BYTES) {
                            throw new IOException("Corpo maior que 10 KB");
                        }
                    }
                    return n;
                }

                @Override
                public boolean isFinished() {
                    return original.isFinished();
                }

                @Override
                public boolean isReady() {
                    return original.isReady();
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    original.setReadListener(listener);
                }
            };
        }
    }
}
