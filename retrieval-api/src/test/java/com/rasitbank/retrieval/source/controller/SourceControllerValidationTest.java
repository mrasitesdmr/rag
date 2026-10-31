package com.rasitbank.retrieval.source.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.rasitbank.retrieval.exception.GlobalExceptionHandler;
import com.rasitbank.retrieval.exception.UnprocessableContentException;
import com.rasitbank.retrieval.ingestion.parsing.PdfDocumentParser;
import com.rasitbank.retrieval.ingestion.service.IngestionService;
import com.rasitbank.retrieval.source.service.SourceService;
import com.rasitbank.retrieval.source.service.SourceUploadValidator;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Gerçek yükleme doğrulayıcısıyla belge ekleme ucunun 400, 415 ve 422 gövdelerini ve servise hiç gitmediğini doğrular.
 */
@WebMvcTest(controllers = SourceController.class)
@Import({GlobalExceptionHandler.class, SourceUploadValidator.class})
class SourceControllerValidationTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SourceService sourceService;

    @MockitoBean
    private IngestionService ingestionService;

    @MockitoBean
    private PdfDocumentParser parser;

    private static MockMultipartFile text(String name, String value) {
        return new MockMultipartFile(name, "", "text/plain", value.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile file(String fileName, String content) {
        return new MockMultipartFile("file", fileName, "application/pdf", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void givenEmptyFileAnd81CharNameAndNoVersion_whenCreateSource_thenReturns400WithThreeViolations() {
        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources").multipart()
                .file(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]))
                .file(text("name", "n".repeat(81))).exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("file");
        assertThat(result).bodyJson().extractingPath("$.violations[1].field").isEqualTo("name");
        assertThat(result).bodyJson().extractingPath("$.violations[2].field").isEqualTo("version");
        assertThat(result).bodyJson().extractingPath("$.violations[2].message").isEqualTo("must not be blank");
        assertThat(result).bodyJson().extractingPath("$.violations").asList().hasSize(3);
        then(ingestionService).shouldHaveNoInteractions();
    }

    @Test
    void givenTxtFile_whenCreateSource_thenReturns415UnsupportedFileType() {
        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources").multipart()
                .file(file("notlar.txt", "%PDF-1.7 merhaba")).file(text("name", "Notlar"))
                .file(text("version", "1")).exchange();

        // Assert
        assertThat(result).hasStatus(415).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_FILE_TYPE");
        then(ingestionService).shouldHaveNoInteractions();
    }

    @Test
    void givenPdfWithoutBookmarks_whenCreateSource_thenReturns422PdfNoBookmarksAndServiceNotCalled() {
        // Arrange
        given(parser.parse(any())).willThrow(new UnprocessableContentException("PDF_NO_BOOKMARKS", "no bookmarks"));

        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources").multipart()
                .file(file("yer-imsiz.pdf", "%PDF-1.7 govde")).file(text("name", "Yer imsiz"))
                .file(text("version", "1")).exchange();

        // Assert
        assertThat(result).hasStatus(422).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("PDF_NO_BOOKMARKS");
        then(ingestionService).shouldHaveNoInteractions();
    }
}
