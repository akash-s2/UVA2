package com.UrbanVogue.admin.UpdateDetails.controller;

import com.UrbanVogue.admin.addProduct.entity.Product;
import com.UrbanVogue.admin.UpdateDetails.dto.UpdateProductRequestDTO;
import com.UrbanVogue.admin.UpdateDetails.service.UpdateProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/products")
public class UpdateProductController {

    private final UpdateProductService updateProductService;

    public UpdateProductController(UpdateProductService updateProductService) {
        this.updateProductService = updateProductService;
    }

    @PutMapping("/update/{id}")
    public Product updateProduct(
            @PathVariable Long id,
            @RequestBody UpdateProductRequestDTO request) {

        return updateProductService.updateProduct(id, request);
    }
}