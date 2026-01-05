package com.example.dapprototype.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAssessmentData {
    private PaymentRequestInfo requestInfo;
    private Customer payeeCustomer;
    private Customer payerCustomer;
    private RulesResponse rulesResponse;
}
