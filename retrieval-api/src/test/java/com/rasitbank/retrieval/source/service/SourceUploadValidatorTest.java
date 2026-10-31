package com.rasitbank.retrieval.source.service;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static com.rasitbank.retrieval.fixture.PdfFixtures.PageSpec;
import static com.rasitbank.retrieval.fixture.PdfFixtures.pdf;
import static com.rasitbank.retrieval.fixture.PdfFixtures.validPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.rasitbank.retrieval.exception.UnprocessableContentException;
import com.rasitbank.retrieval.exception.UnsupportedMediaTypeException;
import com.rasitbank.retrieval.exception.ValidationFailedException;
import com.rasitbank.retrieval.exception.Violation;
import com.rasitbank.retrieval.ingestion.parsing.PageMarginFilter;
import com.rasitbank.retrieval.ingestion.parsing.PdfDocumentParser;
import com.rasitbank.retrieval.source.domain.dto.SourceUpload;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Yükleme parçalarının (dosya, ad, sürüm, tür, PDF içeriği) kurallarını ve ihlal sırasını gerçek okuyucuyla doğrular.
 */
class SourceUploadValidatorTest {

    private final SourceUploadValidator validator = new SourceUploadValidator(
            new PdfDocumentParser(new PageMarginFilter(ingestionProperties())));

    private static MockMultipartFile file(String fileName, byte[] content) {
        return new MockMultipartFile("file", fileName, "application/pdf", content);
    }

    private static MockMultipartFile validFile(String fileName) {
        return file(fileName, validPdf());
    }

    private static Violation violation(String field, String message) {
        return new Violation(field, message);
    }

    private static ValidationFailedException validationFailed(Throwable thrown) {
        assertThat(thrown).isInstanceOf(ValidationFailedException.class);
        return (ValidationFailedException) thrown;
    }

    @Test
    void givenNameWithSurroundingSpaces_whenValidate_thenNameIsStripped() {
        // Act
        SourceUpload upload = validator.validate(validFile("a.pdf"), "  Şirket El Kitabı  ", "1");

        // Assert
        assertThat(upload.name()).isEqualTo("Şirket El Kitabı");
    }

    @Test
    void givenBlankName_whenValidate_thenNameIsFileNameWithoutExtension() {
        // Act
        SourceUpload upload = validator.validate(validFile("Atlaskap-El-Kitabi.pdf"), "   ", "1");

        // Assert
        assertThat(upload.name()).isEqualTo("Atlaskap-El-Kitabi");
    }

    @Test
    void givenTurkishFileNameWithUpperCaseExtension_whenValidate_thenAcceptedAndNameKeepsTurkishLetters() {
        // Act
        SourceUpload upload = validator.validate(validFile("Şirket El Kitabı.PDF"), null, "1");

        // Assert
        assertThat(upload.name()).isEqualTo("Şirket El Kitabı");
        assertThat(upload.fileName()).isEqualTo("Şirket El Kitabı.PDF");
    }

    @Test
    void givenFileNameOf90CharsAndNoName_whenValidate_thenNameIsFirst80Chars() {
        // Act
        SourceUpload upload = validator.validate(validFile("a".repeat(90) + ".pdf"), null, "1");

        // Assert
        assertThat(upload.name()).isEqualTo("a".repeat(80));
    }

    @Test
    void givenFileNameA_whenValidate_thenThrowsValidationFailedOnName() {
        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(validFile("a.pdf"), null, "1"));

