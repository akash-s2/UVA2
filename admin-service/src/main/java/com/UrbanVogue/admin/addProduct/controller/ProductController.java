package com.UrbanVogue.admin.addProduct.controller;

import com.UrbanVogue.admin.addProduct.dto.ProductRequestDTO;
import com.UrbanVogue.admin.addProduct.dto.ProductResponseDTO;
import com.UrbanVogue.admin.addProduct.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/add")
    public ResponseEntity<ProductResponseDTO> addProduct(@Valid @RequestBody ProductRequestDTO productRequestDTO) {
        return new ResponseEntity<>(productService.addProduct(productRequestDTO), HttpStatus.CREATED);
    }
}