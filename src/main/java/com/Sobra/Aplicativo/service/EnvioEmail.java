package com.Sobra.Aplicativo.service;

import com.Sobra.Aplicativo.config.SobraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Envio de e-mail. Esta implementação NÃO envia: registra no log que um e-mail seria enviado
 * (sem endereço nem token, RNF-08). Para produção, troque o corpo de enviar() por um envio
 * real (ex.: spring-boot-starter-mail + JavaMailSender), de preferência assíncrono.
 *
 * Com sobra.email.guardar-em-memoria=true (só no perfil de teste) os e-mails ficam numa lista,
 * para os testes lerem o link de recuperação.
 */
@Component
public class EnvioEmail {

    private static final Logger LOG = LoggerFactory.getLogger(EnvioEmail.class);

    public record Email(String para, String assunto, String corpo) {
    }

    private final SobraProperties props;
    private final List<Email> enviados = Collections.synchronizedList(new ArrayList<>());

    public EnvioEmail(SobraProperties props) {
        this.props = props;
    }

    public void enviar(String para, String assunto, String corpo) {
        if (props.getEmail().isGuardarEmMemoria()) {
            enviados.add(new Email(para, assunto, corpo));
        }
        LOG.info("{\"evento\":\"email_enfileirado\",\"assunto\":\"{}\"}", assunto);
    }

    /** Só para testes. */
    public List<Email> enviados() {
        synchronized (enviados) {
            return List.copyOf(enviados);
        }
    }
}
