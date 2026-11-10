package com.Sobra.Aplicativo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Configurações "sobra.*" do application.yml. Limites de negócio ficam aqui, não no código (D-02). */
@ConfigurationProperties(prefix = "sobra")
public class SobraProperties {

    private String fusoHorario = "America/Sao_Paulo";
    private String relogioFixo;
    private boolean exigirHttps = false;
    private String urlPublica = "http://localhost:8084";
    private BigDecimal taxaSimulacao = new BigDecimal("0.009600");
    private Limites limites = new Limites();
    private Sessao sessao = new Sessao();
    private Recuperacao recuperacao = new Recuperacao();
    private LimiteTaxa limiteTaxa = new LimiteTaxa();
    private Email email = new Email();

    public static class Limites {
        private int prazoSimulacaoMax = 120;
        private int prazoObjetivoMax = 360;

        public int getPrazoSimulacaoMax() { return prazoSimulacaoMax; }
        public void setPrazoSimulacaoMax(int v) { this.prazoSimulacaoMax = v; }
        public int getPrazoObjetivoMax() { return prazoObjetivoMax; }
        public void setPrazoObjetivoMax(int v) { this.prazoObjetivoMax = v; }
    }

    public static class Sessao {
        private int validadeDias = 30;
        private int tetoDias = 90;

        public int getValidadeDias() { return validadeDias; }
        public void setValidadeDias(int v) { this.validadeDias = v; }
        public int getTetoDias() { return tetoDias; }
        public void setTetoDias(int v) { this.tetoDias = v; }
    }

    public static class Recuperacao {
        private int validadeMinutos = 30;

        public int getValidadeMinutos() { return validadeMinutos; }
        public void setValidadeMinutos(int v) { this.validadeMinutos = v; }
    }

    public static class LimiteTaxa {
        private int loginPorEmail = 5;
        private int loginPorIp = 5;
        private int loginJanelaMinutos = 15;
        private int recuperacaoPorEmail = 3;
        private int recuperacaoJanelaMinutos = 60;
        private int publicoPorIp = 30;
        private int publicoJanelaMinutos = 15;
        private int geralPorToken = 120;
        private int geralJanelaSegundos = 60;

        public int getLoginPorEmail() { return loginPorEmail; }
        public void setLoginPorEmail(int v) { this.loginPorEmail = v; }
        public int getLoginPorIp() { return loginPorIp; }
        public void setLoginPorIp(int v) { this.loginPorIp = v; }
        public int getLoginJanelaMinutos() { return loginJanelaMinutos; }
        public void setLoginJanelaMinutos(int v) { this.loginJanelaMinutos = v; }
        public int getRecuperacaoPorEmail() { return recuperacaoPorEmail; }
        public void setRecuperacaoPorEmail(int v) { this.recuperacaoPorEmail = v; }
        public int getRecuperacaoJanelaMinutos() { return recuperacaoJanelaMinutos; }
        public void setRecuperacaoJanelaMinutos(int v) { this.recuperacaoJanelaMinutos = v; }
        public int getPublicoPorIp() { return publicoPorIp; }
        public void setPublicoPorIp(int v) { this.publicoPorIp = v; }
        public int getPublicoJanelaMinutos() { return publicoJanelaMinutos; }
        public void setPublicoJanelaMinutos(int v) { this.publicoJanelaMinutos = v; }
        public int getGeralPorToken() { return geralPorToken; }
        public void setGeralPorToken(int v) { this.geralPorToken = v; }
        public int getGeralJanelaSegundos() { return geralJanelaSegundos; }
        public void setGeralJanelaSegundos(int v) { this.geralJanelaSegundos = v; }
    }

    public static class Email {
        /** Só para testes: guarda os e-mails "enviados" em memória para o teste ler o token. */
        private boolean guardarEmMemoria = false;

        public boolean isGuardarEmMemoria() { return guardarEmMemoria; }
        public void setGuardarEmMemoria(boolean v) { this.guardarEmMemoria = v; }
    }

    public String getFusoHorario() { return fusoHorario; }
    public void setFusoHorario(String v) { this.fusoHorario = v; }
    public String getRelogioFixo() { return relogioFixo; }
    public void setRelogioFixo(String v) { this.relogioFixo = v; }
    public boolean isExigirHttps() { return exigirHttps; }
    public void setExigirHttps(boolean v) { this.exigirHttps = v; }
    public String getUrlPublica() { return urlPublica; }
    public void setUrlPublica(String v) { this.urlPublica = v; }
    public BigDecimal getTaxaSimulacao() { return taxaSimulacao; }
    public void setTaxaSimulacao(BigDecimal v) { this.taxaSimulacao = v; }
    public Limites getLimites() { return limites; }
    public void setLimites(Limites v) { this.limites = v; }
    public Sessao getSessao() { return sessao; }
    public void setSessao(Sessao v) { this.sessao = v; }
    public Recuperacao getRecuperacao() { return recuperacao; }
    public void setRecuperacao(Recuperacao v) { this.recuperacao = v; }
    public LimiteTaxa getLimiteTaxa() { return limiteTaxa; }
    public void setLimiteTaxa(LimiteTaxa v) { this.limiteTaxa = v; }
    public Email getEmail() { return email; }
    public void setEmail(Email v) { this.email = v; }
}
