package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.Parametros;
import com.Sobra.Aplicativo.dominio.Dinheiro;
import com.Sobra.Aplicativo.dominio.Nomes;
import com.Sobra.Aplicativo.dto.HomeDtos.GastoCategoria;
import com.Sobra.Aplicativo.dto.HomeDtos.HomeResposta;
import com.Sobra.Aplicativo.dto.HomeDtos.UsuarioHome;
import com.Sobra.Aplicativo.model.Usuario;
import com.Sobra.Aplicativo.repository.DespesaRepository;
import com.Sobra.Aplicativo.repository.RendaVigenciaRepository;
import com.Sobra.Aplicativo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** OP-10 · RF-10. RN-02: o banco soma; o servidor calcula disponível = renda − gastos. */
@Service
public class HomeService {

    private final UsuarioRepository usuarios;
    private final RendaVigenciaRepository rendas;
    private final DespesaRepository despesas;

    public HomeService(UsuarioRepository usuarios, RendaVigenciaRepository rendas, DespesaRepository despesas) {
        this.usuarios = usuarios;
        this.rendas = rendas;
        this.despesas = despesas;
    }

    @Transactional(readOnly = true)
    public HomeResposta resumo(Long usuarioId, YearMonth mes) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(ErroApi::naoAutenticado);  // Q1c
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.plusMonths(1).atDay(1);

        BigDecimal renda = rendas.rendaVigente(usuarioId, inicio);                         // Q1a
        List<GastoCategoria> categorias = new ArrayList<>();
        BigDecimal gastos = Dinheiro.ZERO;
        for (Object[] linha : despesas.gastoPorCategoria(usuarioId, inicio, fim)) {       // Q1b
            BigDecimal total = Dinheiro.de(linha[2]);
            gastos = gastos.add(total);
            categorias.add(new GastoCategoria((String) linha[0], (String) linha[1], Dinheiro.texto(total)));
        }
        BigDecimal disponivel = renda == null ? null : renda.subtract(gastos);

        return new HomeResposta(
                Parametros.texto(mes),
                new UsuarioHome(Nomes.primeiroNome(u.getNomeCompleto()), u.getAvatarUrl()),
                Dinheiro.texto(renda),
                Dinheiro.texto(gastos),
                Dinheiro.texto(disponivel),
                categorias);
    }
}
