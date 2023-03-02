/* Name: Justice Smith
Course: CNT 4714 – Spring 2023
Assignment title: Project 1 – Event-driven Enterprise Simulation
Date: Sunday January 29, 2023
*/
package src.NileGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NileGUI implements ActionListener {
    private static final String INVENTORY = "inventory.txt";
    private static final String LOGFILE = "transaction.txt";
    private static final ArrayList<String> cart = new ArrayList<>();
    private static int itemNumber = 1;
    private static int numItemEntriesInCart = 0;
    private static double orderSubtotal = 0.00;
    JFrame frame;
    JPanel inputPanel, buttonPanel;
    JButton findItem, purchase, viewOrder, completeOrder, startOrder, close;
    JLabel idLabel, quantityLabel, detailsLabel, subtotalLabel;
    JTextField idField, quantityField, detailsField, subtotalField;

    public NileGUI() {
        frame = new JFrame("NileDotCom");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        addComponentsToInputPanel();
        frame.add(inputPanel);

        addComponentsToButtonPanel();
        frame.add(buttonPanel);

        frame.pack();
        frame.setVisible(true);
    }

    private static void runGUI() {
        new NileGUI();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(NileGUI::runGUI);
    }

    private void addComponentsToButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 2, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        findItem = new JButton("Find Item #" + itemNumber);
        findItem.addActionListener(this);
        buttonPanel.add(findItem);

        purchase = new JButton("Purchase Item #" + itemNumber);
        purchase.setEnabled(false);
        purchase.addActionListener(this);
        buttonPanel.add(purchase);

        viewOrder = new JButton("View Current Order");
        viewOrder.setEnabled(false);
        viewOrder.addActionListener(this);
        buttonPanel.add(viewOrder);

        completeOrder = new JButton("Complete Order - Check Out");
        completeOrder.setEnabled(false);
        completeOrder.addActionListener(this);
        buttonPanel.add(completeOrder);

        startOrder = new JButton("Start New Order");
        startOrder.addActionListener(this);
        buttonPanel.add(startOrder);

        close = new JButton("Exit (Close App)");
        close.addActionListener(this);
        buttonPanel.add(close);
    }

    private void addComponentsToInputPanel() {
        inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        idLabel = new JLabel("Enter item ID for Item #" + itemNumber + ":", SwingConstants.RIGHT);
        inputPanel.add(idLabel);

        idField = new JTextField(20);
        inputPanel.add(idField);

        quantityLabel = new JLabel("Enter quantity for Item #" + itemNumber + ":", SwingConstants.RIGHT);
        inputPanel.add(quantityLabel);

        quantityField = new JTextField(20);
        inputPanel.add(quantityField);

        detailsLabel = new JLabel("Details for Item #" + itemNumber + ":", SwingConstants.RIGHT);
        inputPanel.add(detailsLabel);

        detailsField = new JTextField(20);
        detailsField.setEditable(false);
        inputPanel.add(detailsField);

        subtotalLabel = new JLabel("Order subtotal for " + numItemEntriesInCart + " item(s):", SwingConstants.RIGHT);
        inputPanel.add(subtotalLabel);

        subtotalField = new JTextField(20);
        subtotalField.setEditable(false);
        inputPanel.add(subtotalField);
    }

    private boolean inputsValid(String itemId, String quantity) {
        if (itemId.isEmpty()) {
            return false;
        } else if (quantity.isEmpty()) {
            return false;
        } else return Integer.parseInt(quantity) >= 1;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == findItem) {
            String itemId = idField.getText();
            String quantity = quantityField.getText();

            if (inputsValid(itemId, quantity)) {
                String[] details = findItem(itemId);

                if (details == null) { // item not found
                    new ItemNotFoundDialog(frame, itemId);
                    idField.setText("");
                    quantityField.setText("");
                    return;
                }

                boolean inStock = Boolean.parseBoolean(details[2]);
                if (!inStock) {
                    new ItemOutOfStockDialog(frame);
                    idField.setText("");
                    quantityField.setText("");
                    return;
                }

                // In stock
                idField.setEditable(false);
                quantityField.setEditable(false);
                findItem.setEnabled(false);
                purchase.setEnabled(true);

                double price = Double.parseDouble(details[3]);
                int numToPurchase = Integer.parseInt(quantity);
                int discount = getDiscount(Integer.parseInt(quantity));
                double subtotal = (price * numToPurchase * (1.0 - ((double) discount / 100.0)));
                String itemDetails =
                        details[0]
                                + " "
                                + details[1]
                                + " $"
                                + details[3]
                                + " " + numToPurchase
                                + " " + discount
                                + "% $"
                                + String.format("%.2f", subtotal);
                detailsLabel.setText("Details for Item #" + itemNumber + ":");
                detailsField.setText(itemDetails);
            }
        } else if (e.getSource() == purchase) {
            numItemEntriesInCart++;

            if (numItemEntriesInCart == 1) {
                viewOrder.setEnabled(true);
                completeOrder.setEnabled(true);
            }
            subtotalLabel.setText("Order subtotal for " + numItemEntriesInCart + " item(s):");

            String details = detailsField.getText();
            cart.add(details);
            // extract subtotal from end of details string after last space
            // Example:
            //      '14 "Stanley #2 Philips Screwdriver" $6.95 1 0% $6.95'
            // would grab the substring beginning at last dollar sign and on, then trim surrounding whitespace, if any.
            double subtotal = Double.parseDouble(details.substring(details.lastIndexOf('$') + 1).trim());
            orderSubtotal += subtotal;
            subtotalField.setText("$" + String.format("%.2f", orderSubtotal));

            itemNumber++;

            idLabel.setText("Enter item ID for Item #" + itemNumber + ":");
            quantityLabel.setText("Enter quantity for Item #" + itemNumber + ":");
            idField.setText("");
            quantityField.setText("");
            idField.setEditable(true);
            quantityField.setEditable(true);
            findItem.setEnabled(true);
            purchase.setEnabled(false);
            findItem.setText("Find Item #" + itemNumber);
            purchase.setText("Purchase Item #" + itemNumber);

            new PurchaseDialog(frame, itemNumber - 1);
        } else if (e.getSource() == viewOrder) {
            for (String elem : cart) {
                System.out.println(elem);
            }
            new CartDialog(frame, cart);
        } else if (e.getSource() == completeOrder) {
            new InvoiceDialog(frame, cart, orderSubtotal);

            // output to transaction log TODO
            recordTransaction(cart);

            idField.setEditable(false);
            quantityField.setEditable(false);
            findItem.setEnabled(false);
            purchase.setEnabled(false);
            completeOrder.setEnabled(false);
        } else if (e.getSource() == startOrder) {
            // reset counts
            itemNumber = 1;
            numItemEntriesInCart = 0;
            orderSubtotal = 0.00;
            cart.clear();

            // reset gui

            // Labels and inputs
            idLabel.setText("Enter item ID for Item #" + itemNumber + ":");
            idField.setEditable(true);
            idField.setText("");

            quantityLabel.setText("Enter quantity for Item #" + itemNumber + ":");
            quantityField.setEditable(true);
            quantityField.setText("");

            detailsLabel.setText("Details for Item #" + itemNumber + ":");
            detailsField.setText("");

            subtotalLabel.setText("Order subtotal for " + numItemEntriesInCart + " item(s):");
            subtotalField.setText("");

            // Buttons
            findItem.setText("Find Item #" + itemNumber);
            findItem.setEnabled(true);

            purchase.setText("Purchase Item #" + itemNumber);
            purchase.setEnabled(false);

            viewOrder.setEnabled(false);

            completeOrder.setEnabled(false);
        } else if (e.getSource() == close) {
            System.exit(0);
        }
    }

    private void recordTransaction(ArrayList<String> cart) {
        LocalDateTime checkoutTime = LocalDateTime.now();
        String time = checkoutTime.format(DateTimeFormatter.ofPattern("MMdduuuuHHmm"));
        String date = checkoutTime.format(DateTimeFormatter.ofPattern("M/dd/uu, K:mm:ss a zzz").withZone(ZoneId.systemDefault()));
        System.out.println(time);
        System.out.println(date);
        System.out.println(cart.get(0));

        String regex = "(\\w+) (\".+\") (.\\d+.\\d+) (\\d+) (\\d+.) (.\\d+.\\d+)";
        Matcher m;
        StringBuilder logItem;
        Pattern p = Pattern.compile(regex);

        for (String elem : cart) {
            m = p.matcher(elem);
            if (m.matches()) {
                logItem = new StringBuilder(
                        time
                                + ", "
                                + m.group(1)
                                + ", "
                                + m.group(2)
                                + ", "
                                + m.group(3)
                                + ", "
                                + m.group(4)
                                + ", "
                                + m.group(5)
                                + ", "
                                + m.group(6)
                                + ", "
                                + date
                                + "\n");

                appendToTransactionFile(LOGFILE, logItem.toString());
            }
        }
    }

    private void appendToTransactionFile(String filename, String line) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(filename, true))) {
            out.write(line);
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    private int getDiscount(int numberOfItems) {
        if (numberOfItems <= 4) {
            return 0;
        } else if (numberOfItems <= 9) {
            return 10;
        } else if (numberOfItems <= 14) {
            return 15;
        } else return 20;
    }

    private String[] findItem(String id) {
        String[] parts;
        String itemId;
        try (Scanner input = new Scanner(new File(NileGUI.INVENTORY))) {
            while (input.hasNextLine()) {
                parts = input.nextLine().split(",");
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }
                itemId = parts[0];
                if (itemId.equals(id)) {
                    return parts;
                }
            }
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
        return null;
    }
}

