package com.UrbanVogue.user.OrderModule.controller;

import com.UrbanVogue.user.OrderModule.dto.OrderRequestDTO;
import com.UrbanVogue.user.OrderModule.dto.OrderResponseDTO;
import com.UrbanVogue.user.OrderModule.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/user/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<OrderResponseDTO> placeOrder(
            @Valid @RequestBody OrderRequestDTO request,
            @RequestHeader("Authorization") String authHeader
    ) {
        // remove Bearer prefix
        String token = authHeader.substring(7);

        return new ResponseEntity<>(orderService.placeOrder(request, token), HttpStatus.CREATED);
    }
}