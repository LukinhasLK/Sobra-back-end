package com.Sobra.Aplicativo.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_simulacao")
@Data
public class Simulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal valorInicial;

    @Column(nullable = false)
    private BigDecimal aporteMensal;

    @Column(nullable = false)
    private BigDecimal taxaMensal;

    @Column(nullable = false)
    private BigDecimal resultado;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime criacao;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
}
