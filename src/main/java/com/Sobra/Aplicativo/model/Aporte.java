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
import java.time.LocalDate;

/**
 * Valor guardado em um objetivo. Não tem usuario_id (P-06): o dono se descobre pelo objetivo,
 * então NUNCA consulte aporte sem ligar ao objetivo do usuário (regra 3 da seção 5.12).
 */
@Entity
@Table(name = "aporte")
public class Aporte {

    public static final String MENSAL = "MENSAL";
    public static final String EXTRA = "EXTRA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "objetivo_id", nullable = false)
    private Long objetivoId;

    /** D-06: MENSAL ou EXTRA; o título exibido é montado pelo servidor. */
    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    /** "Aporte Mensal" / "Aporte Extra". */
    public String getTitulo() {
        return MENSAL.equals(tipo) ? "Aporte Mensal" : "Aporte Extra";
    }

    public Long getId() { return id; }
    public Long getObjetivoId() { return objetivoId; }
    public void setObjetivoId(Long objetivoId) { this.objetivoId = objetivoId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
