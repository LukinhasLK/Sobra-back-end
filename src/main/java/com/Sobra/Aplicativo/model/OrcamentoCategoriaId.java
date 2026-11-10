package com.Sobra.Aplicativo.model;

import java.io.Serializable;
import java.util.Objects;

/** Chave composta de orcamento_categoria: (usuario_id, categoria_id). Ver seção 5.3. */
public class OrcamentoCategoriaId implements Serializable {

    private Long usuarioId;
    private Short categoriaId;

    public OrcamentoCategoriaId() {
    }

    public OrcamentoCategoriaId(Long usuarioId, Short categoriaId) {
        this.usuarioId = usuarioId;
        this.categoriaId = categoriaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrcamentoCategoriaId outro)) return false;
        return Objects.equals(usuarioId, outro.usuarioId) && Objects.equals(categoriaId, outro.categoriaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, categoriaId);
    }
}
