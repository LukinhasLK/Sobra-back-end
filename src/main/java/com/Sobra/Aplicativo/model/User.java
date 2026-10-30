package com.Sobra.Aplicativo.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private String nomeCompleto;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private boolean notificacao;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime criacao;

    @UpdateTimestamp
    private LocalDateTime atualizacao;

    @OneToMany(mappedBy = "user")
    private List<Despesas>despesas = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Simulacao>simulacao = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<RendaVigencia>rendaVigencias = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<RecuperacaoSenha>recuperacaoSenhas = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Sessao>sessao = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Objetivo>objetivos = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<OrcamentoCategoria>orcamentoCategorias=new ArrayList<>();

}
