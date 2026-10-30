package com.Sobra.Aplicativo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_orcamento_categoria")
@Data
public class OrcamentoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "categoria_id",nullable = false)
    private Categorias categoria;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

}
