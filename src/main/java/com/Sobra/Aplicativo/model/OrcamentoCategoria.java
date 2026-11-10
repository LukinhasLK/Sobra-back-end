package com.Sobra.Aplicativo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Estimativa mensal por categoria (passo 2 da configuração). valor_estimado nulo = categoria inativa (P-05). */
@Entity
@Table(name = "orcamento_categoria")
@IdClass(OrcamentoCategoriaId.class)
public class OrcamentoCategoria {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Id
    @Column(name = "categoria_id")
    private Short categoriaId;

    @Column(name = "valor_estimado", precision = 12, scale = 2)
    private BigDecimal valorEstimado;

    protected OrcamentoCategoria() {
    }

    public OrcamentoCategoria(Long usuarioId, Short categoriaId, BigDecimal valorEstimado) {
        this.usuarioId = usuarioId;
        this.categoriaId = categoriaId;
        this.valorEstimado = valorEstimado;
    }

    public Long getUsuarioId() { return usuarioId; }
    public Short getCategoriaId() { return categoriaId; }
    public BigDecimal getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(BigDecimal valorEstimado) { this.valorEstimado = valorEstimado; }
}
