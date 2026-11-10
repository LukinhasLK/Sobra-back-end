package com.Sobra.Aplicativo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Histórico da renda (seção 5.8, opção B): cada linha vale a partir de mes_inicio (sempre dia 1). */
@Entity
@Table(name = "renda_vigencia")
@IdClass(RendaVigenciaId.class)
public class RendaVigencia {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Id
    @Column(name = "mes_inicio")
    private LocalDate mesInicio;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected RendaVigencia() {
    }

    public RendaVigencia(Long usuarioId, LocalDate mesInicio, BigDecimal valor, Instant criadoEm) {
        this.usuarioId = usuarioId;
        this.mesInicio = mesInicio;
        this.valor = valor;
        this.criadoEm = criadoEm;
    }

    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public Long getUsuarioId() { return usuarioId; }
    public LocalDate getMesInicio() { return mesInicio; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public Instant getCriadoEm() { return criadoEm; }
}
