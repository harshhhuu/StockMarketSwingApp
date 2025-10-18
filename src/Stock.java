import java.text.DecimalFormat;

public class Stock {
    private String symbol;
    private double price;
    private double change;
    private double changePercent;
    private double open;
    private double high;
    private double low;

    // A formatter for consistent number display
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public Stock(String symbol, double price, double change, double open, double high, double low) {
        this.symbol = symbol;
        this.price = price;
        this.change = change;
        this.open = open;
        this.high = high;
        this.low = low;
        // Calculate change percent based on price and change
        if (price - change != 0) {
            this.changePercent = (change / (price - change)) * 100;
        } else {
            this.changePercent = 0.0;
        }
    }

    // --- Getters for UI display ---

    public String getSymbol() { return symbol; }
    public String getPriceFormatted() {
        return "₹" + df.format(price);
    }
    public String getChangeFormatted() {
        return (change >= 0 ? "+" : "") + df.format(change);
    }
    public String getChangePercentFormatted() {
        return String.format("(%.2f%%)", changePercent);
    }
    public String getOpenFormatted() { return df.format(open); }
    public String getHighFormatted() { return df.format(high); }
    public String getLowFormatted() { return df.format(low); }

    // --- Getters for raw data ---
    public double getPrice() { return price; }
    public double getChange() { return change; }
    public double getChangePercent() { return changePercent; }
}