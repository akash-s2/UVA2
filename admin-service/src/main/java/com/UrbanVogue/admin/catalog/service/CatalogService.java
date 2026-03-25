package com.UrbanVogue.admin.catalog.service;

import com.UrbanVogue.admin.addProduct.entity.Product;
import com.UrbanVogue.admin.addProduct.repository.ProductRepository;
import com.UrbanVogue.admin.catalog.dto.CatalogProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    @Autowired
    private ProductRepository productRepository;

    // for the explore section we have this methode
    public List<CatalogProductDTO> getProducts() {
        return productRepository.findAll()
                .stream()
                .map(product -> {
                    CatalogProductDTO dto = new CatalogProductDTO();
                    dto.setId(product.getId());
                    dto.setName(product.getName());
                    dto.setPrice(product.getPrice());
                    dto.setImageUrl(product.getImageUrl());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // for fetching the products details by id
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
}