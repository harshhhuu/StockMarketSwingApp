import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

// --- JFreeChart Imports ---
// These are for creating the chart
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
// This import was missing in your last version
import java.awt.BasicStroke;

/**
 * Main application class.
 * This class creates the main window (JFrame) and organizes all the UI panels.
 */
public class StockMarketSwingUI extends JFrame {

    // --- UI Component Declarations ---
    // These are declared here so all methods in the class can access them.
    private JTextField symbolTextField;
    private JButton searchButton;
    private JButton addToWatchlistButton;
    private JTable watchlistTable;
    private DefaultTableModel watchlistTableModel;
    private JProgressBar loadingSpinner; // Spinner for the details panel

    // (Details Panel components)
    private JLabel stockSymbolLabel;
    private JLabel stockPriceLabel;
    private JLabel stockChangeLabel;
    private JLabel stockChangePercentLabel;
    private JLabel openPriceLabel;
    private JLabel highPriceLabel;
    private JLabel lowPriceLabel;

    // --- Center Panel Components ---
    private JTabbedPane centerTabbedPane; // This will hold "Details" and "Chart"
    private JPanel chartContainerPanel;  // The panel for the "Chart" tab

    // --- Data Members ---
    private Stock currentStock; // Holds the stock currently being displayed
    private final StockDataService stockDataService; // Our helper for API calls
    private static final String WATCHLIST_FILE = "watchlist.txt"; // File to save/load from
    private String currentlyDisplayedChartSymbol = ""; // Tracks which chart is loaded
    private JProgressBar chartLoadingSpinner; // A separate spinner for the chart tab

