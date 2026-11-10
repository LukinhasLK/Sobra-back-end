package com.Sobra.Aplicativo.controller;

import com.Sobra.Aplicativo.comum.CampoErro;
import com.Sobra.Aplicativo.comum.ErroApi;
import com.Sobra.Aplicativo.comum.JsonErro;
import com.Sobra.Aplicativo.seguranca.RespostaErro;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte toda exceção em resposta no formato único (seção 6.2), sem stack trace
 * nem detalhe interno (RNF-07). O app decide pelo "codigo", nunca pela mensagem.
 */
@RestControllerAdvice
public class TratadorErros {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorErros.class);

    /**
     * Nomes explícitos das restrições (seção 5.13) → erro com o campo certo.
     * Normalmente a validação do servidor pega antes; isto cobre corridas e esquecimentos.
     */
    private static final Map<String, ErroApi> RESTRICOES = new LinkedHashMap<>();

    static {
        RESTRICOES.put("uq_usuario_email", ErroApi.emailJaCadastrado());
        RESTRICOES.put("ck_usuario_nome_preenchido", campo("nome_completo", "OBRIGATORIO"));
        RESTRICOES.put("ck_despesa_valor", campo("valor", "DEVE_SER_POSITIVO"));
        RESTRICOES.put("ck_despesa_descricao", campo("descricao", "OBRIGATORIO"));
        RESTRICOES.put("ck_despesa_data", campo("data", "DATA_INVALIDA"));
        RESTRICOES.put("fk_despesa_categoria", campo("categoria_codigo", "CATEGORIA_DESCONHECIDA"));
        RESTRICOES.put("ck_objetivo_nome", campo("nome", "OBRIGATORIO"));
        RESTRICOES.put("ck_objetivo_valor_meta", campo("valor_meta", "DEVE_SER_POSITIVO"));
        RESTRICOES.put("ck_objetivo_prazo", campo("prazo_meses", "FORA_DO_LIMITE"));
        RESTRICOES.put("ck_aporte_tipo", campo("tipo", "FORMATO_INVALIDO"));
        RESTRICOES.put("ck_aporte_valor", campo("valor", "DEVE_SER_POSITIVO"));
        RESTRICOES.put("ck_aporte_data", campo("data", "DATA_INVALIDA"));
        RESTRICOES.put("ck_renda_vigencia_valor", campo("valor", "DEVE_SER_POSITIVO"));
        RESTRICOES.put("ck_orcamento_categoria_valor", campo("valor_estimado", "DEVE_SER_POSITIVO"));
        RESTRICOES.put("ck_simulacao_valores", campo("aporte_mensal", "DEVE_SER_POSITIVO"));
        RESTRICOES.put("ck_simulacao_prazo", campo("prazo_meses", "FORA_DO_LIMITE"));
    }

    private static ErroApi campo(String campo, String codigo) {
        return ErroApi.validacao(List.of(new CampoErro(campo, codigo, "Valor inválido.")));
    }

    @ExceptionHandler(ErroApi.class)
    public ResponseEntity<byte[]> erroApi(ErroApi erro, HttpServletRequest req) {
        return resposta(erro, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<byte[]> jsonInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(ErroApi.requisicaoInvalida("Corpo da requisição inválido ou maior que 10 KB."), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<byte[]> tipoNaoSuportado(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return resposta(ErroApi.requisicaoInvalida("Envie o corpo como application/json."), req);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<byte[]> parametroInvalido(Exception e, HttpServletRequest req) {
        return resposta(ErroApi.requisicaoInvalida("Parâmetro inválido."), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<byte[]> metodoNaoSuportado(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return resposta(new ErroApi(405, "REQUISICAO_INVALIDA", "Método não permitido nesta rota."), req);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<byte[]> rotaInexistente(Exception e, HttpServletRequest req) {
        return resposta(ErroApi.naoEncontrado(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<byte[]> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        // A mensagem do banco pode conter dados da linha: só procuramos o NOME da restrição e nunca a registramos.
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(e);
        String mensagem = causa.getMessage() == null ? "" : causa.getMessage();
        for (Map.Entry<String, ErroApi> r : RESTRICOES.entrySet()) {
            if (mensagem.contains(r.getKey())) {
                return resposta(r.getValue(), req);
            }
        }
        LOG.error("{\"id_requisicao\":\"{}\",\"excecao\":\"{}\"}", RespostaErro.idRequisicao(req), causa.getClass().getName());
        return resposta(ErroApi.erroInterno(), req);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<byte[]> bancoFora(Exception e, HttpServletRequest req) {
        LOG.error("{\"id_requisicao\":\"{}\",\"excecao\":\"{}\"}", RespostaErro.idRequisicao(req), e.getClass().getName());
        return resposta(ErroApi.servicoIndisponivel(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> inesperado(Exception e, HttpServletRequest req) {
        // Registra a classe e onde aconteceu, mas não a mensagem (pode conter dados pessoais).
        StackTraceElement[] pilha = e.getStackTrace();
        String onde = pilha.length > 0 ? pilha[0].toString() : "?";
        LOG.error("{\"id_requisicao\":\"{}\",\"excecao\":\"{}\",\"onde\":\"{}\"}",
                RespostaErro.idRequisicao(req), e.getClass().getName(), onde);
        return resposta(ErroApi.erroInterno(), req);
    }

    private ResponseEntity<byte[]> resposta(ErroApi erro, HttpServletRequest req) {
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(erro.getStatus())
                .contentType(MediaType.APPLICATION_JSON);
        String autenticacao = RespostaErro.cabecalhoAutenticacao(erro);
        if (autenticacao != null) {
            resposta.header("WWW-Authenticate", autenticacao);
        }
        if (erro.getRetryAfterSegundos() != null) {
            resposta.header("Retry-After", String.valueOf(erro.getRetryAfterSegundos()));
        }
        return resposta.body(JsonErro.corpo(erro, RespostaErro.idRequisicao(req)));
    }
}