        // Assert
        assertThat(thrown).isInstanceOf(ValidationFailedException.class)
                .extracting("errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(validationFailed(thrown).getViolations())
                .containsExactly(violation("name", "size must be between 2 and 80"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 81})
    void givenNamesOf1And81Chars_whenValidate_thenViolationOnName(int length) {
        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(validFile("x.pdf"), "ş".repeat(length), "1"));

        // Assert
        assertThat(validationFailed(thrown).getViolations())
                .containsExactly(violation("name", "size must be between 2 and 80"));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 80})
    void givenNamesOf2And80Chars_whenValidate_thenAccepted(int length) {
        // Act
        SourceUpload upload = validator.validate(validFile("x.pdf"), "ş".repeat(length), "1");

        // Assert
        assertThat(upload.name()).isEqualTo("ş".repeat(length));
    }

    @Test
    void givenEmptyOrMissingFile_whenValidate_thenThrowsValidationFailedOnFile() {
        // Act
        Throwable empty = catchThrowable(() -> validator.validate(file("a.pdf", new byte[0]), "Rehber", "1"));
        Throwable missing = catchThrowable(() -> validator.validate(null, "Rehber", "1"));

        // Assert
        assertThat(validationFailed(empty).getViolations()).containsExactly(violation("file", "must not be empty"));
        assertThat(validationFailed(missing).getViolations()).containsExactly(violation("file", "must not be empty"));
    }

    @Test
    void givenFileNameLongerThan255Chars_whenValidate_thenViolationOnFile() {
        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(validFile("a".repeat(252) + ".pdf"), "Rehber", "1"));

        // Assert
        assertThat(validationFailed(thrown).getViolations())
                .containsExactly(violation("file", "file name must be at most 255 characters"));
    }

    @Test
    void givenFileNameWithDirectories_whenValidate_thenOnlyLastSegmentIsKept() {
        // Act
        SourceUpload upload = validator.validate(validFile("C:\\belgelerim\\yeni/Rehber.pdf"), null, "1");

        // Assert
        assertThat(upload.fileName()).isEqualTo("Rehber.pdf");
        assertThat(upload.name()).isEqualTo("Rehber");
    }

    @Test
    void givenVersionMissingBlankOr41Chars_whenValidate_thenViolationOnVersion() {
        // Act
        Throwable missing = catchThrowable(() -> validator.validate(validFile("a.pdf"), "Rehber", null));
        Throwable blank = catchThrowable(() -> validator.validate(validFile("a.pdf"), "Rehber", "  "));
        Throwable tooLong = catchThrowable(() -> validator.validate(validFile("a.pdf"), "Rehber", "v".repeat(41)));

        // Assert
        assertThat(validationFailed(missing).getViolations()).containsExactly(violation("version", "must not be blank"));
        assertThat(validationFailed(blank).getViolations()).containsExactly(violation("version", "must not be blank"));
        assertThat(validationFailed(tooLong).getViolations())
                .containsExactly(violation("version", "size must be between 1 and 40"));
    }

    @Test
    void givenVersionWithSpacesOrOf40Chars_whenValidate_thenAcceptedAndStripped() {
        // Act
        SourceUpload spaced = validator.validate(validFile("a.pdf"), "Rehber", " 2026.10 ");
        SourceUpload longest = validator.validate(validFile("a.pdf"), "Rehber", "v".repeat(40));

        // Assert
        assertThat(spaced.version()).isEqualTo("2026.10");
        assertThat(longest.version()).isEqualTo("v".repeat(40));
    }

    @Test
    void givenEmptyFileAnd81CharNameAndNoVersion_whenValidate_thenViolationsInFileNameVersionOrder() {
        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(file("a.pdf", new byte[0]), "n".repeat(81), null));

        // Assert
        assertThat(validationFailed(thrown).getViolations()).containsExactly(
                violation("file", "must not be empty"),
                violation("name", "size must be between 2 and 80"),
                violation("version", "must not be blank"));
    }

    @Test
    void givenTxtFile_whenValidate_thenThrowsUnsupportedFileType() {
        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(file("notlar.txt", validPdf()), "Notlar", "1"));

        // Assert
        assertThat(thrown).isInstanceOf(UnsupportedMediaTypeException.class)
                .extracting("errorCode").isEqualTo("UNSUPPORTED_FILE_TYPE");
    }

    @Test
    void givenPdfExtensionWithoutPdfSignature_whenValidate_thenThrowsUnsupportedFileType() {
        // Act
        Throwable thrown = catchThrowable(() ->
                validator.validate(file("sahte.pdf", "merhaba".getBytes(StandardCharsets.UTF_8)), "Sahte", "1"));

        // Assert
        assertThat(thrown).isInstanceOf(UnsupportedMediaTypeException.class)
                .extracting("errorCode").isEqualTo("UNSUPPORTED_FILE_TYPE");
    }

    @Test
    void givenSignatureAtByte600_whenValidate_thenAcceptedAsPdf() {
        // Arrange
        byte[] valid = validPdf();
        byte[] shifted = new byte[600 + valid.length];
        Arrays.fill(shifted, 0, 600, (byte) ' ');
        System.arraycopy(valid, 0, shifted, 600, valid.length);

        // Act
        SourceUpload upload = validator.validate(file("kaydirilmis.pdf", shifted), "Kaydırılmış", "1");

        // Assert
        assertThat(upload.content()).isEqualTo(shifted);
    }

    @Test
    void givenSignatureEndingExactlyAtByte1024_whenValidate_thenAcceptedAsPdf() {
        // Arrange
        byte[] valid = validPdf();
        byte[] shifted = new byte[1019 + valid.length];
        Arrays.fill(shifted, 0, 1019, (byte) ' ');
        System.arraycopy(valid, 0, shifted, 1019, valid.length);

        // Act
        SourceUpload upload = validator.validate(file("sinir.pdf", shifted), "Sınır", "1");

        // Assert
        assertThat(upload.content()).isEqualTo(shifted);
    }

    @Test
    void givenSignatureOnlyAfterFirstKilobyte_whenValidate_thenThrowsUnsupportedFileType() {
        // Arrange
        byte[] valid = validPdf();
        byte[] shifted = new byte[1100 + valid.length];
        Arrays.fill(shifted, 0, 1100, (byte) ' ');
        System.arraycopy(valid, 0, shifted, 1100, valid.length);

        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(file("gec.pdf", shifted), "Geç", "1"));

        // Assert
        assertThat(thrown).isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void givenTxtFileAndBlankVersion_whenValidate_thenFieldViolationWinsOverFileType() {
        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(file("notlar.txt", validPdf()), "Notlar", " "));

        // Assert
        assertThat(thrown).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void givenPdfWithoutBookmarks_whenValidate_thenThrowsPdfNoBookmarks() {
        // Arrange
        byte[] bytes = pdf(false, PageSpec.of(null, "Satır bir.", "Satır iki."));

        // Act
        Throwable thrown = catchThrowable(() -> validator.validate(file("yer-imsiz.pdf", bytes), "Yer imsiz", "1"));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_BOOKMARKS");
    }

    @Test
    void givenValidPdf_whenValidate_thenReturnsUploadWithOriginalBytes() {
        // Arrange
        byte[] bytes = validPdf();

        // Act
        SourceUpload upload = validator.validate(file("Rehber.pdf", bytes), "Rehber", "2026.10");

        // Assert
        assertThat(upload).isEqualTo(new SourceUpload("Rehber", "2026.10", "Rehber.pdf", upload.content()));
        assertThat(upload.content()).isEqualTo(bytes);
    }
}
