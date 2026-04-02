package com.ptos.dto;

import com.ptos.domain.ClientStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class ClientRecordUpdateForm {

    @NotNull(message = "Status is required")
    private ClientStatus status;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String ptNotes;

    @DecimalMin(value = "0", message = "Package price must be positive")
    private BigDecimal monthlyPackagePrice;
}
