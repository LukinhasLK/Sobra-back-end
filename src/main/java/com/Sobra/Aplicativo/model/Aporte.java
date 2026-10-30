package com.Sobra.Aplicativo.model;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "tb_aporte")
@Data
public class Aporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime criacao;

    @ManyToOne
    @JoinColumn(name = "objetivo_id",nullable = false)
    private Objetivo objetivo;

}
