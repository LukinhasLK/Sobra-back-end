package com.Sobra.Aplicativo.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_sessao")
@Data
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String token_hash;

    @Column(nullable = false)
    private Instant expira_em;

    @Column
    private Instant revogacao;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime criacao;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
}
