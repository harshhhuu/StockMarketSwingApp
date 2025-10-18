import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class StockDataService {

    // IMPORTANT: Replace with your own free API key from Alpha Vantage
    private static final String API_KEY = "TYU7WMICU9X4OZOU";
    private static final String BASE_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s";

    private final HttpClient httpClient;

    public StockDataService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public Stock fetchStockData(String symbol) throws Exception {
        // First, check if the user has replaced the placeholder key
        if ("YOUR_API_KEY".equals(API_KEY)) {
            throw new Exception("Please replace 'YOUR_API_KEY' in StockDataService.java with your actual Alpha Vantage API key.");
        }

        String url = String.format(BASE_URL, symbol, API_KEY);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        // Send the request and get the response as a String
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to fetch data from API. Status code: " + response.statusCode());
        }

        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(response.body());
        JSONObject globalQuote = jsonResponse.optJSONObject("Global Quote");

        // Check for invalid symbol or API limit reached
        if (globalQuote == null || globalQuote.isEmpty()) {
            if (jsonResponse.has("Note")) {
                throw new Exception("API limit reached. Please wait a minute and try again.");
            }
            throw new Exception("Invalid symbol or no data available for: " + symbol);
        }

        // Extract data from the JSON object
        double price = Double.parseDouble(globalQuote.getString("05. price"));
        double open = Double.parseDouble(globalQuote.getString("02. open"));
        double high = Double.parseDouble(globalQuote.getString("03. high"));
        double low = Double.parseDouble(globalQuote.getString("04. low"));
        double change = Double.parseDouble(globalQuote.getString("09. change"));

        return new Stock(symbol, price, change, open, high, low);
    }
}