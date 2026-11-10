package com.Sobra.Aplicativo.seguranca;

/**
 * Nome do atributo da requisição onde o filtro de autenticação guarda o id do usuário.
 * Nos controllers: {@code @RequestAttribute(UsuarioLogado.ATRIBUTO) Long usuarioId}.
 * Regra 1 da seção 5.12: o id do usuário vem SEMPRE daqui, nunca do corpo ou da rota.
 */
public final class UsuarioLogado {

    public static final String ATRIBUTO = "sobra.usuarioId";
    public static final String ID_REQUISICAO = "sobra.idRequisicao";

    private UsuarioLogado() {
    }
}
