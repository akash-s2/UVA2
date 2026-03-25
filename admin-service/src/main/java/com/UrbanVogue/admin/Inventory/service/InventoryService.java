package com.UrbanVogue.admin.Inventory.service;

import com.UrbanVogue.admin.addProduct.entity.Product;
import com.UrbanVogue.admin.addProduct.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    //  Update stock for a particular product
    public Product updateStock(Long productId, int numberOfPieces) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setNumberOfPieces((long) numberOfPieces);
        return productRepository.save(product);
    }

    //  Fetch all products (for inventory view)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}