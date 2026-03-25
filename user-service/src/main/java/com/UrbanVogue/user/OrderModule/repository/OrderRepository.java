package com.UrbanVogue.user.OrderModule.repository;

import com.UrbanVogue.user.OrderModule.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}