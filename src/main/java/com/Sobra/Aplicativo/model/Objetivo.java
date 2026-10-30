package com.Sobra.Aplicativo.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_objetivo")
@Data
public class Objetivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private BigDecimal valor_meta;

    @Column(nullable = false)
    private Integer prazoMeses;

    @Column(nullable = false)
    private BigDecimal aporteMensal;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime criacao;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @OneToMany(mappedBy = "objetivo")
    private List<Aporte> aportes = new ArrayList<>();
}
