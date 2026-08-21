package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PaymentLinkRequest {
    @NotBlank(message = "A report reference is required.")
    @Size(max = 64)
    private String publicId;

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }
}
