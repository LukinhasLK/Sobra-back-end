package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.Validacao;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.dominio.Nomes;
import com.Sobra.Aplicativo.dto.PerfilDtos.NotificacoesRequest;
import com.Sobra.Aplicativo.dto.PerfilDtos.NotificacoesResposta;
import com.Sobra.Aplicativo.dto.PerfilDtos.PerfilResposta;
import com.Sobra.Aplicativo.model.Usuario;
import com.Sobra.Aplicativo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OP-20 e OP-21 · RF-20 e RF-21. */
@Service
public class PerfilService {

    private final UsuarioRepository usuarios;
    private final Relogio relogio;

    public PerfilService(UsuarioRepository usuarios, Relogio relogio) {
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public PerfilResposta perfil(Long usuarioId) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(ErroApi::naoAutenticado);   // Q6
        return new PerfilResposta(u.getNomeCompleto(), u.getEmail(), Nomes.iniciais(u.getNomeCompleto()),
                u.getAvatarUrl(), u.isNotificacoesAtivas());
    }

    /** Idempotente: repetir com o mesmo valor não dá erro. */
    @Transactional
    public NotificacoesResposta alterarNotificacoes(Long usuarioId, NotificacoesRequest req) {
        Validacao v = new Validacao();
        Boolean ativas = v.booleano("notificacoes_ativas", req == null ? null : req.notificacoesAtivas());
        v.lancarSeHouver();
        Usuario u = usuarios.findById(usuarioId).orElseThrow(ErroApi::naoAutenticado);
        if (u.isNotificacoesAtivas() != ativas) {
            u.setNotificacoesAtivas(ativas);
            u.setAtualizadoEm(relogio.agora()); // P-10
        }
        return new NotificacoesResposta(u.isNotificacoesAtivas());
    }
}
