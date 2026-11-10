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

/**
 * Meta de economia. O valor acumulado NÃO é coluna: é sempre a soma dos aportes (seção 5.7).
 * aporte_mensal é o plano definido na criação (D-05).
 */
@Entity
@Table(name = "objetivo")
public class Objetivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "valor_meta", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMeta;

    @Column(name = "prazo_meses", nullable = false)
    private Integer prazoMeses;

    @Column(name = "aporte_mensal", nullable = false, precision = 12, scale = 2)
    private BigDecimal aporteMensal;

    @Column(name = "imagem_url", length = 500)
    private String imagemUrl;

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
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getValorMeta() { return valorMeta; }
    public void setValorMeta(BigDecimal valorMeta) { this.valorMeta = valorMeta; }
    public Integer getPrazoMeses() { return prazoMeses; }
    public void setPrazoMeses(Integer prazoMeses) { this.prazoMeses = prazoMeses; }
    public BigDecimal getAporteMensal() { return aporteMensal; }
    public void setAporteMensal(BigDecimal aporteMensal) { this.aporteMensal = aporteMensal; }
    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
