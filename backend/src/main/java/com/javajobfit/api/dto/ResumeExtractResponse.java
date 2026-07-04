package com.javajobfit.api.dto;

public class ResumeExtractResponse {
    private String text;
    private int characterCount;
    private String message;

    public ResumeExtractResponse(String text, int characterCount, String message) {
        this.text = text;
        this.characterCount = characterCount;
        this.message = message;
    }

    public String getText() {
        return text;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public String getMessage() {
        return message;
    }
}
