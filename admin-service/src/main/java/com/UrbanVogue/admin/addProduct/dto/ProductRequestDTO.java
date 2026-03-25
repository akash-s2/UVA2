package com.UrbanVogue.admin.addProduct.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;public class ProductRequestDTO {
    @NotBlank(message = "Name cannot be blank")
    private String name;
    @NotBlank(message = "Brand cannot be blank")
    private String brand;
    @NotBlank(message = "Category cannot be blank")
    private String category;
    @NotBlank(message = "Size cannot be blank")
    private String size;
    @NotBlank(message = "Color cannot be blank")
    private String color;
    @NotNull(message = "Price cannot be null")
    @Min(value = 0, message = "Price cannot be negative")
    private Double price;
    @NotBlank(message = "Image URL cannot be blank")
    private String imageUrl;
    @NotNull(message = "Number of pieces cannot be null")
    @Min(value = 0, message = "Number of pieces cannot be negative")
    private Long   numberOfPieces;
    @NotBlank(message = "Description cannot be blank")
    private String description;

    public ProductRequestDTO() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getNumberOfPieces()
    {
        return numberOfPieces;
    }
    public void setNumberOfPieces(Long numberOfPieces)
    {
        this.numberOfPieces=numberOfPieces;
    }
}