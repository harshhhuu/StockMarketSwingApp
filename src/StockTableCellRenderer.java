import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * This class is a custom "renderer" for our JTable cells.
 * It checks the text and sets the color to red or green.
 */
public class StockTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        // Get the default component (a JLabel)
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value instanceof String) {
            String text = (String) value;

            // Check if the text starts with a + or -
            if (text.startsWith("+")) {
                c.setForeground(new Color(39, 174, 96)); // Green
            } else if (text.startsWith("-")) {
                c.setForeground(new Color(192, 57, 43)); // Red
            } else {
                // Use the table's default color for "---" or symbol names
                c.setForeground(table.getForeground());
            }
        } else {
            // Failsafe for unexpected data
            c.setForeground(table.getForeground());
        }

        return c;
    }
}