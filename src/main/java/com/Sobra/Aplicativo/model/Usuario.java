package com.Sobra.Aplicativo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** Tabela usuario (seção 5.2). Sem coluna de renda: a renda fica em renda_vigencia (5.8). */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "notificacoes_ativas", nullable = false)
    private boolean notificacoesAtivas = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /** P-10: mantido pela aplicação a cada alteração. */
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
        if (atualizadoEm == null) {
            atualizadoEm = criadoEm;
        }
    }

    public Long getId() { return id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isNotificacoesAtivas() { return notificacoesAtivas; }
    public void setNotificacoesAtivas(boolean notificacoesAtivas) { this.notificacoesAtivas = notificacoesAtivas; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
