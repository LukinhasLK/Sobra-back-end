package com.Sobra.Aplicativo.comum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validação de campos com as regras da decisão D-03 (opção B, moderada).
 * Junta todos os erros e lança um único 422 VALIDACAO no fim, com a lista em "campos".
 *
 * Os campos chegam como Object porque o JSON pode trazer qualquer tipo; assim um
 * "valor": true vira 422 FORMATO_INVALIDO no campo certo, e não um 400 genérico.
 *
 * Uso:
 *   Validacao v = new Validacao();
 *   String nome = v.texto("nome", req.nome(), 1, 80);
 *   BigDecimal valor = v.valorPositivo("valor", req.valor());
 *   v.lancarSeHouver();
 */
public class Validacao {

    public static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");
    public static final LocalDate DATA_MINIMA = LocalDate.of(2000, 1, 1);

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern DECIMAL = Pattern.compile("^-?\\d{1,20}(\\.\\d{1,10})?$");
    private static final Pattern INTEIRO = Pattern.compile("^-?\\d{1,9}$");
    private static final DateTimeFormatter ISO_DIA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    /** Senhas muito comuns recusadas (D-03). Lista curta de exemplo; amplie com uma lista pública maior. */
    private static final Set<String> SENHAS_COMUNS = Set.of(
            "12345678", "123456789", "1234567890", "87654321", "11111111", "00000000",
            "password", "password1", "senha123", "senha1234", "senhasenha", "qwerty123",
            "qwertyuiop", "abc12345", "abcd1234", "iloveyou", "11223344", "12341234",
            "admin123", "brasil123", "mudar123", "123mudar", "q1w2e3r4", "1q2w3e4r",
            "asdfghjk", "aaaaaaaa", "12121212", "princesa", "flamengo", "corinthians");

    private final List<CampoErro> erros = new ArrayList<>();

    public void erro(String campo, String codigo, String mensagem) {
        erros.add(new CampoErro(campo, codigo, mensagem));
    }

