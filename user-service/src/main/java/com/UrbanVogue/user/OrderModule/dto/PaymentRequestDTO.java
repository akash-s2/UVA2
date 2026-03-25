package com.UrbanVogue.user.OrderModule.dto;

public class PaymentRequestDTO {

    private Long orderId;
    private Double amount;
    private String idempotencyKey;

    public PaymentRequestDTO(Long orderId, Double amount, String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Double getAmount() {
        return amount;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
}