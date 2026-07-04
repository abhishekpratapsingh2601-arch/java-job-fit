package com.javajobfit.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeExtractControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void extractsTextFileWithoutStoringOrEchoingErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "Java Spring Boot resume with REST API and SQL experience.".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resume/extract").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(containsString("Java Spring Boot")))
                .andExpect(jsonPath("$.characterCount").isNumber())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void extractsDocxResumeText() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxWithText("Java Spring Boot DOCX resume with REST API SQL and JUnit experience."));

        mockMvc.perform(multipart("/api/resume/extract").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(containsString("Java Spring Boot DOCX resume")))
                .andExpect(jsonPath("$.text").value(containsString("REST API SQL")));
    }

    @Test
    void extractsPdfResumeText() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                pdfWithText("Java Spring Boot PDF resume with REST API SQL and Mockito experience."));

        mockMvc.perform(multipart("/api/resume/extract").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(containsString("Java Spring Boot PDF resume")))
                .andExpect(jsonPath("$.text").value(containsString("REST API SQL")));
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.exe",
                "application/octet-stream",
                "private resume phone 9999999999".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resume/extract").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Could not read this file. Please paste your resume text instead."))
                .andExpect(content().string(not(containsString("9999999999"))));
    }

    @Test
    void rejectsTooLargeFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                new byte[(5 * 1024 * 1024) + 1]);

        mockMvc.perform(multipart("/api/resume/extract").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Could not read this file. Please paste your resume text instead."));
    }

    @Test
    void truncatesVeryLongResumeInsteadOfRejectingIt() throws Exception {
        // ~30k chars of text — more than the 12k extraction cap. A long but valid resume must
        // still extract (truncated), not fail with "could not read this file".
        StringBuilder longText = new StringBuilder();
        while (longText.length() < 30000) {
            longText.append("Java Spring Boot backend engineer with REST API SQL JUnit Docker experience. ");
        }
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "long-resume.txt",
                "text/plain",
                longText.toString().getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resume/extract").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(containsString("Java Spring Boot backend engineer")))
                .andExpect(jsonPath("$.characterCount").value(lessThanOrEqualTo(12000)));
    }

    private byte[] docxWithText(String text) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                    + "</Types>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                    + "</Relationships>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                    + "<w:body><w:p><w:r><w:t>"
                    + escapeXml(text)
                    + "</w:t></w:r></w:p></w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private byte[] pdfWithText(String text) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
        }
        return output.toByteArray();
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
