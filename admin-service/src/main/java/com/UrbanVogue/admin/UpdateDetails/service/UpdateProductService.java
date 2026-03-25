package com.UrbanVogue.admin.UpdateDetails.service;

import com.UrbanVogue.admin.addProduct.entity.Product;
import com.UrbanVogue.admin.addProduct.repository.ProductRepository;
import com.UrbanVogue.admin.UpdateDetails.dto.UpdateProductRequestDTO;
import org.springframework.stereotype.Service;

@Service
public class UpdateProductService {

    private final ProductRepository productRepository;

    public UpdateProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product updateProduct(Long productId, UpdateProductRequestDTO dto) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (dto.getName() != null) {
            product.setName(dto.getName());
        }

        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }

        if (dto.getSize() != null) {
            product.setSize(dto.getSize());
        }

        return productRepository.save(product);
    }
}