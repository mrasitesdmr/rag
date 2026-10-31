package com.rasitbank.retrieval.source.service;

import com.rasitbank.retrieval.exception.UnprocessableContentException;
import com.rasitbank.retrieval.exception.UnsupportedMediaTypeException;
import com.rasitbank.retrieval.exception.ValidationFailedException;
import com.rasitbank.retrieval.exception.Violation;
import com.rasitbank.retrieval.ingestion.parsing.PdfDocumentParser;
import com.rasitbank.retrieval.source.domain.dto.SourceUpload;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Belge yüklemesinin parçalarını sırayla denetler (alanlar, dosya türü, PDF içeriği) ve geçerse yüklemeyi hazırlar;
 * hiçbir şey yazmaz.
 */
@Component
public class SourceUploadValidator {

    private static final Logger log = LoggerFactory.getLogger(SourceUploadValidator.class);

    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_VERSION_LENGTH = 40;
    private static final int SIGNATURE_SEARCH_BYTES = 1024;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String PDF_EXTENSION = ".pdf";
    private static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";

    private static final String FILE_FIELD = "file";
    private static final String NAME_FIELD = "name";
    private static final String VERSION_FIELD = "version";
    private static final String EMPTY_FILE_MESSAGE = "must not be empty";
    private static final String FILE_NAME_TOO_LONG_MESSAGE = "file name must be at most 255 characters";
    private static final String NAME_SIZE_MESSAGE = "size must be between 2 and 80";
    private static final String BLANK_VERSION_MESSAGE = "must not be blank";
    private static final String VERSION_SIZE_MESSAGE = "size must be between 1 and 40";

    private final PdfDocumentParser parser;

    public SourceUploadValidator(PdfDocumentParser parser) {
        this.parser = parser;
    }

    public SourceUpload validate(@Nullable MultipartFile file, @Nullable String name, @Nullable String version) {
        List<Violation> violations = new ArrayList<>();
        String fileName = fileName(file);
        if (file == null || file.isEmpty()) {
            violations.add(new Violation(FILE_FIELD, EMPTY_FILE_MESSAGE));
        } else if (length(fileName) > MAX_FILE_NAME_LENGTH) {
            violations.add(new Violation(FILE_FIELD, FILE_NAME_TOO_LONG_MESSAGE));
        }
        String sourceName = sourceName(name, fileName);
        if (length(sourceName) < MIN_NAME_LENGTH || length(sourceName) > MAX_NAME_LENGTH) {
            violations.add(new Violation(NAME_FIELD, NAME_SIZE_MESSAGE));
        }
        String versionLabel = version == null ? "" : version.strip();
        if (versionLabel.isEmpty()) {
            violations.add(new Violation(VERSION_FIELD, BLANK_VERSION_MESSAGE));
        } else if (length(versionLabel) > MAX_VERSION_LENGTH) {
            violations.add(new Violation(VERSION_FIELD, VERSION_SIZE_MESSAGE));
        }
        if (!violations.isEmpty()) {
            throw new ValidationFailedException(violations);
        }
        byte[] content = readContent(Objects.requireNonNull(file));
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(PDF_EXTENSION) || !hasPdfSignature(content)) {
            log.info("Upload rejected errorCode={} fileSizeBytes={}", UNSUPPORTED_FILE_TYPE, content.length);
            throw new UnsupportedMediaTypeException(UNSUPPORTED_FILE_TYPE, "Only PDF files are accepted");
        }
        try {
            parser.parse(content);
        } catch (UnprocessableContentException exception) {
            log.info("Upload rejected errorCode={} fileSizeBytes={}", exception.getErrorCode(), content.length);
            throw exception;
        }
        return new SourceUpload(sourceName, versionLabel, fileName, content);
    }

    private static String fileName(@Nullable MultipartFile file) {
        String originalFilename = file == null ? null : file.getOriginalFilename();
        if (originalFilename == null) {
            return "";
        }
        int lastSeparator = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        return originalFilename.substring(lastSeparator + 1);
    }

    private static String sourceName(@Nullable String name, String fileName) {
        String givenName = name == null ? "" : name.strip();
        if (!givenName.isEmpty()) {
            return givenName;
        }
        int lastDot = fileName.lastIndexOf('.');
        String derivedName = (lastDot >= 0 ? fileName.substring(0, lastDot) : fileName).strip();
        if (length(derivedName) > MAX_NAME_LENGTH) {
            derivedName = derivedName.substring(0, derivedName.offsetByCodePoints(0, MAX_NAME_LENGTH)).strip();
        }
        return derivedName;
    }

    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }

    private static byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Uploaded file could not be read", exception);
        }
    }

    private static boolean hasPdfSignature(byte[] content) {
        int searchEnd = Math.min(content.length, SIGNATURE_SEARCH_BYTES) - PDF_SIGNATURE.length;
        for (int offset = 0; offset <= searchEnd; offset++) {
            if (Arrays.equals(content, offset, offset + PDF_SIGNATURE.length, PDF_SIGNATURE, 0,
                    PDF_SIGNATURE.length)) {
                return true;
            }
        }
        return false;
    }
}
