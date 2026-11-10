package com.Sobra.Aplicativo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Simulação salva: uma "fotografia" com a taxa usada na época (seção 5.7, D-01). */
@Entity
@Table(name = "simulacao")
public class Simulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "valor_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorInicial;

    @Column(name = "aporte_mensal", nullable = false, precision = 12, scale = 2)
    private BigDecimal aporteMensal;

    @Column(name = "prazo_meses", nullable = false)
    private Integer prazoMeses;

    @Column(name = "taxa_mensal", nullable = false, precision = 8, scale = 6)
    private BigDecimal taxaMensal;

    /** Estimativa final calculada na época. */
    @Column(name = "resultado", nullable = false, precision = 15, scale = 2)
    private BigDecimal resultado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public BigDecimal getValorInicial() { return valorInicial; }
    public void setValorInicial(BigDecimal valorInicial) { this.valorInicial = valorInicial; }
    public BigDecimal getAporteMensal() { return aporteMensal; }
    public void setAporteMensal(BigDecimal aporteMensal) { this.aporteMensal = aporteMensal; }
    public Integer getPrazoMeses() { return prazoMeses; }
    public void setPrazoMeses(Integer prazoMeses) { this.prazoMeses = prazoMeses; }
    public BigDecimal getTaxaMensal() { return taxaMensal; }
    public void setTaxaMensal(BigDecimal taxaMensal) { this.taxaMensal = taxaMensal; }
    public BigDecimal getResultado() { return resultado; }
    public void setResultado(BigDecimal resultado) { this.resultado = resultado; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
