package com.Sobra.Aplicativo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_categorias")
@Data
public class Categorias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false,unique = true)
    private Integer codigo;

    @Column(nullable = false)
    private String nome ;

    @OneToMany(mappedBy = "categoria")
    private List<Despesas>despesas = new ArrayList<>();

    @OneToMany(mappedBy = "categoria")
    private List<OrcamentoCategoria> orcamentoCategoria = new ArrayList<>();
}
