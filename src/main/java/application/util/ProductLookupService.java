package application.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ProductLookupService {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String[] OPEN_FACTS_DOMAINS = {
            "world.openfoodfacts.org",
            "world.openproductsfacts.org",
            "world.openbeautyfacts.org"
    };

    public static ProductLookupResult lookup(String barcode) {
        System.out.println("==================================================");
        System.out.println("[ProductLookup] Starting 100% Legal Open Data lookup for barcode: " + barcode);
        System.out.println("==================================================");
        
        for (String domain : OPEN_FACTS_DOMAINS) {
            ProductLookupResult result = lookupOpenFactsDomain(barcode, domain);
            if (result != null) {
                System.out.println("[ProductLookup] ✅ SUCCESS: Found '" + result.name + "' via " + domain);
                System.out.println("==================================================");
                return result;
            }
        }
        
        System.out.println("[ProductLookup] ❌ FAILURE: Barcode not found in any Open Facts database.");
        System.out.println("==================================================");
        return null;
    }

    private static ProductLookupResult lookupOpenFactsDomain(String barcode, String domain) {
        try {
            String url = "https://" + domain + "/api/v0/product/" + barcode + ".json";
            System.out.println("[ProductLookup] Requesting URL: " + url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    // Legal requirement: Identifiable User-Agent for Open Food Facts TOS
                    .header("User-Agent", "StockPilot/1.0 (Windows; Java) - Open Source POS System")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                int status = json.optInt("status", 0);
                
                if (status == 1 && json.has("product")) {
                    JSONObject product = json.getJSONObject("product");
                    
                    String name = product.optString("product_name", "");
                    String brand = product.optString("brands", "");
                    String category = product.optString("categories", "");
                    if (category.contains(",")) {
                        category = category.split(",")[0].trim();
                    }
                    String imageUrl = product.optString("image_url", "");
                    
                    if (!name.isEmpty()) {
                        System.out.println("[ProductLookup] Extracted Name: " + name);
                        return new ProductLookupResult(name, category, brand, "", imageUrl);
                    }
                }
            } else if (response.statusCode() == 429) {
                System.out.println("[ProductLookup] Rate Limit Hit for " + domain);
            }
        } catch (Exception e) {
            System.err.println("[ProductLookup] Exception querying " + domain + ": " + e.getMessage());
        }
        return null;
    }
}
