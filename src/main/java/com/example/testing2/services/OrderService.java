package com.example.testing2.services;

import com.example.testing2.models.Product;

public class OrderService {

    private final InventoryService inventoryService = new InventoryService();

    public String recordSale(String sku, int qty) {
        Product p = inventoryService.findBySku(sku);

        if (p == null) return "Product not found!";
        if (qty <= 0) return "Invalid quantity!";
        if (p.getStock() < qty) return "Not enough stock available!";

        inventoryService.reduceStock(p, qty);
        return "Sale order recorded successfully!";
    }

    public String handleCustomerReturn(String sku, int qty) {
        Product p = inventoryService.findBySku(sku);
        if (p == null) return "Product not found!";
        if (qty <= 0) return "Invalid quantity!";

        inventoryService.increaseStock(p, qty);
        return "Customer return processed!";
    }

    public String updateOrderStatus(int orderId, String status) {
        return "Order " + orderId + " updated to: " + status;
    }
}
