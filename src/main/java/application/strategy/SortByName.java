package application.strategy;

import application.model.Product;

import java.util.Comparator;
import java.util.List;

public class SortByName implements SortStrategy{
    @Override
    public List<Product> sort(List<Product> products) {
        return products.stream().sorted(Comparator.comparing(Product::getProductName)).toList();
    }
}
