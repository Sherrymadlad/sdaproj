package com.example.testing2.services;

import com.example.testing2.models.Product;
import java.util.ArrayList;
import java.util.List;

public class InventoryService {

    private static final List<Product> products = new ArrayList<>();

    static {
        products.add(new Product("SKU001", "Laptop", "Electronics", 50000, 20));
        products.add(new Product("SKU002", "Shirt", "Clothing", 1200, 50));
        products.add(new Product("SKU003", "Milk", "Grocery", 250, 100));
    }

    public List<Product> getAllProducts() {
        return products;
    }

    public Product findBySku(String sku) {
        return products.stream()
                .filter(p -> p.getSku().equalsIgnoreCase(sku))
                .findFirst()
                .orElse(null);
    }

    public void reduceStock(Product p, int qty) {
        p.setStock(p.getStock() - qty);
    }

    public void increaseStock(Product p, int qty) {
        p.setStock(p.getStock() + qty);
    }
}
