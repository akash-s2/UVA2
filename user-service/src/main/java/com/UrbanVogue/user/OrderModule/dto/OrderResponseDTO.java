package com.UrbanVogue.user.OrderModule.dto;

public class OrderResponseDTO {

    private String message;
    private String paymentStatus;
    private String orderStatus;

    public OrderResponseDTO(String message, String paymentStatus, String orderStatus) {
        this.message = message;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
    }

    public String getMessage() {
        return message;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }
}