package com.javajobfit.api;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import com.javajobfit.api.dto.ResumeExtractResponse;

@RestController
@RequestMapping("/api/resume")
public class ResumeExtractController {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_CHARACTERS = 12000;
    private static final Set<String> SUPPORTED_EXTENSIONS = Arrays.stream(new String[]{"pdf", "docx", "doc", "txt"})
            .collect(Collectors.toSet());

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return badRequest("Could not read this file. Please paste your resume text instead.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return badRequest("Could not read this file. Please paste your resume text instead.");
        }
        if (!isSupported(file.getOriginalFilename())) {
            return badRequest("Could not read this file. Please paste your resume text instead.");
        }
        try (InputStream input = file.getInputStream()) {
            ContentHandler handler = new BodyContentHandler(MAX_CHARACTERS + 1);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, safeFileName(file.getOriginalFilename()));
            new AutoDetectParser().parse(input, handler, metadata, new ParseContext());
            String extracted = handler.toString().replaceAll("\\s+", " ").trim();
            if (extracted.length() < 40) {
                return badRequest("Could not read this file. Please paste your resume text instead.");
            }
            if (extracted.length() > MAX_CHARACTERS) {
                extracted = extracted.substring(0, MAX_CHARACTERS).trim();
            }
            return ResponseEntity.ok(new ResumeExtractResponse(
                    extracted,
                    extracted.length(),
                    "Resume text extracted. Please review before analyzing."));
        } catch (Exception ignored) {
            return badRequest("Could not read this file. Please paste your resume text instead.");
        }
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", message));
    }

    private boolean isSupported(String filename) {
        String safe = safeFileName(filename);
        int dot = safe.lastIndexOf('.');
        if (dot < 0 || dot == safe.length() - 1) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(safe.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private String safeFileName(String filename) {
        return filename == null ? "" : filename.replaceAll("[^A-Za-z0-9._-]", "");
    }
}
