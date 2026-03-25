package com.UrbanVogue.admin.orderHandling.service;

import com.UrbanVogue.admin.addProduct.entity.Product;
import com.UrbanVogue.admin.addProduct.repository.ProductRepository;
import com.UrbanVogue.admin.orderHandling.dto.ProductInternalDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalService {

    @Autowired
    private ProductRepository productRepository;

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public ProductInternalDTO getProductDetails(Long id, Integer qty) {
        Product product = getProduct(id);
        Integer available = (product.getNumberOfPieces() >= qty) ? qty : null;
        return new ProductInternalDTO(product.getPrice(), available, product.getName());
    }

    @Transactional
    public void reduceStock(Long id, Integer qty) {
        int updatedRows = productRepository.reduceStockAtomically(id, qty);
        if (updatedRows == 0) {
            throw new RuntimeException("Insufficient stock or product not found");
        }
    }
}