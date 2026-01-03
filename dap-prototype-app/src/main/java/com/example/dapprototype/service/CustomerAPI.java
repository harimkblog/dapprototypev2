package com.example.dapprototype.service;

import com.example.dapprototype.model.Customer;
import com.example.dapprototype.model.CustomerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerAPI {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAPI.class);

    /**
     * Retrieves customer information for each customer ID in the request.
     * 
     * @param customerRequest the customer request containing customer IDs
     * @return list of Customer objects, one for each customer ID
     */
    public List<Customer> getCustomers(CustomerRequest customerRequest) {
        List<Customer> customers = new ArrayList<>();
        
        if (customerRequest == null || customerRequest.getCustomerIds() == null) {
            logger.warn("CustomerRequest or customerIds is null");
            return customers;
        }
        
        for (String customerId : customerRequest.getCustomerIds()) {
            // Generate a random customer name
            String customerName = generateRandomCustomerName();
            Customer customer = new Customer(customerId, customerName);
            customers.add(customer);
            logger.debug("Created customer: id={}, name={}", customerId, customerName);
        }
        
        logger.info("Retrieved {} customers", customers.size());
        return customers;
    }
    
    /**
     * Generates a random customer name.
     * 
     * @return a random customer name string
     */
    private String generateRandomCustomerName() {
        return "Customer-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
