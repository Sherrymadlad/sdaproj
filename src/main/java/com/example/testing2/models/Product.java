package com.example.testing2.models;

public class Product {
    private String sku;
    private String name;
    private String category;
    private double price;
    private int stock;

    public Product(String sku, String name, String category, double price, int stock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }

    public void setStock(int stock) { this.stock = stock; }
}
