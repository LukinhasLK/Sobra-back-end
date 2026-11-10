package com.Sobra.Aplicativo.seguranca;

import com.Sobra.Aplicativo.comum.ErroApi;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RNF-06: limite de taxa em janela deslizante, em memória.
 * Serve para uma instância só. Com mais de uma instância do back end, troque por um
 * armazenamento compartilhado (ex.: Redis) mantendo esta mesma interface.
 * Usa o relógio real (não o relógio fixo dos testes), porque é proteção operacional.
 */
@Component
public class LimiteTaxa {

    private final Map<String, Deque<Long>> registros = new ConcurrentHashMap<>();

    /** Registra uma tentativa; se passou do máximo na janela, lança 429 com Retry-After. */
    public void consumir(String chave, int maximo, Duration janela) {
        long agora = System.currentTimeMillis();
        long inicioJanela = agora - janela.toMillis();
        Deque<Long> fila = registros.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst() <= inicioJanela) {
                fila.pollFirst();
            }
            if (fila.size() >= maximo) {
                long esperaMs = fila.peekFirst() + janela.toMillis() - agora;
                throw ErroApi.limiteRequisicoes((esperaMs + 999) / 1000);
            }
            fila.addLast(agora);
        }
        if (registros.size() > 50_000) {
            limpar(inicioJanela);
        }
    }

    private void limpar(long antesDe) {
        registros.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long ultimo = e.getValue().peekLast();
                return ultimo == null || ultimo <= antesDe;
            }
        });
    }

    /** Só para testes. */
    public void zerar() {
        registros.clear();
    }
}
