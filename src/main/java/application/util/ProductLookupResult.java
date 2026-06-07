package application.util;

public class ProductLookupResult {
    public String name;
    public String category;
    public String brand;
    public String description;
    public String imageUrl;

    public ProductLookupResult(String name, String category, String brand, String description, String imageUrl) {
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
