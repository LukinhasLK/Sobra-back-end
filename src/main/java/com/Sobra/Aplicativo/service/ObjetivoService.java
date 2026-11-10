package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.comum.Validacao;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.config.SobraProperties;
import com.Sobra.Aplicativo.dominio.Dinheiro;
import com.Sobra.Aplicativo.dominio.RegrasObjetivo;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.AporteCriadoResposta;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.AporteRequest;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.AporteResposta;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivoCriado;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivoDetalhe;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivoItem;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ObjetivoRequest;
import com.Sobra.Aplicativo.dto.ObjetivoDtos.ProgressoObjetivo;
import com.Sobra.Aplicativo.model.Aporte;
import com.Sobra.Aplicativo.model.Objetivo;
import com.Sobra.Aplicativo.repository.AporteRepository;
import com.Sobra.Aplicativo.repository.ObjetivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** OP-13 a OP-16 · RF-13 a RF-16. Regras RN-03, RN-04 e RN-08. */
@Service
public class ObjetivoService {

    private final ObjetivoRepository objetivos;
    private final AporteRepository aportes;
    private final Relogio relogio;
    private final SobraProperties props;

    public ObjetivoService(ObjetivoRepository objetivos, AporteRepository aportes, Relogio relogio,
                           SobraProperties props) {
        this.objetivos = objetivos;
        this.aportes = aportes;
        this.relogio = relogio;
        this.props = props;
    }

    /** Linha das consultas Q3/Q4a já convertida. */
    private record Cabecalho(Long id, String nome, BigDecimal valorMeta, Integer prazoMeses,
                             BigDecimal aporteMensal, String imagemUrl,
                             BigDecimal acumulado, BigDecimal percentual) {

        boolean concluido() {
            return RegrasObjetivo.concluido(acumulado, valorMeta);
        }

        String previsao(YearMonth referencia) {
            return Parametros.texto(RegrasObjetivo.previsaoConclusao(valorMeta, acumulado, aporteMensal, referencia));
        }
    }

    /** Q3. Sem objetivos: lista vazia. */
    @Transactional(readOnly = true)
    public List<ObjetivoItem> listar(Long usuarioId) {
        return objetivos.listarComAcumulado(usuarioId).stream()
                .map(l -> new ObjetivoItem(
                        ((Number) l[0]).longValue(),
                        (String) l[1],
                        Dinheiro.texto(Dinheiro.de(l[4])),
                        Dinheiro.texto(Dinheiro.de(l[2])),
                        Dinheiro.texto(Dinheiro.de(l[5])),
                        (String) l[3]))
                .toList();
    }

    /** OP-14: aporte_mensal = meta ÷ prazo (RN-04); a previsão sai já na resposta. */
    @Transactional
    public ObjetivoCriado criar(Long usuarioId, ObjetivoRequest req) {
        ObjetivoRequest r = req == null ? new ObjetivoRequest(null, null, null) : req;
        Validacao v = new Validacao();
        String nome = v.texto("nome", r.nome(), 1, 80);
        BigDecimal meta = v.valorPositivo("valor_meta", r.valorMeta());
        Integer prazo = v.inteiro("prazo_meses", r.prazoMeses(), 1, props.getLimites().getPrazoObjetivoMax());
        v.lancarSeHouver();

        Objetivo o = new Objetivo();
        o.setUsuarioId(usuarioId);
        o.setNome(nome);
        o.setValorMeta(meta);
        o.setPrazoMeses(prazo);
        o.setAporteMensal(RegrasObjetivo.aporteSugerido(meta, prazo));
        o.setImagemUrl(null); // D-10: catálogo de imagens ainda não escolhido; nulo = app mostra o padrão
        o.setCriadoEm(relogio.agora());
        objetivos.saveAndFlush(o);

        Cabecalho c = cabecalho(o.getId(), usuarioId);
        return new ObjetivoCriado(c.id(), c.nome(), Dinheiro.texto(c.valorMeta()), Dinheiro.texto(c.acumulado()),
                Dinheiro.texto(c.percentual()), c.prazoMeses(), Dinheiro.texto(c.aporteMensal()),
                c.previsao(relogio.mesCorrente()), c.concluido(), c.imagemUrl());
    }

    /** OP-15: inexistente ou de outro usuário → 404 (mesma resposta, regra 4 da seção 5.12). */
    @Transactional(readOnly = true)
    public ObjetivoDetalhe detalhe(Long usuarioId, long objetivoId) {
        Cabecalho c = cabecalho(objetivoId, usuarioId);
        List<AporteResposta> historico = aportes.historico(objetivoId, usuarioId).stream()   // Q4b
                .map(ObjetivoService::resposta)
                .toList();
        return new ObjetivoDetalhe(c.id(), c.nome(), Dinheiro.texto(c.valorMeta()), Dinheiro.texto(c.acumulado()),
                Dinheiro.texto(c.percentual()), c.prazoMeses(), Dinheiro.texto(c.aporteMensal()),
                c.previsao(relogio.mesCorrente()), c.concluido(), c.imagemUrl(), historico);
    }

    /** OP-16: confere o dono do objetivo ANTES de gravar; a resposta traz o progresso atualizado. */
    @Transactional
    public AporteCriadoResposta registrarAporte(Long usuarioId, long objetivoId, AporteRequest req) {
        Objetivo o = objetivos.findByIdAndUsuarioId(objetivoId, usuarioId).orElseThrow(ErroApi::naoEncontrado);

        AporteRequest r = req == null ? new AporteRequest(null, null, null) : req;
        Validacao v = new Validacao();
        String tipo = null;
        if (r.tipo() == null) {
            v.erro("tipo", "OBRIGATORIO", "Informe MENSAL ou EXTRA.");
        } else if (Aporte.MENSAL.equals(r.tipo()) || Aporte.EXTRA.equals(r.tipo())) {
            tipo = (String) r.tipo();
        } else {
            v.erro("tipo", "FORMATO_INVALIDO", "Use MENSAL ou EXTRA.");
        }
        BigDecimal valor = v.valorPositivo("valor", r.valor());
        LocalDate data = v.data("data", r.data(), relogio.hoje());
        v.lancarSeHouver();

        Aporte a = new Aporte();
        a.setObjetivoId(o.getId());
        a.setTipo(tipo);
        a.setValor(valor);
        a.setData(data);
        a.setCriadoEm(relogio.agora());
        aportes.saveAndFlush(a);

        Cabecalho c = cabecalho(o.getId(), usuarioId);
        return new AporteCriadoResposta(resposta(a), new ProgressoObjetivo(
                Dinheiro.texto(c.acumulado()), Dinheiro.texto(c.percentual()),
                c.previsao(relogio.mesCorrente()), c.concluido()));
    }

    /** Q4a. */
    private Cabecalho cabecalho(long objetivoId, Long usuarioId) {
        List<Object[]> linhas = objetivos.detalhe(objetivoId, usuarioId);
        if (linhas.isEmpty()) {
            throw ErroApi.naoEncontrado();
        }
        Object[] l = linhas.get(0);
        return new Cabecalho(((Number) l[0]).longValue(), (String) l[1], Dinheiro.de(l[2]),
                ((Number) l[3]).intValue(), Dinheiro.de(l[4]), (String) l[5], Dinheiro.de(l[6]), Dinheiro.de(l[7]));
    }

    private static AporteResposta resposta(Aporte a) {
        return new AporteResposta(a.getId(), a.getTipo(), a.getTitulo(), Parametros.dia(a.getData()),
                Dinheiro.texto(a.getValor()));
    }
}
