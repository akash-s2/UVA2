package com.UrbanVogue.admin.addProduct.repository;

import com.UrbanVogue.admin.addProduct.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("UPDATE Product p SET p.numberOfPieces = p.numberOfPieces - :qty WHERE p.id = :id AND p.numberOfPieces >= :qty")
    int reduceStockAtomically(@Param("id") Long id, @Param("qty") Integer qty);
}