class ItemNotFoundDialog extends JDialog implements ActionListener {
    JPanel mainPanel;
    JPanel buttonPanel;
    JLabel label;
    JButton button;

    ItemNotFoundDialog(Frame container, String itemId) {
        super(container, "Nile Dot Com - Error");
        setResizable(false);
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel("Item ID " + itemId + " not found in file");
        mainPanel.add(label);

        buttonPanel = new JPanel();
        button = new JButton("OK");
        button.setSize(40, 40);
        button.addActionListener(this);
        buttonPanel.add(button);
        mainPanel.add(buttonPanel);

        add(mainPanel);
        pack();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dispose();
    }
}

class ItemOutOfStockDialog extends JDialog implements ActionListener {
    JPanel mainPanel;
    JPanel buttonPanel;
    JLabel label;
    JButton button;

    ItemOutOfStockDialog(Frame container) {
        super(container, "Nile Dot Com - Error");
        setResizable(false);
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel("Sorry... that item is out of stock, please try another item");
        mainPanel.add(label);

        buttonPanel = new JPanel();
        button = new JButton("OK");
        button.setSize(40, 40);
        button.addActionListener(this);
        buttonPanel.add(button);
        mainPanel.add(buttonPanel);

        add(mainPanel);
        pack();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dispose();
    }
}

