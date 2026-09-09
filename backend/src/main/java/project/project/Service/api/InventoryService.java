package project.project.Service.api;

import project.project.Entity.product.Product;

import java.util.Map;

public interface InventoryService {

    Map<Long, Product> reserve(Map<Long, Integer> quantities);

    void release(Map<Long, Integer> quantities);
}