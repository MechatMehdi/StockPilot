package application.model;

import java.util.ArrayList;
import java.util.List;

public class Category implements CatalogComponent {

    private final String name;
    private final List<CatalogComponent> children = new ArrayList<>();

    public Category(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getPrice() {
        double total = 0;
        for (CatalogComponent c : children) {
            total += c.getPrice();
        }
        return total;
    }

    // Composite behavior
    public void add(CatalogComponent component) {
        children.add(component);
    }

    /*
    public void remove(CatalogComponent component) {
        children.remove(component);
    }
     */

    public List<CatalogComponent> getChildren() {
        return children;
    }

    // Very important: flatten products
    public List<Product> getAllProducts() {
        List<Product> result = new ArrayList<>();
        for (CatalogComponent c : children) {
            if (c instanceof Product p) {
                result.add(p);
            } else if (c instanceof Category cat) {
                result.addAll(cat.getAllProducts());
            }
        }
        return result;
    }
}
