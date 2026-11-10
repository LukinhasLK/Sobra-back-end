package com.Sobra.Aplicativo.comum;

/** Um item de "campos" no formato único de erro (seção 6.2). */
public record CampoErro(String campo, String codigo, String mensagem) {
}
