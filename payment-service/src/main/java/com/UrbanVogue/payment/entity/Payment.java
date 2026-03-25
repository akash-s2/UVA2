package com.UrbanVogue.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    
    private Double amount;

    private String status;

    @Column(unique = true)
    private String idempotencyKey;

    private String transactionId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
