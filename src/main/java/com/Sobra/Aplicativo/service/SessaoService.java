package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.config.SobraProperties;
import com.Sobra.Aplicativo.model.Sessao;
import com.Sobra.Aplicativo.repository.SessaoRepository;
import com.Sobra.Aplicativo.seguranca.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Sessões com validade deslizante (D-07, opção B):
 * vence após 30 dias sem uso; cada uso empurra expira_em (no máximo uma gravação por hora),
 * até o teto de 90 dias desde criado_em.
 */
@Service
public class SessaoService {

    private static final Duration INTERVALO_MINIMO_RENOVACAO = Duration.ofHours(1);

    public record NovaSessao(String token, Instant expiraEm) {
    }

    private final SessaoRepository repo;
    private final Relogio relogio;
    private final SobraProperties props;

    public SessaoService(SessaoRepository repo, Relogio relogio, SobraProperties props) {
        this.repo = repo;
        this.relogio = relogio;
        this.props = props;
    }

    @Transactional
    public NovaSessao criar(Long usuarioId) {
        Instant agora = relogio.agora();
        String token = Tokens.gerar();
        Sessao s = new Sessao();
        s.setUsuarioId(usuarioId);
        s.setTokenHash(Tokens.hash(token));
        s.setCriadoEm(agora);
        s.setExpiraEm(agora.plus(Duration.ofDays(props.getSessao().getValidadeDias())));
        repo.save(s);
        return new NovaSessao(token, s.getExpiraEm());
    }

    /** Devolve o id do dono do token, ou lança NAO_AUTENTICADO / SESSAO_EXPIRADA (RF-05). */
    @Transactional
    public Long validar(String token) {
        Sessao s = repo.findByTokenHash(Tokens.hash(token)).orElseThrow(ErroApi::naoAutenticado);
        Instant agora = relogio.agora();
        if (s.getRevogadaEm() != null || !s.getExpiraEm().isAfter(agora)) {
            throw ErroApi.sessaoExpirada();
        }
        Instant teto = s.getCriadoEm().plus(Duration.ofDays(props.getSessao().getTetoDias()));
        Instant novaValidade = agora.plus(Duration.ofDays(props.getSessao().getValidadeDias()));
        if (novaValidade.isAfter(teto)) {
            novaValidade = teto;
        }
        if (novaValidade.isAfter(s.getExpiraEm().plus(INTERVALO_MINIMO_RENOVACAO))) {
            s.setExpiraEm(novaValidade); // gravado no fim da transação
        }
        return s.getUsuarioId();
    }

    /**
     * Logout (RF-03): revoga a sessão do token. Idempotente: repetir com o mesmo token devolve 204.
     * Token ausente ou desconhecido = NAO_AUTENTICADO.
     */
    @Transactional
    public void encerrar(String token) {
        if (token == null) {
            throw ErroApi.naoAutenticado();
        }
        Optional<Sessao> sessao = repo.findByTokenHash(Tokens.hash(token));
        Sessao s = sessao.orElseThrow(ErroApi::naoAutenticado);
        if (s.getRevogadaEm() == null) {
            Instant agora = relogio.agora();
            s.setRevogadaEm(agora.isBefore(s.getCriadoEm()) ? s.getCriadoEm() : agora);
        }
    }

    @Transactional
    public void revogarTodas(Long usuarioId) {
        repo.revogarTodas(usuarioId, relogio.agora());
    }
}
