package com.UrbanVogue.admin.orderHandling.controller;
import com.UrbanVogue.admin.addProduct.entity.Product;
//import com.UrbanVogue.admin.addProduct.service.InternalService;
import com.UrbanVogue.admin.orderHandling.dto.ProductInternalDTO;
import com.UrbanVogue.admin.orderHandling.service.InternalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequestMapping("/internal/products") //  INTERNAL APIs
public class InternalController {

    @Autowired
    private InternalService internalService;

    @GetMapping("/{id}")
    public ProductInternalDTO getProduct(@PathVariable Long id,
                                             @RequestParam Integer qty) {
        return internalService.getProductDetails(id, qty);
    }

    @PutMapping("/reduce/{id}")
    public void reduceStock(@PathVariable Long id,
                            @RequestParam Integer qty) {
        internalService.reduceStock(id, qty);
    }
}