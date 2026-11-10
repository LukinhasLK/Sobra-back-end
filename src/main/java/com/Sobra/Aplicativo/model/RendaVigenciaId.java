package com.Sobra.Aplicativo.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** Chave composta de renda_vigencia: (usuario_id, mes_inicio). */
public class RendaVigenciaId implements Serializable {

    private Long usuarioId;
    private LocalDate mesInicio;

    public RendaVigenciaId() {
    }

    public RendaVigenciaId(Long usuarioId, LocalDate mesInicio) {
        this.usuarioId = usuarioId;
        this.mesInicio = mesInicio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RendaVigenciaId outro)) return false;
        return Objects.equals(usuarioId, outro.usuarioId) && Objects.equals(mesInicio, outro.mesInicio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, mesInicio);
    }
}
