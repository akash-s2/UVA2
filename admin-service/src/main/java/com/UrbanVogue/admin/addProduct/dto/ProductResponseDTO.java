package com.UrbanVogue.admin.addProduct.dto;

public class ProductResponseDTO {

    private Long id;
    private String name;
    private String message;

    public ProductResponseDTO(Long id, String name, String message) {
        this.id = id;
        this.name = name;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }
}