package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.comum.Validacao;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.config.SobraProperties;
import com.Sobra.Aplicativo.dominio.Dinheiro;
import com.Sobra.Aplicativo.dominio.Simulador;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.CalculoResposta;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.PontoSerie;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.SimulacaoRequest;
import com.Sobra.Aplicativo.dto.SimulacaoDtos.SimulacaoSalva;
import com.Sobra.Aplicativo.model.Simulacao;
import com.Sobra.Aplicativo.repository.SimulacaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** OP-17 a OP-19 · RF-17 a RF-19. Fórmula provisória D-01 (RN-07). */
@Service
public class SimulacaoService {

    private final SimulacaoRepository simulacoes;
    private final Relogio relogio;
    private final SobraProperties props;

    public SimulacaoService(SimulacaoRepository simulacoes, Relogio relogio, SobraProperties props) {
        this.simulacoes = simulacoes;
        this.relogio = relogio;
        this.props = props;
    }

    private record Entrada(BigDecimal valorInicial, BigDecimal aporteMensal, int prazoMeses) {
    }

    /** OP-17: só calcula, não grava nada. */
    public CalculoResposta calcular(SimulacaoRequest req) {
        Entrada e = validar(req);
        Simulador.Resultado r = Simulador.calcular(e.valorInicial(), e.aporteMensal(), e.prazoMeses(), taxa());
        List<PontoSerie> serie = r.serie().stream()
                .map(p -> new PontoSerie(p.mes(), Dinheiro.texto(p.comRendimento()), Dinheiro.texto(p.semRendimento())))
                .toList();
        return new CalculoResposta(Dinheiro.taxa(r.taxaMensal()), Dinheiro.texto(r.totalInvestido()),
                Dinheiro.texto(r.totalAportes()), Dinheiro.texto(r.estimativaFinal()),
                Dinheiro.texto(r.rendimentoEstimado()), serie);
    }

    /** OP-18: recalcula no servidor e grava a taxa usada; "resultado" do cliente é ignorado. */
    @Transactional
    public SimulacaoSalva salvar(Long usuarioId, SimulacaoRequest req) {
        Entrada e = validar(req);
        BigDecimal taxa = taxa();
        Simulador.Resultado r = Simulador.calcular(e.valorInicial(), e.aporteMensal(), e.prazoMeses(), taxa);

        Simulacao s = new Simulacao();
        s.setUsuarioId(usuarioId);
        s.setValorInicial(e.valorInicial());
        s.setAporteMensal(e.aporteMensal());
        s.setPrazoMeses(e.prazoMeses());
        s.setTaxaMensal(taxa);
        s.setResultado(r.estimativaFinal());
        s.setCriadoEm(relogio.agora());
        simulacoes.saveAndFlush(s);
        return resposta(s);
    }

    /** OP-19: mais recente primeiro, até "limite" itens. */
    @Transactional(readOnly = true)
    public List<SimulacaoSalva> listar(Long usuarioId, int limite) {
        return simulacoes.doUsuario(usuarioId, PageRequest.of(0, limite)).stream()
                .map(SimulacaoService::resposta)
                .toList();
    }

    private Entrada validar(SimulacaoRequest req) {
        SimulacaoRequest r = req == null ? new SimulacaoRequest(null, null, null) : req;
        Validacao v = new Validacao();
        BigDecimal inicial = v.valorNaoNegativo("valor_inicial", r.valorInicial());
        BigDecimal aporte = v.valorNaoNegativo("aporte_mensal", r.aporteMensal());
        Integer prazo = v.inteiro("prazo_meses", r.prazoMeses(), 1, props.getLimites().getPrazoSimulacaoMax());
        if (inicial != null && aporte != null && inicial.signum() == 0 && aporte.signum() == 0) {
            v.erro("aporte_mensal", "DEVE_SER_POSITIVO", "Informe um valor inicial ou um aporte maior que zero.");
        }
        v.lancarSeHouver();
        return new Entrada(inicial, aporte, prazo);
    }

    private BigDecimal taxa() {
        return props.getTaxaSimulacao().setScale(6);
    }

    private static SimulacaoSalva resposta(Simulacao s) {
        return new SimulacaoSalva(s.getId(), Parametros.instante(s.getCriadoEm()), Dinheiro.texto(s.getValorInicial()),
                Dinheiro.texto(s.getAporteMensal()), s.getPrazoMeses(), Dinheiro.taxa(s.getTaxaMensal()),
                Dinheiro.texto(s.getResultado()));
    }
}
