package com.example.dapprototype.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestInfo {
        private String activityId;
        private String activityTimeStamp;
        private String payeeCustomerId;
        private String payerCustomerId;
}
