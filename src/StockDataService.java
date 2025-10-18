import org.json.JSONObject;
import org.jfree.data.time.Day; // <-- NEW IMPORT
import org.jfree.data.time.TimeSeries; // <-- NEW IMPORT
import org.jfree.data.time.TimeSeriesCollection; // <-- NEW IMPORT
import org.jfree.data.xy.XYDataset; // <-- NEW IMPORT

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate; // <-- NEW IMPORT
import java.util.Iterator;

public class StockDataService {

    // IMPORTANT: Replace with your own free API key from Alpha Vantage
    private static final String API_KEY = "TYU7WMICU9X40ZOU"; // Use your actual key

    // Our original API for a single quote
    private static final String BASE_URL_QUOTE = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s";

    // NEW: API for historical data (last 100 days)
    private static final String BASE_URL_TIMESERIES = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s";

    private final HttpClient httpClient;

    public StockDataService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetches the current price and details for a stock.
     */
    public Stock fetchStockData(String symbol) throws Exception {
        if ("YOUR_API_KEY".equals(API_KEY)) {
            throw new Exception("Please replace 'YOUR_API_KEY' in StockDataService.java with your actual Alpha Vantage API key.");
        }

        String url = String.format(BASE_URL_QUOTE, symbol, API_KEY);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to fetch data from API. Status code: " + response.statusCode());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        JSONObject globalQuote = jsonResponse.optJSONObject("Global Quote");

        if (globalQuote == null || globalQuote.isEmpty()) {
            if (jsonResponse.has("Note")) {
                throw new Exception("API limit reached (5 calls/min). Please wait a minute and try again.");
            }
            throw new Exception("Invalid symbol or no data available for: " + symbol);
        }

        double price = Double.parseDouble(globalQuote.getString("05. price"));
        double open = Double.parseDouble(globalQuote.getString("02. open"));
        double high = Double.parseDouble(globalQuote.getString("03. high"));
        double low = Double.parseDouble(globalQuote.getString("04. low"));
        double change = Double.parseDouble(globalQuote.getString("09. change"));

        return new Stock(symbol, price, change, open, high, low);
    }

    /**
     * NEW: Fetches the last 100 days of stock data for the chart.
     */
    public XYDataset fetchChartData(String symbol) throws Exception {
        if ("YOUR_API_KEY".equals(API_KEY)) {
            throw new Exception("Please replace 'YOUR_API_KEY' in StockDataService.java with your actual Alpha Vantage API key.");
        }

        String url = String.format(BASE_URL_TIMESERIES, symbol, API_KEY);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to fetch chart data. Status code: " + response.statusCode());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        JSONObject timeSeriesData = jsonResponse.optJSONObject("Time Series (Daily)");

        if (timeSeriesData == null) {
            if (jsonResponse.has("Note")) {
                throw new Exception("API limit reached (5 calls/min). Please wait a minute and try again.");
            }
            throw new Exception("No time series data available for: " + symbol);
        }

        TimeSeries series = new TimeSeries(symbol);

        // Get all the date keys (e.g., "2025-10-17")
        Iterator<String> keys = timeSeriesData.keys();
        while(keys.hasNext()) {
            String dateStr = keys.next();
            JSONObject dayData = timeSeriesData.getJSONObject(dateStr);

            // Parse the date string "YYYY-MM-DD"
            LocalDate date = LocalDate.parse(dateStr);
            // Get the closing price
            double closePrice = Double.parseDouble(dayData.getString("4. close"));

            // Add to the series
            series.addOrUpdate(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), closePrice);
        }

        return new TimeSeriesCollection(series);
    }
}