    public boolean temErro(String campo) {
        return erros.stream().anyMatch(e -> e.campo().equals(campo));
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    public void lancarSeHouver() {
        if (!erros.isEmpty()) {
            throw ErroApi.validacao(erros);
        }
    }

    // ------------------------------------------------------------------ texto

    /** Texto obrigatório, com espaços das pontas removidos e tamanho entre min e max. */
    public String texto(String campo, Object bruto, int min, int max) {
        if (bruto == null) {
            erro(campo, "OBRIGATORIO", "Campo obrigatório.");
            return null;
        }
        if (!(bruto instanceof String s)) {
            erro(campo, "FORMATO_INVALIDO", "Informe um texto.");
            return null;
        }
        String limpo = s.trim();
        if (limpo.isEmpty()) {
            erro(campo, "OBRIGATORIO", "Campo obrigatório.");
            return null;
        }
        int tamanho = limpo.codePointCount(0, limpo.length());
        if (tamanho < min || tamanho > max) {
            erro(campo, "FORA_DO_LIMITE", "Use de " + min + " a " + max + " caracteres.");
            return null;
        }
        return limpo;
    }

    /** E-mail normalizado (sem espaços nas pontas, minúsculo), formato simples, até 254 caracteres. */
    public String email(String campo, Object bruto) {
        if (bruto == null || (bruto instanceof String s0 && s0.isBlank())) {
            erro(campo, "OBRIGATORIO", "Informe o e-mail.");
            return null;
        }
        if (!(bruto instanceof String s)) {
            erro(campo, "FORMATO_INVALIDO", "Informe um e-mail válido.");
            return null;
        }
        String email = normalizarEmail(s);
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            erro(campo, "FORMATO_INVALIDO", "Informe um e-mail válido.");
            return null;
        }
        return email;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Senha de 8 a 64 caracteres, sem exigir símbolos, recusando as muito comuns (D-03). */
    public String senha(String campo, Object bruto) {
        if (bruto == null || (bruto instanceof String s0 && s0.isEmpty())) {
            erro(campo, "OBRIGATORIO", "Informe a senha.");
            return null;
        }
        if (!(bruto instanceof String s)) {
            erro(campo, "FORMATO_INVALIDO", "Informe um texto.");
            return null;
        }
        int tamanho = s.codePointCount(0, s.length());
        if (tamanho < 8 || tamanho > 64) {
            erro(campo, "FORA_DO_LIMITE", "A senha deve ter de 8 a 64 caracteres.");
            return null;
        }
        if (SENHAS_COMUNS.contains(s.toLowerCase(Locale.ROOT))) {
            erro(campo, "FORMATO_INVALIDO", "Essa senha é muito comum. Escolha outra.");
            return null;
        }
        return s;
    }

    /** Só confere presença (usado no login, onde não se revela a regra de senha). */
    public String presente(String campo, Object bruto) {
        if (bruto == null || (bruto instanceof String s0 && s0.isEmpty())) {
            erro(campo, "OBRIGATORIO", "Campo obrigatório.");
            return null;
        }
        if (!(bruto instanceof String s)) {
            erro(campo, "FORMATO_INVALIDO", "Informe um texto.");
            return null;
        }
        return s;
    }

    // ------------------------------------------------------------------ números

    /** Valor em reais maior que zero, até 99.999.999,99, no máximo 2 casas. */
    public BigDecimal valorPositivo(String campo, Object bruto) {
        BigDecimal v = decimal(campo, bruto);
        if (v == null) {
            return null;
        }
        if (v.signum() <= 0) {
            erro(campo, "DEVE_SER_POSITIVO", "Informe um valor maior que zero.");
            return null;
        }
        return dentroDoTeto(campo, v);
    }

    /** Valor em reais maior ou igual a zero (simulação). */
    public BigDecimal valorNaoNegativo(String campo, Object bruto) {
        BigDecimal v = decimal(campo, bruto);
        if (v == null) {
            return null;
        }
        if (v.signum() < 0) {
            erro(campo, "FORA_DO_LIMITE", "O valor não pode ser negativo.");
            return null;
        }
        return dentroDoTeto(campo, v);
    }

    /** Valor opcional: nulo é aceito (ex.: categoria inativa); se vier, deve ser positivo. */
    public BigDecimal valorPositivoOpcional(String campo, Object bruto) {
        return bruto == null ? null : valorPositivo(campo, bruto);
    }

    private BigDecimal dentroDoTeto(String campo, BigDecimal v) {
        if (v.compareTo(VALOR_MAXIMO) > 0) {
            erro(campo, "FORA_DO_LIMITE", "O valor máximo é 99999999.99.");
            return null;
        }
        return v.setScale(2);
    }

    private BigDecimal decimal(String campo, Object bruto) {
        if (bruto == null) {
            erro(campo, "OBRIGATORIO", "Campo obrigatório.");
            return null;
        }
        String texto;
        if (bruto instanceof String s) {
            texto = s.trim();
        } else if (bruto instanceof Number n && !(bruto instanceof Double d && (d.isNaN() || d.isInfinite()))) {
            texto = new BigDecimal(n.toString()).toPlainString();
        } else {
            erro(campo, "FORMATO_INVALIDO", "Use o formato 1234.56.");
            return null;
        }
        if (!DECIMAL.matcher(texto).matches()) {
            erro(campo, "FORMATO_INVALIDO", "Use o formato 1234.56.");
            return null;
        }
        BigDecimal v = new BigDecimal(texto);
        BigDecimal semZerosExtras = v.scale() > 2 ? v.stripTrailingZeros() : v;
        if (semZerosExtras.scale() > 2) {
            erro(campo, "FORMATO_INVALIDO", "Use no máximo duas casas decimais.");
            return null;
        }
        return v;
    }

    /** Inteiro entre min e max (prazos). Aceita número JSON inteiro ou texto só com dígitos. */
    public Integer inteiro(String campo, Object bruto, int min, int max) {
        if (bruto == null) {
            erro(campo, "OBRIGATORIO", "Campo obrigatório.");
            return null;
        }
        long valor;
        if (bruto instanceof Integer || bruto instanceof Long || bruto instanceof Short) {
            valor = ((Number) bruto).longValue();
        } else if (bruto instanceof Number n) {
            BigDecimal bd = new BigDecimal(n.toString());
            if (bd.stripTrailingZeros().scale() > 0) {
                erro(campo, "FORMATO_INVALIDO", "Informe um número inteiro.");
                return null;
            }
            valor = bd.longValue();
        } else if (bruto instanceof String s && INTEIRO.matcher(s.trim()).matches()) {
            valor = Long.parseLong(s.trim());
        } else {
            erro(campo, "FORMATO_INVALIDO", "Informe um número inteiro.");
            return null;
        }
        if (valor < min || valor > max) {
            erro(campo, "FORA_DO_LIMITE", "Use um valor de " + min + " a " + max + ".");
            return null;
        }
        return (int) valor;
    }

    // ------------------------------------------------------------------ outros

    /** Data "AAAA-MM-DD" entre 2000-01-01 e hoje (D-03: datas futuras bloqueadas no servidor). */
    public LocalDate data(String campo, Object bruto, LocalDate hoje) {
        if (bruto == null) {
            erro(campo, "OBRIGATORIO", "Campo obrigatório.");
            return null;
        }
        if (!(bruto instanceof String s)) {
            erro(campo, "DATA_INVALIDA", "Use o formato AAAA-MM-DD.");
            return null;
        }
        LocalDate data;
        try {
            data = LocalDate.parse(s.trim(), ISO_DIA);
        } catch (DateTimeParseException e) {
            erro(campo, "DATA_INVALIDA", "Use o formato AAAA-MM-DD.");
            return null;
        }
        if (data.isBefore(DATA_MINIMA) || data.isAfter(hoje)) {
            erro(campo, "DATA_INVALIDA", "A data deve estar entre 2000-01-01 e hoje.");
            return null;
        }
        return data;
    }

    public Boolean booleano(String campo, Object bruto) {
        if (bruto == null) {
            erro(campo, "OBRIGATORIO", "Campo obrigatório.");
            return null;
        }
        if (!(bruto instanceof Boolean b)) {
            erro(campo, "FORMATO_INVALIDO", "Use true ou false.");
            return null;
        }
        return b;
    }
}
