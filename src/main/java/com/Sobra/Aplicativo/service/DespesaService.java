package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.comum.Validacao;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.dominio.Dinheiro;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.CategoriaRef;
import com.Sobra.Aplicativo.dto.DespesaDtos.DespesaRequest;
import com.Sobra.Aplicativo.dto.DespesaDtos.DespesaResposta;
import com.Sobra.Aplicativo.dto.DespesaDtos.DespesasMesResposta;
import com.Sobra.Aplicativo.model.Categoria;
import com.Sobra.Aplicativo.model.Despesa;
import com.Sobra.Aplicativo.repository.CategoriaRepository;
import com.Sobra.Aplicativo.repository.DespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** OP-11 e OP-12 · RF-11 e RF-12. */
@Service
public class DespesaService {

    private final DespesaRepository despesas;
    private final CategoriaRepository categorias;
    private final Relogio relogio;

    public DespesaService(DespesaRepository despesas, CategoriaRepository categorias, Relogio relogio) {
        this.despesas = despesas;
        this.categorias = categorias;
        this.relogio = relogio;
    }

    /** Mês sem despesas: 200 com lista vazia e total "0.00" (nunca 404). */
    @Transactional(readOnly = true)
    public DespesasMesResposta doMes(Long usuarioId, YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.plusMonths(1).atDay(1);
        List<DespesaResposta> lista = despesas.doMes(usuarioId, inicio, fim).stream()   // Q2a
                .map(DespesaService::resposta)
                .toList();
        BigDecimal total = despesas.totalDoMes(usuarioId, inicio, fim);                   // Q2b
        return new DespesasMesResposta(Parametros.texto(mes), Dinheiro.texto(total), lista);
    }

    @Transactional
    public DespesaResposta criar(Long usuarioId, DespesaRequest req) {
        DespesaRequest r = req == null ? new DespesaRequest(null, null, null, null) : req;
        Validacao v = new Validacao();
        String descricao = v.texto("descricao", r.descricao(), 1, 120);
        Categoria categoria = null;
        if (r.categoriaCodigo() == null) {
            v.erro("categoria_codigo", "OBRIGATORIO", "Escolha uma categoria.");
        } else {
            categoria = r.categoriaCodigo() instanceof String codigo
                    ? categorias.findByCodigo(codigo.trim()).orElse(null) : null;
            if (categoria == null) {
                v.erro("categoria_codigo", "CATEGORIA_DESCONHECIDA", "Categoria desconhecida.");
            }
        }
        LocalDate data = v.data("data", r.data(), relogio.hoje());
        BigDecimal valor = v.valorPositivo("valor", r.valor());
        v.lancarSeHouver();

        Despesa d = new Despesa();
        d.setUsuarioId(usuarioId);          // sempre o dono da sessão
        d.setCategoria(categoria);
        d.setDescricao(descricao);
        d.setData(data);
        d.setValor(valor);
        d.setCriadoEm(relogio.agora());
        despesas.saveAndFlush(d);
        return resposta(d);
    }

    private static DespesaResposta resposta(Despesa d) {
        Categoria c = d.getCategoria();
        return new DespesaResposta(d.getId(), d.getDescricao(), new CategoriaRef(c.getCodigo(), c.getNome()),
                Parametros.dia(d.getData()), Dinheiro.texto(d.getValor()));
    }
}
