package com.ptos.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PTProfileForm {

    @Size(max = 100, message = "Business name must be at most 100 characters")
    private String businessName;

    @Size(max = 200, message = "Specialisation must be at most 200 characters")
    private String specialisation;

    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;

    @Size(max = 1000, message = "Bio must be at most 1000 characters")
    private String bio;

    @Size(max = 500, message = "Logo URL must be at most 500 characters")
    private String logoUrl;
}