class CartDialog extends JDialog implements ActionListener {
    JPanel mainPanel;
    JPanel buttonPanel;
    JButton button;

    CartDialog(Frame container, ArrayList<String> cart) {
        super(container, "Nile Dot Com - Current Shopping Cart Status");
        setResizable(false);
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(cart.size() + 1, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (int i = 1; i <= cart.size(); i++) {
            mainPanel.add(new JLabel(i + ". " + cart.get(i - 1)));
        }

        buttonPanel = new JPanel();
        button = new JButton("OK");
        button.setSize(40, 40);
        button.addActionListener(this);
        buttonPanel.add(button);
        mainPanel.add(buttonPanel);

        add(mainPanel);
        pack();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dispose();
    }
}

class PurchaseDialog extends JDialog implements ActionListener {
    JPanel mainPanel;
    JPanel buttonPanel;
    JLabel label;
    JButton button;

    PurchaseDialog(Frame container, int itemNumber) {
        super(container, "Nile Dot Com - Item Confirmed");
        setResizable(false);
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel("Item #" + itemNumber + " accepted. Added to your cart.");
        mainPanel.add(label);

        buttonPanel = new JPanel();
        button = new JButton("OK");
        button.setSize(40, 40);
        button.addActionListener(this);
        buttonPanel.add(button);
        mainPanel.add(buttonPanel);

        add(mainPanel);
        pack();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dispose();
    }
}

class InvoiceDialog extends JDialog implements ActionListener {
    private static final double TAX_AMOUNT = 0.06;
    JPanel mainPanel;
    JPanel buttonPanel;
    JButton button;

    InvoiceDialog(Frame container, ArrayList<String> cart, double orderSubtotal) {
        super(container, "Nile Dot Com - FINAL INVOICE");
        setResizable(false);
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3 + cart.size() + 6, 1));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        mainPanel.add(new JLabel("Date: " + getFormattedDateTime(LocalDateTime.now())));
        mainPanel.add(new JLabel("Number of line items: " + cart.size()));
        mainPanel.add(new JLabel("Item# / ID / Title / Price / Qty / Disc % / Subtotal:"));
        for (int i = 1; i <= cart.size(); i++) {
            mainPanel.add(new JLabel(i + ". " + cart.get(i - 1)));
        }
        mainPanel.add(new JLabel("Order subtotal: $" + String.format("%.2f", orderSubtotal)));
        mainPanel.add(new JLabel("Tax rate: " + (TAX_AMOUNT * 100) + "%"));
        mainPanel.add(new JLabel("Tax amount: $" + String.format("%.2f", (orderSubtotal * TAX_AMOUNT))));
        mainPanel.add(new JLabel("ORDER TOTAL: $" + String.format("%.2f", (orderSubtotal + (orderSubtotal * TAX_AMOUNT)))));

        mainPanel.add(new JLabel("Thanks for shopping at Nile Dot Com!"));

        buttonPanel = new JPanel();
        button = new JButton("OK");
        button.setSize(40, 40);
        button.addActionListener(this);
        buttonPanel.add(button);
        mainPanel.add(buttonPanel);

        add(mainPanel);
        pack();
        setVisible(true);
    }

    private String getFormattedDateTime(LocalDateTime now) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/dd/uu K:mm:ss a zzz").withZone(ZoneId.systemDefault());
        return now.format(formatter);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dispose();
    }
}

