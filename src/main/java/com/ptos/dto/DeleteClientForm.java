package com.ptos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeleteClientForm {

    @NotBlank(message = "Please enter the client's full name to confirm deletion")
    private String confirmationName;
}
