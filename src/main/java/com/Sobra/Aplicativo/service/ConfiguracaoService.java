package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.comum.Validacao;
import com.Sobra.Aplicativo.config.Relogio;
import com.Sobra.Aplicativo.dominio.Dinheiro;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.CategoriaEstimativa;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.CategoriaRef;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.CategoriasRequest;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.ConfiguracaoResposta;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.EstimativasResposta;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.RendaRequest;
import com.Sobra.Aplicativo.dto.ConfiguracaoDtos.RendaResposta;
import com.Sobra.Aplicativo.model.Categoria;
import com.Sobra.Aplicativo.model.OrcamentoCategoria;
import com.Sobra.Aplicativo.model.OrcamentoCategoriaId;
import com.Sobra.Aplicativo.model.RendaVigencia;
import com.Sobra.Aplicativo.model.RendaVigenciaId;
import com.Sobra.Aplicativo.repository.CategoriaRepository;
import com.Sobra.Aplicativo.repository.OrcamentoCategoriaRepository;
import com.Sobra.Aplicativo.repository.RendaVigenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Configuração inicial (OP-06, 07, 08) e categorias de referência (OP-09). */
@Service
public class ConfiguracaoService {

    public static final int TOTAL_CATEGORIAS = 7;

    private final RendaVigenciaRepository rendas;
    private final OrcamentoCategoriaRepository orcamentos;
    private final CategoriaRepository categorias;
    private final Relogio relogio;

    public ConfiguracaoService(RendaVigenciaRepository rendas, OrcamentoCategoriaRepository orcamentos,
                               CategoriaRepository categorias, Relogio relogio) {
        this.rendas = rendas;
        this.orcamentos = orcamentos;
        this.categorias = categorias;
        this.relogio = relogio;
    }

    /** RF-09: etapa 1 = existe renda; etapa 2 = existem as 7 linhas de estimativa. */
    @Transactional(readOnly = true)
    public boolean concluida(Long usuarioId) {
        return rendas.contarDoUsuario(usuarioId) > 0 && orcamentos.contarDoUsuario(usuarioId) == TOTAL_CATEGORIAS;
    }

    /** OP-06. */
    @Transactional(readOnly = true)
    public ConfiguracaoResposta estado(Long usuarioId) {
        boolean etapa1 = rendas.contarDoUsuario(usuarioId) > 0;
        boolean etapa2 = orcamentos.contarDoUsuario(usuarioId) == TOTAL_CATEGORIAS;
        BigDecimal renda = rendas.rendaVigente(usuarioId, relogio.mesCorrente().atDay(1));
        return new ConfiguracaoResposta(Dinheiro.texto(renda), estimativas(usuarioId), etapa1, etapa2, etapa1 && etapa2);
    }

    /**
     * OP-07 · RF-07: renda com vigência a partir do 1º dia do mês corrente.
     * Mesmo mês: substitui o valor. Outro mês: nova linha; meses anteriores não mudam (seção 5.8).
     */
    @Transactional
    public RendaResposta registrarRenda(Long usuarioId, RendaRequest req) {
        Validacao v = new Validacao();
        BigDecimal valor = v.valorPositivo("valor", req == null ? null : req.valor());
        v.lancarSeHouver();

        YearMonth mes = relogio.mesCorrente();
        LocalDate inicio = mes.atDay(1);
        RendaVigencia linha = rendas.findById(new RendaVigenciaId(usuarioId, inicio)).orElse(null);
        if (linha == null) {
            rendas.save(new RendaVigencia(usuarioId, inicio, valor, relogio.agora()));
        } else {
            linha.setValor(valor);
        }
        return new RendaResposta(Dinheiro.texto(valor), Parametros.texto(mes));
    }

    /**
     * OP-08 · RF-08: exatamente as 7 categorias, sem repetição; valor positivo ou nulo (inativa).
     * Tudo ou nada: valida tudo antes de gravar qualquer linha.
     */
    @Transactional
    public EstimativasResposta registrarCategorias(Long usuarioId, CategoriasRequest req) {
        Object bruto = req == null ? null : req.categorias();
        if (!(bruto instanceof List<?> itens)) {
            throw ErroApi.validacao("categorias", "CATEGORIAS_INCOMPLETAS", "Envie a lista com as 7 categorias.");
        }
        Map<String, Categoria> porCodigo = new HashMap<>();
        for (Categoria c : categorias.findAllByOrderByIdAsc()) {
            porCodigo.put(c.getCodigo(), c);
        }

        Validacao v = new Validacao();
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        boolean repetida = false;
        for (int i = 0; i < itens.size(); i++) {
            String campo = "categorias[" + i + "]";
            if (!(itens.get(i) instanceof Map<?, ?> item)) {
                v.erro(campo, "FORMATO_INVALIDO", "Cada item deve ter codigo e valor_estimado.");
                continue;
            }
            Object codigoBruto = item.get("codigo");
            String codigo = codigoBruto instanceof String s ? s.trim() : null;
            if (codigo == null || !porCodigo.containsKey(codigo)) {
                v.erro(campo + ".codigo", "CATEGORIA_DESCONHECIDA", "Categoria desconhecida.");
                continue;
            }
            if (valores.containsKey(codigo)) {
                repetida = true;
                continue;
            }
            valores.put(codigo, v.valorPositivoOpcional(campo + ".valor_estimado", item.get("valor_estimado")));
        }
        if (repetida || itens.size() != TOTAL_CATEGORIAS || (!v.temErros() && valores.size() != TOTAL_CATEGORIAS)) {
            v.erro("categorias", "CATEGORIAS_INCOMPLETAS", "Envie exatamente as 7 categorias, sem repetir.");
        }
        v.lancarSeHouver();

        for (Map.Entry<String, BigDecimal> e : valores.entrySet()) {
            Short categoriaId = porCodigo.get(e.getKey()).getId();
            OrcamentoCategoria linha = orcamentos.findById(new OrcamentoCategoriaId(usuarioId, categoriaId)).orElse(null);
            if (linha == null) {
                orcamentos.save(new OrcamentoCategoria(usuarioId, categoriaId, e.getValue()));
            } else {
                linha.setValorEstimado(e.getValue());
            }
        }
        orcamentos.flush();
        return new EstimativasResposta(estimativas(usuarioId));
    }

    /** Q7. */
    private List<CategoriaEstimativa> estimativas(Long usuarioId) {
        List<CategoriaEstimativa> lista = new ArrayList<>();
        for (Object[] linha : orcamentos.estimativas(usuarioId)) {
            lista.add(new CategoriaEstimativa((String) linha[0], (String) linha[1],
                    Dinheiro.texto(Dinheiro.de(linha[2]))));
        }
        return lista;
    }

    /** OP-09 · RF-22: as 7 categorias na ordem do id. */
    @Transactional(readOnly = true)
    public List<CategoriaRef> categorias() {
        return categorias.findAllByOrderByIdAsc().stream()
                .map(c -> new CategoriaRef(c.getCodigo(), c.getNome()))
                .toList();
    }
}
