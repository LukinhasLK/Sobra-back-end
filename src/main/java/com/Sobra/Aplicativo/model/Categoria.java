package com.Sobra.Aplicativo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabela de referência com 7 linhas fixas (V002). Id fixo, sem identity. A API usa o código. */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @Column(name = "id")
    private Short id;

    @Column(name = "codigo", nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 40)
    private String nome;

    protected Categoria() {
    }

    public Short getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
}
