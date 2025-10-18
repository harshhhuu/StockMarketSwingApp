import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

// Our main window class, extending JFrame
public class StockMarketSwingUI extends JFrame {

    // --- UI Component Declarations ---
    // (No changes here)
    private JTextField symbolTextField;
    private JButton searchButton;
    private JButton addToWatchlistButton;
    private JLabel stockSymbolLabel;
    private JLabel stockPriceLabel;
    private JLabel stockChangeLabel;
    private JLabel stockChangePercentLabel;
    private JLabel openPriceLabel;
    private JLabel highPriceLabel;
    private JLabel lowPriceLabel;
    private JTable watchlistTable;
    private DefaultTableModel watchlistTableModel;

    // --- Data Members ---
    private Stock currentStock;
    private final StockDataService stockDataService; // <-- ADD THIS

    public StockMarketSwingUI() {
        // --- Initialize the data service ---
        this.stockDataService = new StockDataService(); // <-- ADD THIS

        // --- 1. Main Window Setup ---
        setTitle("Stock Market Viewer (Swing Edition)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- 2. Create and Add Panels ---
        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();
        JPanel bottomPanel = createBottomPanel();
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 3. Add Event Listeners ---
        addEventListeners();

        // --- 4. Finalize Window ---
        pack();
        setLocationRelativeTo(null);
    }

    // (createTopPanel, createCenterPanel, createBottomPanel, and helper methods are unchanged)

    // ... (All other panel creation methods are the same) ...
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Stock Search"));
        JLabel titleLabel = new JLabel("Stock Market Viewer");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);
        JPanel searchBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        symbolTextField = new JTextField(20);
        symbolTextField.setToolTipText("e.g., RELIANCE.BSE or TCS.NSE");
        searchButton = new JButton("Search");
        addToWatchlistButton = new JButton("Add to Watchlist");
        searchBoxPanel.add(new JLabel("Symbol:"));
        searchBoxPanel.add(symbolTextField);
        searchBoxPanel.add(searchButton);
        searchBoxPanel.add(addToWatchlistButton);
        panel.add(searchBoxPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        stockSymbolLabel = new JLabel("SELECT A STOCK");
        stockSymbolLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(stockSymbolLabel, gbc);
        stockPriceLabel = new JLabel("₹0.00");
        stockPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(stockPriceLabel, gbc);
        JPanel changePanel = new JPanel();
        changePanel.setLayout(new BoxLayout(changePanel, BoxLayout.Y_AXIS));
        stockChangeLabel = new JLabel("+0.00");
        stockChangeLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        stockChangePercentLabel = new JLabel("(0.00%)");
        stockChangePercentLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        changePanel.add(stockChangeLabel);
        changePanel.add(stockChangePercentLabel);
        gbc.gridx = 1;
        panel.add(changePanel, gbc);
        openPriceLabel = createDetailLabel("0.00");
        highPriceLabel = createDetailLabel("0.00");
        lowPriceLabel = createDetailLabel("0.00");
        JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
        detailsPanel.add(createDetailBox("OPEN", openPriceLabel));
        detailsPanel.add(createDetailBox("HIGH", highPriceLabel));
        detailsPanel.add(createDetailBox("LOW", lowPriceLabel));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(detailsPanel, gbc);
        return panel;
    }

    private JPanel createDetailBox(String title, JLabel valueLabel) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(titleLabel);
        box.add(valueLabel);
        return box;
    }

    private JLabel createDetailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        return label;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Watchlist"));
        String[] columnNames = {"Symbol", "Price", "Change", "Change %"};
        watchlistTableModel = new DefaultTableModel(columnNames, 0);
        watchlistTable = new JTable(watchlistTableModel);
        watchlistTable.setFillsViewportHeight(true);
        watchlistTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        watchlistTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(watchlistTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    // --- THIS SECTION IS COMPLETELY REWRITTEN ---

    private void addEventListeners() {
        searchButton.addActionListener(e -> handleSearchAction());
        addToWatchlistButton.addActionListener(e -> handleAddToWatchlist());
    }

    private void handleSearchAction() {
        String symbol = symbolTextField.getText().trim().toUpperCase();
        if (symbol.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a stock symbol.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- UI State Change: Show loading ---
        searchButton.setEnabled(false);
        stockSymbolLabel.setText("Loading...");
        stockSymbolLabel.setFont(new Font("SansSerif", Font.BOLD, 30)); // Slightly smaller font

        // --- Background Task with SwingWorker ---
        SwingWorker<Stock, Void> worker = new SwingWorker<>() {
            @Override
            protected Stock doInBackground() throws Exception {
                // This happens on a background thread
                return stockDataService.fetchStockData(symbol);
            }

            @Override
            protected void done() {
                // This happens on the UI thread after doInBackground is finished
                try {
                    currentStock = get(); // Get the result from doInBackground
                    updateCenterPanel(currentStock);
                } catch (Exception ex) {
                    currentStock = null; // Clear current stock on error
                    // Display the error message from our service
                    JOptionPane.showMessageDialog(StockMarketSwingUI.this,
                            ex.getCause().getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                    stockSymbolLabel.setText("SEARCH FAILED");
                } finally {
                    searchButton.setEnabled(true); // Always re-enable the button
                }
            }
        };

        worker.execute(); // Start the background task
    }

    // handleAddToWatchlist is unchanged
    private void handleAddToWatchlist() {
        if (currentStock == null) {
            JOptionPane.showMessageDialog(this, "Please search for a stock first.", "No Stock Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        watchlistTableModel.addRow(new Object[]{
                currentStock.getSymbol(),
                currentStock.getPriceFormatted(),
                currentStock.getChangeFormatted(),
                currentStock.getChangePercentFormatted()
        });
    }

    // updateCenterPanel is unchanged
    private void updateCenterPanel(Stock stock) {
        stockSymbolLabel.setFont(new Font("SansSerif", Font.BOLD, 36)); // Reset font size
        stockSymbolLabel.setText(stock.getSymbol());
        stockPriceLabel.setText(stock.getPriceFormatted());
        openPriceLabel.setText(stock.getOpenFormatted());
        highPriceLabel.setText(stock.getHighFormatted());
        lowPriceLabel.setText(stock.getLowFormatted());

        double change = stock.getChange();
        stockChangeLabel.setText(stock.getChangeFormatted());
        stockChangePercentLabel.setText(stock.getChangePercentFormatted());

        if (change > 0) {
            stockChangeLabel.setForeground(new Color(39, 174, 96));
            stockChangePercentLabel.setForeground(new Color(39, 174, 96));
        } else if (change < 0) {
            stockChangeLabel.setForeground(new Color(192, 57, 43));
            stockChangePercentLabel.setForeground(new Color(192, 57, 43));
        } else {
            stockChangeLabel.setForeground(Color.GRAY);
            stockChangePercentLabel.setForeground(Color.GRAY);
        }
    }

    /**
     * Main method - The entry point of the application
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                System.out.println("Nimbus L&F not available, using default.");
            }
            new StockMarketSwingUI().setVisible(true);
        });
    }
}
