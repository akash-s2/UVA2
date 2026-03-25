package com.UrbanVogue.user.OrderModule.client;

import java.util.UUID;

import com.UrbanVogue.user.OrderModule.dto.PaymentRequestDTO;
import com.UrbanVogue.user.OrderModule.dto.PaymentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    @Value("${service.payment.url}")
    private String paymentServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    public PaymentResponseDTO processPayment(Long orderId, Double amount) {
        
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequestDTO request = new PaymentRequestDTO(orderId, amount, idempotencyKey);

        return restTemplate.postForObject(
                paymentServiceUrl + "/payment/process",
                request,
                PaymentResponseDTO.class
        );
    }
}