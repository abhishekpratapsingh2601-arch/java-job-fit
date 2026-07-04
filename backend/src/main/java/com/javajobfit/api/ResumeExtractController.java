package com.javajobfit.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.tika.exception.WriteLimitReachedException;
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
    private static final long PARSE_TIMEOUT_SECONDS = 10L;
    private static final String UNREADABLE_MESSAGE = "Could not read this file. Please paste your resume text instead.";
    private static final Set<String> SUPPORTED_EXTENSIONS = Arrays.stream(new String[]{"pdf", "docx", "doc", "txt"})
            .collect(Collectors.toSet());

    // Bounded pool: at most 2 concurrent Tika parses plus 4 queued uploads. Anything beyond that
    // is shed with 503 instead of letting hostile or bursty uploads pin every CPU on the instance.
    private final ThreadPoolExecutor parsePool = new ThreadPoolExecutor(
            2, 2, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(4),
            new ThreadPoolExecutor.AbortPolicy());

    @PreDestroy
    void shutdownParsePool() {
        parsePool.shutdownNow();
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return badRequest(UNREADABLE_MESSAGE);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return badRequest(UNREADABLE_MESSAGE);
        }
        if (!isSupported(file.getOriginalFilename())) {
            return badRequest(UNREADABLE_MESSAGE);
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            return badRequest(UNREADABLE_MESSAGE);
        }
        final String safeName = safeFileName(file.getOriginalFilename());

        Future<String> parseTask;
        try {
            parseTask = parsePool.submit(() -> parseToText(bytes, safeName));
        } catch (RejectedExecutionException busy) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap(
                            "error", "Resume upload is busy right now. Please try again in a minute or paste your resume text."));
        }

        String extracted;
        try {
            extracted = parseTask.get(PARSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException timedOut) {
            parseTask.cancel(true);
            return badRequest("This file took too long to read. Please paste your resume text instead.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            parseTask.cancel(true);
            return badRequest(UNREADABLE_MESSAGE);
        } catch (Exception parseFailed) {
            return badRequest(UNREADABLE_MESSAGE);
        }

        extracted = extracted.replaceAll("\\s+", " ").trim();
        if (extracted.length() < 40) {
            return badRequest(UNREADABLE_MESSAGE);
        }
        if (extracted.length() > MAX_CHARACTERS) {
            extracted = extracted.substring(0, MAX_CHARACTERS).trim();
        }
        return ResponseEntity.ok(new ResumeExtractResponse(
                extracted,
                extracted.length(),
                "Resume text extracted. Please review before analyzing."));
    }

    private String parseToText(byte[] bytes, String resourceName) throws Exception {
        ContentHandler handler = new BodyContentHandler(MAX_CHARACTERS + 1);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, resourceName);
        try (InputStream input = new ByteArrayInputStream(bytes)) {
            new AutoDetectParser().parse(input, handler, metadata, new ParseContext());
        } catch (WriteLimitReachedException reachedLimit) {
            // Expected for long resumes: keep the partial text already captured by the handler
            // and let the caller truncate, instead of rejecting a valid 4-5 page resume.
        }
        return handler.toString();
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
