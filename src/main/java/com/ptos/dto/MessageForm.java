package com.ptos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageForm {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    private String content;
}
