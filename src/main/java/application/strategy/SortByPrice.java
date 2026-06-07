package application.strategy;

import application.model.Product;

import java.util.Comparator;
import java.util.List;

public class SortByPrice implements SortStrategy{
    @Override
    public List<Product> sort(List<Product> products) {
        return products.stream().sorted(Comparator.comparing(Product::getPrice)).toList();
    }
}
