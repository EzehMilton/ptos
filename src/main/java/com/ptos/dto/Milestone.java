package com.ptos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class Milestone {

    private String icon;
    private String title;
    private String description;
    private LocalDate achievedDate;
}
