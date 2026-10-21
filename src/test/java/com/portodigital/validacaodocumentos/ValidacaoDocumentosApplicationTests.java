package com.portodigital.validacaodocumentos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ValidacaoDocumentosApplicationTests {

    @Test
    void contextLoads() {
        // Verifica se o contexto Spring, os Repositories e os Mappings JPA carregam com sucesso
    }
}
