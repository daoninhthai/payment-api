package com.daoninhthai.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {

    @NotBlank(message = "URL is required")
    @URL(message = "Invalid URL format")
    private String url;

    @NotBlank(message = "Secret is required")
    private String secret;
}
