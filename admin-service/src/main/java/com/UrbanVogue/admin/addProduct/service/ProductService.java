package com.UrbanVogue.admin.addProduct.service;

import com.UrbanVogue.admin.addProduct.dto.ProductRequestDTO;
import com.UrbanVogue.admin.addProduct.dto.ProductResponseDTO;
import com.UrbanVogue.admin.addProduct.entity.Product;
import com.UrbanVogue.admin.addProduct.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public ProductResponseDTO addProduct(ProductRequestDTO request) {

        // DTO → Entity
        Product product = new Product();
        product.setName(request.getName());
        product.setBrand(request.getBrand());
        product.setCategory(request.getCategory());
        product.setSize(request.getSize());
        product.setColor(request.getColor());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        product.setNumberOfPieces(request.getNumberOfPieces());
        // Save to DB
        Product savedProduct = productRepository.save(product);

        // Entity → Response DTO
        return new ProductResponseDTO(
                savedProduct.getId(),
                savedProduct.getName(),
                "Product added successfully"
        );
    }
}