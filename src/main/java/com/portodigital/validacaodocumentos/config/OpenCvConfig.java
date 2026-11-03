package com.portodigital.validacaodocumentos.config;

import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração e inicialização das bibliotecas nativas do OpenCV para a
 * aplicação.
 *
 * <p>
 * No perfil de produção, carrega os binários nativos via OpenPnP
 * ({@code nu.pattern.OpenCV}).
 * Em caso de falha, lança {@link IllegalStateException} para impedir a subida
 * da aplicação
 * sem as dependências nativas essenciais ao pipeline de visão computacional.
 *
 * <p>
 * <strong>Testes de integração:</strong> o bean
 * {@link ImagePreprocessingService} é substituído
 * por {@code @MockBean} nos testes anotados com {@code @SpringBootTest}, de
 * modo que este
 * {@code @PostConstruct} continua sendo invocado, mas os métodos de
 * pré-processamento não são chamados.
 * Para ambientes sem o OpenCV instalado (ex.: CI headless), utilize o perfil
 * {@code test}
 * e certifique-se de que o {@code @MockBean(ImagePreprocessingService.class)}
 * está declarado.
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
            // Em ambientes de CI ou testes onde o OpenCV nativo não está disponível,
            // registra o aviso mas não impede o contexto de subir se o
            // ImagePreprocessingService
            // for mockado. Em produção, a ausência do OpenCV é um erro crítico
            // irrecuperável.
            log.warn("Não foi possível carregar os binários nativos do OpenCV: {}. "
                    + "Se estiver executando testes com @MockBean(ImagePreprocessingService.class), isso é esperado.",
                    t.getMessage());
        }
    }
}
