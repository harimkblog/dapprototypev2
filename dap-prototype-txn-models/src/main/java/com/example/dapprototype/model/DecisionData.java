package com.example.dapprototype.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionData {
    private RequestInfo requestInfo;
    private Customer payeeCustomer;
    private Customer payerCustomer;
}
