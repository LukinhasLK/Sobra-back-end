package com.Sobra.Aplicativo.comum;

import java.util.List;

/**
 * Erro de negócio que vira resposta no formato único (seção 6.2/6.3).
 * Lance de qualquer camada; o TratadorErros (ou o filtro) converte em JSON.
 */
public class ErroApi extends RuntimeException {

    private final int status;
    private final String codigo;
    private final List<CampoErro> campos;
    private final Long retryAfterSegundos;

    public ErroApi(int status, String codigo, String mensagem, List<CampoErro> campos, Long retryAfterSegundos) {
        super(mensagem, null, false, false); // sem stack trace: é um fluxo esperado
        this.status = status;
        this.codigo = codigo;
        this.campos = campos == null ? List.of() : List.copyOf(campos);
        this.retryAfterSegundos = retryAfterSegundos;
    }

    public ErroApi(int status, String codigo, String mensagem) {
        this(status, codigo, mensagem, List.of(), null);
    }

    public int getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public List<CampoErro> getCampos() {
        return campos;
    }

    public Long getRetryAfterSegundos() {
        return retryAfterSegundos;
    }

    // ----- Fábricas, uma por código da seção 6.3 -----

    public static ErroApi requisicaoInvalida(String mensagem) {
        return new ErroApi(400, "REQUISICAO_INVALIDA", mensagem);
    }

    public static ErroApi tokenRecuperacaoInvalido() {
        return new ErroApi(400, "TOKEN_RECUPERACAO_INVALIDO",
                "O link de recuperação é inválido ou expirou. Peça um novo.");
    }

    public static ErroApi naoAutenticado() {
        return new ErroApi(401, "NAO_AUTENTICADO", "Faça login para continuar.");
    }

    public static ErroApi sessaoExpirada() {
        return new ErroApi(401, "SESSAO_EXPIRADA", "Sua sessão expirou. Entre novamente.");
    }

    public static ErroApi credenciaisInvalidas() {
        return new ErroApi(401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos.");
    }

    public static ErroApi naoEncontrado() {
        return new ErroApi(404, "NAO_ENCONTRADO", "Recurso não encontrado.");
    }

    public static ErroApi emailJaCadastrado() {
        return new ErroApi(409, "EMAIL_JA_CADASTRADO", "Este e-mail já está cadastrado.",
                List.of(new CampoErro("email", "EMAIL_JA_CADASTRADO", "Este e-mail já está cadastrado.")), null);
    }

    public static ErroApi validacao(List<CampoErro> campos) {
        return new ErroApi(422, "VALIDACAO", "Há campos inválidos.", campos, null);
    }

    public static ErroApi validacao(String campo, String codigo, String mensagem) {
        return validacao(List.of(new CampoErro(campo, codigo, mensagem)));
    }

    public static ErroApi limiteRequisicoes(long segundos) {
        return new ErroApi(429, "LIMITE_REQUISICOES", "Muitas tentativas. Aguarde e tente novamente.",
                List.of(), Math.max(1, segundos));
    }

    public static ErroApi erroInterno() {
        return new ErroApi(500, "ERRO_INTERNO", "Ocorreu um erro inesperado. Tente novamente.");
    }

    public static ErroApi servicoIndisponivel() {
        return new ErroApi(503, "SERVICO_INDISPONIVEL", "Serviço indisponível no momento. Tente novamente.");
    }
}
