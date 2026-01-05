package com.example.dapprototype.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DAResponse {
    private boolean success;
    private String message;
    private String code;
    private List<String> details;
    private RulesResponse rulesResponse;

}
