package com.Sobra.Aplicativo.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_rendaVigencia")
@Data
public class RendaVigencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate mesInicio;

    @Column(nullable = false)
    private BigDecimal valor;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime criacao;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
}
