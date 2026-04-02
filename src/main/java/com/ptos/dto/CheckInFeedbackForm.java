package com.ptos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CheckInFeedbackForm {

    @NotBlank(message = "Feedback is required")
    @Size(max = 2000, message = "Feedback must be at most 2000 characters")
    private String feedbackText;
}
