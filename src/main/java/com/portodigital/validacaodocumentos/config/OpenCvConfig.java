package com.portodigital.validacaodocumentos.config;

import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração e inicialização das bibliotecas nativas do OpenCV para a aplicação.
 */
@Configuration
public class OpenCvConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenCvConfig.class);

    @PostConstruct
    public void initOpenCv() {
        try {
            log.info("Inicializando bibliotecas nativas do OpenCV via OpenPnP...");
            OpenCV.loadLocally();
            log.info("OpenCV carregado com sucesso. Versão: {}", Core.VERSION);
        } catch (Throwable t) {
            log.error("Falha crítica ao inicializar bibliotecas nativas do OpenCV: {}", t.getMessage(), t);
            throw new IllegalStateException("Não foi possível carregar os binários nativos do OpenCV", t);
        }
    }
}
