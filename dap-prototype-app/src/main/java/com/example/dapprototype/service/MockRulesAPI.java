package com.example.dapprototype.service;

import com.example.dapprototype.model.RulesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

@Service
public class MockRulesAPI {

    private static final Logger logger = LoggerFactory.getLogger(MockRulesAPI.class);

    /**
     * Evaluates rules for the given decision data and updates the rulesResponse.
     * 
     * @param decisionData the decision data object to evaluate
     */
    public void evaluateRules(Object decisionData) {
        try {
            // Create a RulesResponse object with decision set to "Step Up"
            RulesResponse rulesResponse = new RulesResponse("Step Up");
            
            // Use reflection to set the rulesResponse on the DecisionData object
            Method setRulesResponseMethod = decisionData.getClass().getMethod("setRulesResponse", RulesResponse.class);
            setRulesResponseMethod.invoke(decisionData, rulesResponse);
            
            logger.info("Rules evaluated and rulesResponse set to: {}", rulesResponse.getDecision());
        } catch (Exception e) {
            logger.error("Failed to set rulesResponse on DecisionData", e);
            throw new RuntimeException("Failed to evaluate rules", e);
        }
    }
}
