package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.comum.Validacao;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.config.SobraProperties;
import com.Sobra.Aplicativo.dominio.Nomes;
import com.Sobra.Aplicativo.dto.AuthDtos.AuthResposta;
import com.Sobra.Aplicativo.dto.AuthDtos.CadastroRequest;
import com.Sobra.Aplicativo.dto.AuthDtos.LoginRequest;
import com.Sobra.Aplicativo.dto.AuthDtos.RecuperarSenhaRequest;
import com.Sobra.Aplicativo.dto.AuthDtos.RedefinirSenhaRequest;
import com.Sobra.Aplicativo.dto.AuthDtos.SessaoResposta;
import com.Sobra.Aplicativo.dto.AuthDtos.UsuarioResumo;
import com.Sobra.Aplicativo.model.RecuperacaoSenha;
import com.Sobra.Aplicativo.model.Usuario;
import com.Sobra.Aplicativo.repository.RecuperacaoSenhaRepository;
import com.Sobra.Aplicativo.repository.UsuarioRepository;
import com.Sobra.Aplicativo.seguranca.LimiteTaxa;
import com.Sobra.Aplicativo.seguranca.SenhaHasher;
import com.Sobra.Aplicativo.seguranca.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Cadastro, login, recuperação e redefinição de senha (OP-01, 02, 04 e 05). */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final RecuperacaoSenhaRepository recuperacoes;
    private final SessaoService sessoes;
    private final ConfiguracaoService configuracao;
    private final SenhaHasher senhas;
    private final LimiteTaxa limiteTaxa;
    private final EnvioEmail email;
    private final Relogio relogio;
    private final SobraProperties props;

    public AuthService(UsuarioRepository usuarios, RecuperacaoSenhaRepository recuperacoes, SessaoService sessoes,
                       ConfiguracaoService configuracao, SenhaHasher senhas, LimiteTaxa limiteTaxa,
                       EnvioEmail email, Relogio relogio, SobraProperties props) {
        this.usuarios = usuarios;
        this.recuperacoes = recuperacoes;
        this.sessoes = sessoes;
        this.configuracao = configuracao;
        this.senhas = senhas;
        this.limiteTaxa = limiteTaxa;
        this.email = email;
        this.relogio = relogio;
        this.props = props;
    }

    /** OP-01 · RF-01: cria usuário (senha só em hash) e já devolve uma sessão. */
    @Transactional
    public AuthResposta cadastrar(CadastroRequest req, String ip) {
        limitePublico("cadastro", ip);
        CadastroRequest r = req == null ? new CadastroRequest(null, null, null, null) : req;
        Validacao v = new Validacao();
        String nome = v.texto("nome_completo", r.nomeCompleto(), 2, 150);
        String emailNormalizado = v.email("email", r.email());
        String senha = v.senha("senha", r.senha());
        if (senha != null && !senha.equals(r.confirmacaoSenha())) {
            v.erro("confirmacao_senha", "SENHAS_DIFERENTES", "A confirmação não confere com a senha.");
        }
        v.lancarSeHouver();

        if (usuarios.existsByEmail(emailNormalizado)) {
            throw ErroApi.emailJaCadastrado();
        }
        Instant agora = relogio.agora();
        Usuario u = new Usuario();
        u.setNomeCompleto(nome);
        u.setEmail(emailNormalizado);
        u.setSenhaHash(senhas.gerar(senha));
        u.setNotificacoesAtivas(true);
        u.setCriadoEm(agora);
        u.setAtualizadoEm(agora);
        usuarios.saveAndFlush(u); // flush aqui: e-mail repetido por corrida vira 409 pelo nome da restrição

        return resposta(u, sessoes.criar(u.getId()), false);
    }

    /** OP-02 · RF-02: e-mail inexistente e senha errada têm a mesma resposta e o mesmo custo de tempo. */
    @Transactional
    public AuthResposta login(LoginRequest req, String ip) {
        LoginRequest r = req == null ? new LoginRequest(null, null) : req;
        Validacao v = new Validacao();
        Object emailBruto = r.email();
        if (emailBruto == null || (emailBruto instanceof String s && s.isBlank())) {
            v.erro("email", "OBRIGATORIO", "Informe o e-mail.");
        } else if (!(emailBruto instanceof String)) {
            v.erro("email", "FORMATO_INVALIDO", "Informe um e-mail válido.");
        }
        String senha = v.presente("senha", r.senha());
        v.lancarSeHouver();
        String emailNormalizado = Validacao.normalizarEmail((String) emailBruto);

        var lt = props.getLimiteTaxa();
        Duration janela = Duration.ofMinutes(lt.getLoginJanelaMinutos());
        limiteTaxa.consumir("login-ip:" + ip, lt.getLoginPorIp(), janela);
        limiteTaxa.consumir("login-email:" + Tokens.hash(emailNormalizado), lt.getLoginPorEmail(), janela);

        Optional<Usuario> encontrado = usuarios.findByEmail(emailNormalizado);
        if (encontrado.isEmpty()) {
            senhas.gastarTempoEquivalente(senha);
            throw ErroApi.credenciaisInvalidas();
        }
        Usuario u = encontrado.get();
        if (!senhas.confere(senha, u.getSenhaHash())) {
            throw ErroApi.credenciaisInvalidas();
        }
        return resposta(u, sessoes.criar(u.getId()), configuracao.concluida(u.getId()));
    }

    /**
     * OP-04 · RF-04: sempre a mesma resposta, exista ou não o e-mail.
     * Se existe: invalida pedidos anteriores, cria um novo (só o hash do token) e envia o link (D-08, opção C).
     */
    @Transactional
    public void pedirRecuperacao(RecuperarSenhaRequest req, String ip) {
        limitePublico("recuperacao", ip);
        Validacao v = new Validacao();
        String emailNormalizado = v.email("email", req == null ? null : req.email());
        v.lancarSeHouver();

        var lt = props.getLimiteTaxa();
        limiteTaxa.consumir("recuperacao-email:" + Tokens.hash(emailNormalizado), lt.getRecuperacaoPorEmail(),
                Duration.ofMinutes(lt.getRecuperacaoJanelaMinutos()));

        Optional<Usuario> encontrado = usuarios.findByEmail(emailNormalizado);
        if (encontrado.isEmpty()) {
            return;
        }
        Usuario u = encontrado.get();
        Instant agora = relogio.agora();
        recuperacoes.invalidarPendentes(u.getId(), agora);

        String token = Tokens.gerar();
        RecuperacaoSenha r = new RecuperacaoSenha();
        r.setUsuarioId(u.getId());
        r.setTokenHash(Tokens.hash(token));
        r.setCriadoEm(agora);
        r.setExpiraEm(agora.plus(Duration.ofMinutes(props.getRecuperacao().getValidadeMinutos())));
        recuperacoes.save(r);

        // O token vai no fragmento (#) para não aparecer em logs de servidor web.
        String link = props.getUrlPublica() + "/redefinir-senha.html#token=" + token;
        email.enviar(u.getEmail(), "Sobra: redefinição de senha",
                "Olá! Para criar uma nova senha, abra o link abaixo em até "
                        + props.getRecuperacao().getValidadeMinutos() + " minutos:\n" + link
                        + "\n\nSe não foi você, ignore este e-mail.");
    }

    /**
     * OP-05 · RF-06: troca a senha com um token válido, não usado e não vencido.
     * Os três casos inválidos têm a mesma resposta. Revoga todas as sessões da pessoa.
     */
    @Transactional
    public void redefinirSenha(RedefinirSenhaRequest req, String ip) {
        limitePublico("redefinicao", ip);
        RedefinirSenhaRequest r = req == null ? new RedefinirSenhaRequest(null, null, null) : req;
        Validacao v = new Validacao();
        String token = v.presente("token", r.token());
        String novaSenha = v.senha("nova_senha", r.novaSenha());
        if (novaSenha != null && !novaSenha.equals(r.confirmacaoSenha())) {
            v.erro("confirmacao_senha", "SENHAS_DIFERENTES", "A confirmação não confere com a senha.");
        }
        v.lancarSeHouver();

        Instant agora = relogio.agora();
        RecuperacaoSenha pedido = recuperacoes.findByTokenHash(Tokens.hash(token))
                .filter(p -> p.getUsadoEm() == null && p.getExpiraEm().isAfter(agora))
                .orElseThrow(ErroApi::tokenRecuperacaoInvalido);
        Usuario u = usuarios.findById(pedido.getUsuarioId()).orElseThrow(ErroApi::tokenRecuperacaoInvalido);

        u.setSenhaHash(senhas.gerar(novaSenha));
        u.setAtualizadoEm(agora);
        pedido.setUsadoEm(agora);
        sessoes.revogarTodas(u.getId());
        email.enviar(u.getEmail(), "Sobra: sua senha foi alterada",
                "A senha da sua conta Sobra foi alterada. Se não foi você, peça uma nova recuperação de senha.");
    }

    private void limitePublico(String rota, String ip) {
        var lt = props.getLimiteTaxa();
        limiteTaxa.consumir("publico-" + rota + ":" + ip, lt.getPublicoPorIp(),
                Duration.ofMinutes(lt.getPublicoJanelaMinutos()));
    }

    private AuthResposta resposta(Usuario u, SessaoService.NovaSessao sessao, boolean configuracaoConcluida) {
        return new AuthResposta(
                new UsuarioResumo(u.getId(), u.getNomeCompleto(), u.getEmail(), Nomes.iniciais(u.getNomeCompleto())),
                new SessaoResposta(sessao.token(), Parametros.instante(sessao.expiraEm())),
                configuracaoConcluida);
    }
}
