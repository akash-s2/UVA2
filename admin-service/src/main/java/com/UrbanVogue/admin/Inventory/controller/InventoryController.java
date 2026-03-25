package com.UrbanVogue.admin.Inventory.controller;

import com.UrbanVogue.admin.addProduct.entity.Product;
import com.UrbanVogue.admin.Inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // Update stock for a product
    @PutMapping("/{productId}")
    public ResponseEntity<String> updateStock(@PathVariable Long productId,
                                              @RequestParam int numberOfPieces) {
        inventoryService.updateStock(productId, numberOfPieces);
        return ResponseEntity.ok("Stock updated for product id: " + productId);
    }

    //  Fetch all products (admin view)
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }
}