package application.service;

import application.dao.DatabaseConnection;
import application.model.Product;
import application.model.Category;
import application.repository.ProductRepository;
import application.strategy.SortStrategy;

import java.util.List;
import java.util.Optional;

public class ProductService {
    private final ProductRepository productRepo;
    private SortStrategy strategy;

    public ProductService(ProductRepository repo) {
        this.productRepo = repo;
    }

    public List<Product> getAll() {
        return productRepo.findAll(DatabaseConnection.getConnection(), false);
    }

    public List<Product> getAllIncludingArchived() {
        return productRepo.findAll(DatabaseConnection.getConnection(), true);
    }

    public List<Product> getProductsByCategory(String categoryName) {
        if ("All".equalsIgnoreCase(categoryName)) {
            return getAll();
        }
        return productRepo.findByCategory(DatabaseConnection.getConnection(), categoryName);
    }

    public List<Product> searchProductByTitle(String keyword) {
        return productRepo.search(DatabaseConnection.getConnection(), keyword);
    }

    public Optional<Product> getProductById(int id) {
        return productRepo.findById(DatabaseConnection.getConnection(), id);
    }

    public Optional<Product> findBySku(String sku) {
        return productRepo.findBySku(DatabaseConnection.getConnection(), sku);
    }

    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Product> sort(List<Product> products) {
        if (strategy != null) {
            return strategy.sort(products);
        }
        return products;
    }

    public void updateStock(Product product, int newStock) {
        productRepo.updateStock(DatabaseConnection.getConnection(), product, newStock);
        product.setQuantity(newStock);
    }

    public void createProduct(Product product) {
        productRepo.insert(DatabaseConnection.getConnection(), product);
    }

    public void updateProduct(Product product) {
        productRepo.update(DatabaseConnection.getConnection(), product);
    }

    public void archiveProduct(Product product) {
        productRepo.archive(DatabaseConnection.getConnection(), product.getId());
    }

    public Category buildCatalog() {
        Category catalogRoot = new Category("All Products");
        List<String> categoryNames = productRepo.findAllCategories(DatabaseConnection.getConnection());
        List<Product> products = getAll();
        
        for (String catName : categoryNames) {
            Category category = new Category(catName);
            for (Product p : products) {
                if (catName.equals(p.getCategory())) {
                    category.add(p);
                }
            }
            catalogRoot.add(category);
        }
        return catalogRoot;
    }
}
