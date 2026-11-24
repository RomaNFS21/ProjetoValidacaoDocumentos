package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.exception.OcrProcessingException;
import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Serviço de Extração de Texto via Tesseract OCR utilizando a biblioteca Tess4J.
 *
 * Configurações aplicadas:
 * - Idioma: Português ("por" ou "por+eng") com dados treinados do modelo LSTM.
 * - OCR Engine Mode (OEM): OEM_LSTM_ONLY (modo baseado em redes neurais recorrentes LSTM,
 *   mais preciso e moderno do que o motor legado).
 * - Page Segmentation Mode (PSM): PSM_AUTO (3) ou PSM_SINGLE_BLOCK (6) para reconhecimento
 *   estruturado de documentos, faturas e comprovantes de residência.
 * - Validação proativa de diretório tessdata e arquivos .traineddata.
 */
@Service
public class OcrExtractionService implements TesseractOcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrExtractionService.class);

    private final String configuredDatapath;
    private final String language;
    private final int oem;
    private final int psm;

    private ITesseract tesseract;
    private String resolvedDatapath;

    public OcrExtractionService(
            @Value("${tesseract.datapath:${TESSDATA_PREFIX:tessdata}}") String datapath,
            @Value("${tesseract.language:${TESSERACT_LANGUAGE:por}}") String language,
            @Value("${ocr.tesseract.oem:1}") int oem,
            @Value("${ocr.tesseract.psm:3}") int psm) {
        this.configuredDatapath = datapath;
        this.language = language;
        this.oem = oem;
        this.psm = psm;
    }

    @PostConstruct
    public void init() {
        log.info("Inicializando motor Tesseract OCR (Tess4J)...");
        this.resolvedDatapath = resolveDatapath(this.configuredDatapath);

        validateTessdata(this.resolvedDatapath, this.language);

        this.tesseract = createTesseractInstance();
        log.info("Tesseract OCR inicializado com sucesso. Idioma: '{}', Datapath: '{}', OEM: {}, PSM: {}",
                this.language, this.resolvedDatapath, this.oem, this.psm);
    }

    @Override
    public String extrairTexto(BufferedImage imagem) {
        return extractText(imagem);
    }

    @Override
    public String extrairTexto(BufferedImage imagemProcessada, BufferedImage imagemOriginal) {
        // Passe 1: OCR na imagem pré-processada (OpenCV)
        String textoProcessado = extractText(imagemProcessada);
        int lenProcessado = textoProcessado != null ? textoProcessado.trim().length() : 0;

        // Passe 2: OCR na imagem original (sem filtros) se o resultado for insuficiente
        String textoOriginal = null;
        if (imagemOriginal != null && lenProcessado < 180) {
            try {
                textoOriginal = extractText(imagemOriginal);
                int lenOriginal = textoOriginal != null ? textoOriginal.trim().length() : 0;
                if (lenOriginal > lenProcessado) {
                    log.info("Passe OCR na imagem original capturou mais texto ({} vs {}). Priorizando imagem original.",
                            lenOriginal, lenProcessado);
                    textoProcessado = textoOriginal;
                    lenProcessado = lenOriginal;
                }
            } catch (Exception e) {
                log.warn("Passe complementar de OCR na imagem original falhou: {}", e.getMessage());
            }
        }

        // Passe 3: OCR com PSM=6 (single_block) na imagem original — melhor para CNH/RG que têm layout estruturado
        if (imagemOriginal != null && lenProcessado < 250) {
            try {
                String textoPsm6 = extractTextWithPsm(imagemOriginal, ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK);
                int lenPsm6 = textoPsm6 != null ? textoPsm6.trim().length() : 0;
                if (lenPsm6 > lenProcessado) {
                    log.info("Passe OCR PSM=6 (single_block) capturou mais texto ({} vs {}). Usando resultado PSM=6.",
                            lenPsm6, lenProcessado);
                    return textoPsm6;
                }
            } catch (Exception e) {
                log.warn("Passe OCR PSM=6 falhou: {}", e.getMessage());
            }
        }

        return textoProcessado;
    }

    /**
     * Executa o OCR com um Page Segmentation Mode (PSM) alternativo,
     * sem alterar a configuração padrão da instância principal.
     */
    private String extractTextWithPsm(BufferedImage image, int psmMode) {
        ITesseract instance = createTesseractInstance();
        instance.setPageSegMode(psmMode);
        try {
            String text = instance.doOCR(image);
            return text != null ? text.trim() : "";
        } catch (TesseractException e) {
            log.debug("OCR PSM={} falhou: {}", psmMode, e.getMessage());
            return "";
        }
    }

    /**
     * Extrai o texto bruto contido em uma imagem pré-processada.
     *
     * @param processedImage Imagem tratada e otimizada (OpenCV)
     * @return String de texto bruto extraído pelo OCR
     * @throws OcrProcessingException Em caso de erro do motor ou falha no reconhecimento
     */
    public String extractText(BufferedImage processedImage) {
        if (processedImage == null) {
            throw new IllegalArgumentException("A imagem fornecida para extração OCR não pode ser nula");
        }

        if (processedImage.getWidth() <= 0 || processedImage.getHeight() <= 0) {
            throw new OcrProcessingException("A imagem possui dimensões inválidas (largura ou altura menor ou igual a zero).");
        }

        long startTime = System.currentTimeMillis();
        try {
            log.debug("Executando extração OCR em imagem {}x{}...", processedImage.getWidth(), processedImage.getHeight());
            String rawText = this.tesseract.doOCR(processedImage);

            long elapsed = System.currentTimeMillis() - startTime;
            if (rawText == null || rawText.trim().isEmpty()) {
                log.warn("OCR executado em {}ms mas nenhum caractere legível foi reconhecido na imagem.", elapsed);
                return "";
            }

            String sanitized = rawText.trim();
            log.info("OCR concluído com sucesso em {}ms. Caracteres extraídos: {}", elapsed, sanitized.length());
            return sanitized;

        } catch (TesseractException te) {
            String errorMsg = String.format("Falha ao executar o OCR com Tesseract: %s", te.getMessage());
            log.error(errorMsg, te);
            throw new OcrProcessingException(errorMsg, te);
        } catch (Throwable t) {
            String errorMsg = String.format("Erro inesperado durante a execução do Tesseract OCR: %s", t.getMessage());
            log.error(errorMsg, t);
            throw new OcrProcessingException(errorMsg, t);
        }
    }

    /**
     * Extrai o texto a partir de um arquivo de imagem físico em disco.
     *
     * @param imageFile Arquivo de imagem
     * @return String de texto bruto extraído
     */
    public String extractText(File imageFile) {
        if (imageFile == null || !imageFile.exists() || !imageFile.canRead()) {
            throw new IllegalArgumentException("Arquivo de imagem inválido ou inacessível: " + imageFile);
        }

        try {
            log.debug("Executando extração OCR no arquivo '{}'...", imageFile.getName());
            String rawText = this.tesseract.doOCR(imageFile);
            return rawText != null ? rawText.trim() : "";
        } catch (TesseractException te) {
            throw new OcrProcessingException("Falha ao executar OCR no arquivo " + imageFile.getName() + ": " + te.getMessage(), te);
        }
    }

    /**
     * Cria e parametriza a instância do {@link ITesseract}.
     */
    protected ITesseract createTesseractInstance() {
        ITesseract instance = new Tesseract();
        instance.setDatapath(this.resolvedDatapath);
        instance.setLanguage(this.language);
        instance.setOcrEngineMode(this.oem);
        instance.setPageSegMode(this.psm);
        return instance;
    }

    /**
     * Valida se a pasta tessdata existe e se o modelo de linguagem configurado está disponível.
     */
    private void validateTessdata(String datapath, String language) {
        File dir = new File(datapath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new OcrProcessingException(String.format(
                    "Diretório tessdata não encontrado ou inválido no caminho: '%s'. " +
                    "Certifique-se de configurar a propriedade 'tesseract.datapath' ou a variável de ambiente 'TESSDATA_PREFIX'.",
                    dir.getAbsolutePath()
            ));
        }

        // Verifica os modelos solicitados (ex: "por" ou "por+eng")
        String[] langs = language.split("\\+");
        for (String lang : langs) {
            String trainedDataFile = lang.trim() + ".traineddata";
            File modelFile = new File(dir, trainedDataFile);
            if (!modelFile.exists() || modelFile.length() == 0) {
                throw new OcrProcessingException(String.format(
                        "Arquivo de modelo treinado '%s' não encontrado no diretório tessdata: '%s'.",
                        trainedDataFile, dir.getAbsolutePath()
                ));
            } else {
                log.debug("Modelo de idioma verificado: {} ({} bytes)", modelFile.getName(), modelFile.length());
            }
        }
    }

    /**
     * Resolve o caminho para o diretório tessdata, garantindo compatibilidade ASCII com a biblioteca nativa do Tesseract no Windows.
     */
    private String resolveDatapath(String configured) {
        if (configured != null && !configured.isBlank() && !"tessdata".equalsIgnoreCase(configured.trim())) {
            File custom = new File(configured);
            if (!custom.exists() || !custom.isDirectory()) {
                throw new OcrProcessingException(String.format(
                        "Diretório tessdata não encontrado ou inválido no caminho: '%s'",
                        custom.getAbsolutePath()
                ));
            }
            return ensureAsciiSafeDatapath(custom.toPath());
        }

        // Verifica na raiz do projeto (mantém caminho relativo para evitar caracteres não-ASCII do diretório pai)
        Path localProject = Paths.get("tessdata");
        if (localProject.toFile().exists() && localProject.toFile().isDirectory()) {
            return ensureAsciiSafeDatapath(localProject);
        }

        // Verifica variável de ambiente TESSDATA_PREFIX
        String envPrefix = System.getenv("TESSDATA_PREFIX");
        if (envPrefix != null && !envPrefix.isBlank()) {
            File envFile = new File(envPrefix);
            if (envFile.exists() && envFile.isDirectory()) {
                return ensureAsciiSafeDatapath(envFile.toPath());
            }
        }

        // Caminho padrão do instalador no Windows
        File windowsDefault = new File("C:\\Program Files\\Tesseract-OCR\\tessdata");
        if (windowsDefault.exists() && windowsDefault.isDirectory()) {
            return ensureAsciiSafeDatapath(windowsDefault.toPath());
        }

        throw new OcrProcessingException("Nenhum diretório tessdata válido foi encontrado na aplicação ou no sistema.");
    }

    /**
     * A biblioteca nativa C++ do Tesseract (via JNA) no Windows falha ao abrir caminhos com acentos/não-ASCII.
     * Se o caminho contiver caracteres não-ASCII, tenta usar caminho relativo ao diretório de trabalho ou
     * copia os modelos .traineddata para um diretório temporário ASCII.
     */
    private String ensureAsciiSafeDatapath(Path dir) {
        String raw = dir.toString();
        if (!dir.isAbsolute() && isAscii(raw)) {
            return raw;
        }

        String abs = dir.toAbsolutePath().normalize().toString();
        if (isAscii(abs)) {
            return abs;
        }

        // Tenta caminho relativo ao CWD (ex: "tessdata")
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path absPath = dir.toAbsolutePath().normalize();
        if (absPath.startsWith(cwd)) {
            String rel = cwd.relativize(absPath).toString();
            if (!rel.isEmpty() && isAscii(rel)) {
                return rel;
            }
        }

        // Fallback: copia arquivos .traineddata para um diretório temporário puramente ASCII
        try {
            Path tempBase = resolveAsciiTempDir();
            Path targetDir = tempBase.resolve("projeto-validacao-tessdata");
            Files.createDirectories(targetDir);
            try (Stream<Path> files = Files.list(absPath)) {
                files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".traineddata"))
                        .forEach(src -> {
                            try {
                                Path dst = targetDir.resolve(src.getFileName());
                                if (!Files.exists(dst) || Files.size(dst) != Files.size(src)) {
                                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException e) {
                                log.warn("Não foi possível copiar {} para diretório ASCII temporário: {}", src, e.getMessage());
                            }
                        });
            }
            return targetDir.toAbsolutePath().toString();
        } catch (IOException e) {
            log.warn("Falha ao criar diretório tessdata ASCII temporário, usando caminho original: {}", e.getMessage());
            return abs;
        }
    }

    private Path resolveAsciiTempDir() {
        String sysTemp = System.getProperty("java.io.tmpdir");
        if (sysTemp != null && isAscii(sysTemp)) {
            return Paths.get(sysTemp);
        }
        Path publicTemp = Paths.get("C:\\Users\\Public");
        if (Files.isDirectory(publicTemp) && Files.isWritable(publicTemp)) {
            return publicTemp;
        }
        return Paths.get(sysTemp != null ? sysTemp : ".");
    }

    private boolean isAscii(String value) {
        return value != null && StandardCharsets.US_ASCII.newEncoder().canEncode(value);
    }

    public String getResolvedDatapath() {
        return resolvedDatapath;
    }

    public String getLanguage() {
        return language;
    }

    public ITesseract getTesseract() {
        return tesseract;
    }
}
