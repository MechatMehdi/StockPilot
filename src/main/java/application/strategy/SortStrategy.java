package application.strategy;

import application.model.Product;

import java.util.List;

public interface SortStrategy {
    List<Product> sort(List<Product> products);
}