    /**
     * Constructor: This is where the application is put together.
     */
    public StockMarketSwingUI() {
        // Initialize our API helper class
        this.stockDataService = new StockDataService();

        // --- 1. Main Window Setup ---
        setTitle("Stock Market Viewer (Swing Edition)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Use BorderLayout (North, Center, South)

        // --- 2. Create and Add Panels ---
        JPanel topPanel = createTopPanel(); // Search bar
        centerTabbedPane = createCenterPanel(); // Tabbed pane for details/chart
        JPanel bottomPanel = createBottomPanel(); // Watchlist table

        // Add padding around the whole window
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add the panels to the main window
        add(topPanel, BorderLayout.NORTH);
        add(centerTabbedPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 3. Add Event Listeners ---
        // This method connects the buttons to their functions
        addEventListeners();

        // --- 4. Finalize Window ---
        setSize(900, 700); // Set a larger size to fit the chart
        setLocationRelativeTo(null); // Center on screen

        // --- 5. Load Watchlist & Add Closing Hook ---
        loadWatchlist(); // Load saved symbols from file

        // Add a "hook" to save the watchlist when the user clicks the 'X' button
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWatchlist();
            }
        });
    }

    /**
     * Creates the top panel with the title and search bar.
     */
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

    /**
     * Creates the main center JTabbedPane.
     * This will have two tabs: "Details" and "Chart".
     */
    private JTabbedPane createCenterPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Tab 1: Details Panel ---
        // We call a helper method to create the panel for this tab
        JPanel detailsPanel = createDetailsPanel();
        tabbedPane.addTab("Details", detailsPanel);

        // --- Tab 2: Chart Panel ---
        // This panel is just a container; we'll add the chart to it later
        chartContainerPanel = new JPanel(new BorderLayout());
        chartContainerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add a spinner for chart loading, initially hidden
        chartLoadingSpinner = new JProgressBar();
        chartLoadingSpinner.setIndeterminate(true);
        chartLoadingSpinner.setVisible(false);
        chartContainerPanel.add(chartLoadingSpinner, BorderLayout.CENTER);

        tabbedPane.addTab("Chart", chartContainerPanel);

        return tabbedPane;
    }

    /**
     * Creates the "Details" panel with all the stock price labels.
     */
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding

        // Row 0: Stock Symbol
        stockSymbolLabel = new JLabel("SELECT A STOCK");
        stockSymbolLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; // Span 3 columns
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(stockSymbolLabel, gbc);

        // Row 1: Price
        stockPriceLabel = new JLabel("₹0.00");
        stockPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        gbc.gridy = 1; gbc.gridwidth = 1; // Reset to 1 column
        panel.add(stockPriceLabel, gbc);

        // Row 1: Change and Change %
        JPanel changePanel = new JPanel();
        changePanel.setLayout(new BoxLayout(changePanel, BoxLayout.Y_AXIS)); // Stack vertically
        stockChangeLabel = new JLabel("+0.00");
        stockChangeLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        stockChangePercentLabel = new JLabel("(0.00%)");
        stockChangePercentLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        changePanel.add(stockChangeLabel);
        changePanel.add(stockChangePercentLabel);
        gbc.gridx = 1;
        panel.add(changePanel, gbc);

        // Row 1: Loading Spinner
        gbc.gridx = 2;
        loadingSpinner = new JProgressBar();
        loadingSpinner.setIndeterminate(true);
        loadingSpinner.setVisible(false); // Hide it
        panel.add(loadingSpinner, gbc);

        // Row 2: Open, High, Low
        openPriceLabel = createDetailLabel("0.00");
        highPriceLabel = createDetailLabel("0.00");
        lowPriceLabel = createDetailLabel("0.00");
        JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
        detailsPanel.add(createDetailBox("OPEN", openPriceLabel));
        detailsPanel.add(createDetailBox("HIGH", highPriceLabel));
        detailsPanel.add(createDetailBox("LOW", lowPriceLabel));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(detailsPanel, gbc);
        return panel;
    }

    // Helper method to create the "OPEN", "HIGH", "LOW" boxes
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

    // Helper method to create a formatted label
    private JLabel createDetailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        return label;
    }

    /**
     * Creates the bottom panel with the watchlist JTable.
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Watchlist"));
        String[] columnNames = {"Symbol", "Price", "Change", "Change %"};

        // Create a DefaultTableModel, which lets us add/remove rows
        watchlistTableModel = new DefaultTableModel(columnNames, 0) {
            // Override this method to make the table cells non-editable
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        watchlistTable = new JTable(watchlistTableModel);

        // Apply our custom cell renderer to color the "Change" columns
        StockTableCellRenderer colorRenderer = new StockTableCellRenderer();
        watchlistTable.getColumnModel().getColumn(2).setCellRenderer(colorRenderer);
        watchlistTable.getColumnModel().getColumn(3).setCellRenderer(colorRenderer);

        watchlistTable.setFillsViewportHeight(true);
        watchlistTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        watchlistTable.setRowHeight(25);

        // The table must be put inside a JScrollPane to see column headers
        JScrollPane scrollPane = new JScrollPane(watchlistTable);
        // Set a fixed height for the watchlist panel
        scrollPane.setPreferredSize(new Dimension(0, 250));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Connects all our buttons and listeners to their functions.
     */
    private void addEventListeners() {
        // Search button click
        searchButton.addActionListener(e -> handleSearchAction());
        // Add to Watchlist button click
        addToWatchlistButton.addActionListener(e -> handleAddToWatchlist());

        // Mouse click on the table
        watchlistTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = watchlistTable.rowAtPoint(evt.getPoint());
                if (row >= 0) { // If the click was on a valid row
                    // Get the symbol from the first column
                    String symbol = (String) watchlistTable.getValueAt(row, 0);
                    symbolTextField.setText(symbol); // Put it in the search box
                    handleSearchAction(); // Run a search for it
                }
            }
        });

        // --- Listener for tab changes ---
        // This fires when the user clicks "Details" or "Chart"
        centerTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Check if the newly selected tab is the "Chart" tab (index 1)
                if (centerTabbedPane.getSelectedIndex() == 1) {
                    loadChartData(); // If so, load the chart
                }
            }
        });
    }

    /**
     * This is the main logic for the Search button.
     * It uses a SwingWorker to run the API call on a background thread.
     */
    private void handleSearchAction() {
        String symbol = symbolTextField.getText().trim().toUpperCase();
        if (symbol.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a stock symbol.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- 1. Update UI for Loading ---
        searchButton.setEnabled(false); // Disable button
        loadingSpinner.setVisible(true); // Show spinner
        stockSymbolLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        stockSymbolLabel.setText("Loading...");

        // --- Clear the old chart ---
        chartContainerPanel.removeAll(); // Remove the old chart
        chartContainerPanel.add(chartLoadingSpinner, BorderLayout.CENTER); // Add the spinner
        currentlyDisplayedChartSymbol = ""; // Reset the chart tracker

        // --- 2. Create the SwingWorker ---
        // This runs the network call on a separate thread to prevent UI freeze
        SwingWorker<Stock, Void> worker = new SwingWorker<>() {
            /**
             * This runs on a BACKGROUND thread.
             * No UI updates are allowed here.
             */
            @Override
            protected Stock doInBackground() throws Exception {
                // Call our service to get the data
                return stockDataService.fetchStockData(symbol);
            }

            /**
             * This runs on the UI thread after doInBackground() is finished.
             * We can safely update the UI here.
             */
            @Override
            protected void done() {
                try {
                    // Get the result from the background task
                    currentStock = get();
                    // Update the labels with the new data
                    updateCenterPanel(currentStock);
                    // If this stock is in the watchlist, update its row
                    updateWatchlistRow(currentStock);
                    // Switch back to the "Details" tab
                    centerTabbedPane.setSelectedIndex(0);

                } catch (Exception ex) {
                    // Handle errors (e.g., API key invalid, stock not found)
                    currentStock = null;
                    JOptionPane.showMessageDialog(StockMarketSwingUI.this,
                            ex.getCause().getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                    stockSymbolLabel.setText("SEARCH FAILED");
                } finally {
                    // --- 3. Clean up UI ---
                    // This block runs whether the task succeeded or failed
                    searchButton.setEnabled(true); // Re-enable button
                    loadingSpinner.setVisible(false); // Hide spinner
                }
            }
        };

        // --- 4. Start the worker ---
        worker.execute();
    }

    /**
     * This logic is called when the user clicks the "Chart" tab.
     * It uses a separate SwingWorker to load the chart data.
     */
    private void loadChartData() {
        // If no stock is selected, show a message.
        if (currentStock == null) {
            chartContainerPanel.removeAll();
            chartContainerPanel.add(new JLabel("Please search for a stock to see its chart."));
            return;
        }

        // If the chart for this stock is already displayed, don't reload it.
        if (currentStock.getSymbol().equals(currentlyDisplayedChartSymbol)) {
            return;
        }

        // --- 1. Update UI for Loading ---
        chartLoadingSpinner.setVisible(true);

        // --- 2. Create the SwingWorker for the chart ---
        SwingWorker<XYDataset, Void> chartWorker = new SwingWorker<>() {
            /**
             * This runs on a BACKGROUND thread.
             */
            @Override
            protected XYDataset doInBackground() throws Exception {
                // Call our service to get the historical data
                return stockDataService.fetchChartData(currentStock.getSymbol());
            }

            /**
             * This runs on the UI thread.
             */
            @Override
            protected void done() {
                try {
                    // Get the data from the background task
                    XYDataset dataset = get();

                    // Create the chart using our helper method
                    JFreeChart chart = createChart(dataset, currentStock.getSymbol());
                    // Remember which chart we've loaded
                    currentlyDisplayedChartSymbol = currentStock.getSymbol();

                    // Create a ChartPanel, which is a Swing component
                    ChartPanel chartPanel = new ChartPanel(chart);
                    chartPanel.setMouseWheelEnabled(true); // Enable mouse wheel zoom

                    // --- 3. Add the chart to the UI ---
                    chartContainerPanel.removeAll(); // Remove the spinner
                    chartContainerPanel.add(chartPanel, BorderLayout.CENTER);

                } catch (Exception ex) {
                    // Handle errors
                    chartContainerPanel.removeAll();
                    chartContainerPanel.add(new JLabel("Error loading chart: " + ex.getCause().getMessage()));
                } finally {
                    // --- 4. Clean up UI ---
                    chartLoadingSpinner.setVisible(false); // Hide spinner
                    chartContainerPanel.revalidate(); // Tell the panel to refresh
                    chartContainerPanel.repaint();
                }
            }
        };

        // --- 5. Start the worker ---
        chartWorker.execute();
    }

    /**
     * Helper method to create and style the JFreeChart object.
     */
    private JFreeChart createChart(XYDataset dataset, String symbol) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                symbol + " - Last 100 Days", // Chart title
                "Date",                      // X-Axis label
                "Price (₹)",                 // Y-Axis label
                dataset
        );

        // --- Professional Polish ---
        XYPlot plot = chart.getXYPlot();

        // Set colors
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // Customize the line
        var renderer = new XYLineAndShapeRenderer(true, false); // Lines, no shapes
        renderer.setSeriesPaint(0, new Color(0, 102, 204)); // Blue color
        renderer.setSeriesStroke(0, new BasicStroke(2.0f)); // Thicker line

        plot.setRenderer(renderer);
        chart.setBackgroundPaint(this.getBackground()); // Match window background

        return chart;
    }

    /**
     * This is the logic for the "Add to Watchlist" button.
     * This was one of the methods you were missing.
     */
    private void handleAddToWatchlist() {
        if (currentStock == null) {
            JOptionPane.showMessageDialog(this, "Please search for a stock first.", "No Stock Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if symbol is already in the table to prevent duplicates
        boolean exists = false;
        for (int i = 0; i < watchlistTableModel.getRowCount(); i++) {
            if (watchlistTableModel.getValueAt(i, 0).equals(currentStock.getSymbol())) {
                exists = true;
                break;
            }
        }

        if (exists) {
            JOptionPane.showMessageDialog(this, "This stock is already in your watchlist.", "Duplicate", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Add the new row to our table model
            watchlistTableModel.addRow(new Object[]{
                    currentStock.getSymbol(),
                    currentStock.getPriceFormatted(),
                    currentStock.getChangeFormatted(),
                    currentStock.getChangePercentFormatted()
            });
        }
    }

    /**
     * Updates an existing row in the watchlist table with new data.
     * This was one of the methods you were missing.
     */
    private void updateWatchlistRow(Stock stock) {
        // Loop through the table
        for (int i = 0; i < watchlistTableModel.getRowCount(); i++) {
            if (watchlistTableModel.getValueAt(i, 0).equals(stock.getSymbol())) {
                // Found it. Update the values
                watchlistTableModel.setValueAt(stock.getPriceFormatted(), i, 1);
                watchlistTableModel.setValueAt(stock.getChangeFormatted(), i, 2);
                watchlistTableModel.setValueAt(stock.getChangePercentFormatted(), i, 3);
                break; // Stop looping
            }
        }
    }

    /**
     * Loads the watchlist symbols from watchlist.txt at startup.
     * This was one of the methods you were missing.
     */
    private void loadWatchlist() {
        File file = new File(WATCHLIST_FILE);
        if (!file.exists()) {
            return; // No file, nothing to load
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String symbol;
            while ((symbol = reader.readLine()) != null) {
                // Add a placeholder row. The user can click it to load live data.
                watchlistTableModel.addRow(new Object[]{
                        symbol, "Click to load", "---", "---"
                });
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading watchlist: ".concat(e.getMessage()), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves all symbols from the table to watchlist.txt on exit.
     * This was one of the methods you were missing.
     */
    private void saveWatchlist() {
        List<String> symbols = new ArrayList<>();
        // Get all symbols from the table
        for (int i = 0; i < watchlistTableModel.getRowCount(); i++) {
            symbols.add((String) watchlistTableModel.getValueAt(i, 0));
        }

        // Write them to the file, one per line
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WATCHLIST_FILE))) {
            for (String symbol : symbols) {
                writer.write(symbol);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving watchlist: ".concat(e.getMessage()), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Updates all the labels in the "Details" panel.
     * This was one of the methods you were missing.
     */
    private void updateCenterPanel(Stock stock) {
        stockSymbolLabel.setFont(new Font("SansSerif", Font.BOLD, 36)); // Reset font size
        stockSymbolLabel.setText(stock.getSymbol());
        stockPriceLabel.setText(stock.getPriceFormatted());
        openPriceLabel.setText(stock.getOpenFormatted());
        highPriceLabel.setText(stock.getHighFormatted());
        lowPriceLabel.setText(stock.getLowFormatted());

        // Set color based on positive or negative change
        double change = stock.getChange();
        stockChangeLabel.setText(stock.getChangeFormatted());
        stockChangePercentLabel.setText(stock.getChangePercentFormatted());

        if (change > 0) {
            stockChangeLabel.setForeground(new Color(39, 174, 96)); // Green
            stockChangePercentLabel.setForeground(new Color(39, 174, 96));
        } else if (change < 0) {
            stockChangeLabel.setForeground(new Color(192, 57, 43)); // Red
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
        // Runs the UI on a special thread (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            try {
                // Set a modern "Look and Feel"
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                System.out.println("Nimbus L&F not available, using default.");
            }
            new StockMarketSwingUI().setVisible(true); // Create and show the window
        });
    }
}