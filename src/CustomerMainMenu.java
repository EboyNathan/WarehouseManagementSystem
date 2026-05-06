import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerMainMenu {
    private static final String SHOP_VIEW = "shop";
    private static final String CART_VIEW = "cart";
    private static final String PAYMENT_VIEW = "payment";
    private static final String PROFILE_VIEW = "profile";

    private final DataStorage.User currentUser;
    private final Runnable logoutHandler;
    private final Map<Product, Integer> cart = new LinkedHashMap<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    private final List<Product> products = new java.util.ArrayList<>();
    private final CardLayout contentLayout = new CardLayout();
    private static final int QR_SIZE = 280;

    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel cardsPanel;
    private JPanel cartItemsPanel;
    private JLabel cartBadgeLabel;
    private JLabel userLabel;
    private JComboBox<String> categoryBox;
    private JTextField searchField;

    private JTextField paymentNameField;
    private JTextField paymentAddressField;
    private JTextField paymentPhoneField;
    private JComboBox<String> paymentOptionBox;
    private JLabel paymentSummaryLabel;
    private JLabel paymentDeliveryLabel;
    private Map<Product, Integer> paymentItems = new LinkedHashMap<>();
    private boolean paymentIsPreOrder;
    private JTextField profileFullNameField;
    private JTextField profileAddressField;
    private JTextField profilePhoneField;
    private JTable profileHistoryTable;

    public CustomerMainMenu(DataStorage.User currentUser, Runnable logoutHandler) {
        this.currentUser = currentUser;
        this.logoutHandler = logoutHandler;
        loadProductsFromInventory();
        buildUi();
    }

    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(98, 140, 224));
        mainPanel.add(buildHeader(), BorderLayout.NORTH);

        contentPanel = new JPanel(contentLayout);
        contentPanel.add(buildShopPanel(), SHOP_VIEW);
        contentPanel.add(buildCartPanel(), CART_VIEW);
        contentPanel.add(buildPaymentPanel(), PAYMENT_VIEW);
        contentPanel.add(buildProfilePanel(), PROFILE_VIEW);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        showView(SHOP_VIEW);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));
        header.setBackground(new Color(41, 49, 63));

        JLabel brand = new JLabel("Warehouse Shop");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        categoryBox = new JComboBox<>(new String[]{
                "All",
                "Electronics",
                "Office supplies",
                "tools & equipment",
                "Consumables",
                "Spare Parts",
                "Finished goods"
        });
        searchField = new JTextField(16);
        JButton searchButton = new JButton("Search");
        styleHeaderButton(searchButton, new Color(25, 135, 84));
        searchButton.addActionListener(e -> refreshProductCards());
        categoryBox.addActionListener(e -> refreshProductCards());
        left.add(brand);
        left.add(categoryBox);
        left.add(searchField);
        left.add(searchButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        userLabel = new JLabel(currentUser.fullName.isBlank() ? currentUser.username : currentUser.fullName);
        userLabel.setForeground(new Color(220, 230, 244));
        userLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showProfilePanel();
            }
        });

        cartBadgeLabel = new JLabel("Cart (0)");
        cartBadgeLabel.setForeground(Color.WHITE);
        JButton cartButton = new JButton("View Cart");
        JButton profileButton = new JButton("Profile");
        JButton logoutButton = new JButton("Logout");
        styleHeaderButton(cartButton, new Color(37, 99, 235));
        styleHeaderButton(profileButton, new Color(108, 117, 125));
        styleHeaderButton(logoutButton, new Color(220, 53, 69));
        cartButton.addActionListener(e -> {
            refreshCartPanel();
            showView(CART_VIEW);
        });
        profileButton.addActionListener(e -> showProfilePanel());
        logoutButton.addActionListener(e -> {
            if (logoutHandler != null) {
                logoutHandler.run();
            }
        });

        right.add(userLabel);
        right.add(cartBadgeLabel);
        right.add(cartButton);
        right.add(profileButton);
        right.add(logoutButton);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildShopPanel() {
        JPanel shopPanel = new JPanel(new BorderLayout());
        shopPanel.setOpaque(false);

        String displayName = currentUser.fullName.isBlank() ? currentUser.username : currentUser.fullName;
        JLabel title = new JLabel(displayName + ", welcome to RAMJA'S Warehouse");
        title.setFont(new Font("Segoe UI", Font.BOLD, 48));
        title.setForeground(new Color(18, 56, 124));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(24, 0, 18, 0));
        shopPanel.add(title, BorderLayout.NORTH);

        cardsPanel = new JPanel(new GridLayout(0, 4, 12, 12));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(10, 20, 14, 20));
        refreshProductCards();

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        shopPanel.add(scrollPane, BorderLayout.CENTER);
        return shopPanel;
    }

    private JPanel buildCartPanel() {
        JPanel cartPanel = new JPanel(new BorderLayout(0, 12));
        cartPanel.setOpaque(false);
        cartPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel title = new JLabel("Your Cart");
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setForeground(new Color(18, 56, 124));
        cartPanel.add(title, BorderLayout.NORTH);

        cartItemsPanel = new JPanel();
        cartItemsPanel.setLayout(new BoxLayout(cartItemsPanel, BoxLayout.Y_AXIS));
        cartItemsPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(cartItemsPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        cartPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        JButton continueShopping = new JButton("Continue Shopping");
        JButton checkout = new JButton("Proceed to Payment");
        styleActionButton(continueShopping, new Color(108, 117, 125));
        styleActionButton(checkout, new Color(46, 125, 50));
        continueShopping.setPreferredSize(new Dimension(190, 38));
        checkout.setPreferredSize(new Dimension(240, 38));
        checkout.setMinimumSize(new Dimension(240, 38));
        continueShopping.setFont(new Font("Segoe UI", Font.BOLD, 14));
        checkout.setFont(new Font("Segoe UI", Font.BOLD, 14));
        continueShopping.addActionListener(e -> showView(SHOP_VIEW));
        checkout.addActionListener(e -> {
            Map<Product, Integer> items = getNonZeroCartItems();
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "Cart is empty.", "Cart", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openPaymentPanel(items, false);
        });
        footer.add(continueShopping);
        footer.add(checkout);
        cartPanel.add(footer, BorderLayout.SOUTH);
        return cartPanel;
    }

    private JPanel buildPaymentPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setPreferredSize(new Dimension(520, 520));
        panel.setBackground(new Color(245, 246, 248));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 222)),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel title = new JLabel("Payment");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(title);

        paymentSummaryLabel = new JLabel();
        paymentDeliveryLabel = new JLabel();
        panel.add(paymentSummaryLabel);
        panel.add(paymentDeliveryLabel);

        panel.add(new JLabel("Name"));
        paymentNameField = new JTextField(currentUser.fullName);
        panel.add(paymentNameField);

        panel.add(new JLabel("Address"));
        paymentAddressField = new JTextField(currentUser.address);
        panel.add(paymentAddressField);

        panel.add(new JLabel("Phone Number"));
        paymentPhoneField = new JTextField(currentUser.phoneNumber);
        panel.add(paymentPhoneField);

        panel.add(new JLabel("Payment Option"));
        paymentOptionBox = new JComboBox<>(new String[]{"QR Code", "Cash"});
        panel.add(paymentOptionBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton backButton = new JButton("Back");
        JButton checkoutButton = new JButton("Check Out");
        styleActionButton(backButton, new Color(108, 117, 125));
        styleActionButton(checkoutButton, new Color(25, 118, 210));
        backButton.addActionListener(e -> showView(CART_VIEW));
        checkoutButton.addActionListener(e -> confirmCheckout());
        buttons.add(backButton);
        buttons.add(checkoutButton);
        panel.add(new JLabel());
        panel.add(buttons);

        wrapper.add(panel);
        return wrapper;
    }

    private JPanel buildProfilePanel() {
        JPanel profilePanel = new JPanel(new BorderLayout(0, 12));
        profilePanel.setOpaque(false);
        profilePanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel title = new JLabel("Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setForeground(new Color(18, 56, 124));
        profilePanel.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);

        JPanel formPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        formPanel.setBackground(new Color(245, 246, 248));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 222)),
                new EmptyBorder(14, 14, 14, 14)
        ));

        formPanel.add(label("Full Name", Font.BOLD, 14, new Color(33, 37, 41)));
        profileFullNameField = new JTextField();
        formPanel.add(profileFullNameField);
        formPanel.add(label("Address", Font.BOLD, 14, new Color(33, 37, 41)));
        profileAddressField = new JTextField();
        formPanel.add(profileAddressField);
        formPanel.add(label("Phone Number", Font.BOLD, 14, new Color(33, 37, 41)));
        profilePhoneField = new JTextField();
        formPanel.add(profilePhoneField);

        JPanel formActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        formActions.setOpaque(false);
        JButton saveProfile = new JButton("Save Profile");
        JButton backToShop = new JButton("Back to Shop");
        styleActionButton(saveProfile, new Color(25, 118, 210));
        styleActionButton(backToShop, new Color(108, 117, 125));
        saveProfile.addActionListener(e -> saveProfileChanges());
        backToShop.addActionListener(e -> showView(SHOP_VIEW));
        formActions.add(saveProfile);
        formActions.add(backToShop);
        formPanel.add(new JLabel());
        formPanel.add(formActions);

        JPanel historyPanel = new JPanel(new BorderLayout(0, 8));
        historyPanel.setBackground(new Color(245, 246, 248));
        historyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 222)),
                new EmptyBorder(14, 14, 14, 14)
        ));
        historyPanel.add(label("Purchase History", Font.BOLD, 18, new Color(33, 37, 41)), BorderLayout.NORTH);

        profileHistoryTable = new JTable();
        profileHistoryTable.setRowHeight(24);
        profileHistoryTable.setModel(new DefaultTableModel(new Object[][]{}, new Object[]{"Order ID", "Item", "Qty", "Status", "Delivery ETA"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        historyPanel.add(new JScrollPane(profileHistoryTable), BorderLayout.CENTER);

        body.add(formPanel);
        body.add(historyPanel);
        profilePanel.add(body, BorderLayout.CENTER);
        return profilePanel;
    }

    private JPanel createProductCard(Product product) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(new Color(245, 246, 248));
        card.setPreferredSize(new Dimension(280, 300));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 222)),
                new EmptyBorder(8, 8, 8, 8)
        ));

        JLabel image = new JLabel(loadProductImage(product.imageUrl, product.name), SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(220, 125));
        card.add(image, BorderLayout.NORTH);

        JPanel meta = new JPanel(new GridLayout(0, 1));
        meta.setOpaque(false);
        meta.add(label(product.name, Font.BOLD, 16, new Color(33, 37, 41)));
        meta.add(label(currencyFormat.format(product.price), Font.PLAIN, 14, new Color(33, 37, 41)));
        meta.add(label("Stock: " + product.stock, Font.PLAIN, 13, new Color(73, 80, 87)));
        card.add(meta, BorderLayout.CENTER);

        int maxQty = product.stock > 0 ? product.stock : 9999;
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxQty, 1));
        qtySpinner.setPreferredSize(new Dimension(58, 30));
        JComponent editor = qtySpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JTextField textField = defaultEditor.getTextField();
            textField.setHorizontalAlignment(SwingConstants.CENTER);
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        }

        JButton minus = new JButton("-");
        JButton plus = new JButton("+");
        styleQuantityButton(minus);
        styleQuantityButton(plus);
        minus.addActionListener(e -> {
            int qty = (Integer) qtySpinner.getValue();
            if (qty > 1) {
                qtySpinner.setValue(qty - 1);
            }
        });
        plus.addActionListener(e -> {
            int qty = (Integer) qtySpinner.getValue();
            if (qty < maxQty) {
                qtySpinner.setValue(qty + 1);
            }
        });

        JButton addToCart = new JButton(product.stock > 0 ? "Add to Cart" : "Out of Stock");
        JButton buyNow = new JButton(product.stock > 0 ? "Buy Now" : "Unavailable");
        styleActionButton(addToCart, new Color(25, 118, 210));
        styleActionButton(buyNow, product.stock > 0 ? new Color(46, 125, 50) : new Color(107, 114, 128));
        if (product.stock <= 0) {
            addToCart.setEnabled(false);
            buyNow.setEnabled(false);
            qtySpinner.setEnabled(false);
            minus.setEnabled(false);
            plus.setEnabled(false);
        }

        addToCart.addActionListener(e -> {
            int qty = (Integer) qtySpinner.getValue();
            if (product.stock <= 0) {
                JOptionPane.showMessageDialog(mainPanel, "This item is currently out of stock.", "Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            cart.put(product, qty);
            updateCartBadge();
            JOptionPane.showMessageDialog(mainPanel, product.name + " added to cart.", "Cart", JOptionPane.INFORMATION_MESSAGE);
        });
        buyNow.addActionListener(e -> {
            int qty = (Integer) qtySpinner.getValue();
            Map<Product, Integer> singleItem = new LinkedHashMap<>();
            singleItem.put(product, qty);
            openPaymentPanel(singleItem, false);
        });

        JPanel controls = new JPanel(new BorderLayout(0, 8));
        controls.setOpaque(false);
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.add(minus);
        qtyPanel.add(qtySpinner);
        qtyPanel.add(plus);
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        actions.add(addToCart);
        actions.add(buyNow);
        controls.add(qtyPanel, BorderLayout.NORTH);
        controls.add(actions, BorderLayout.CENTER);
        card.add(controls, BorderLayout.SOUTH);
        return card;
    }

    private void refreshProductCards() {
        cardsPanel.removeAll();
        String category = (String) categoryBox.getSelectedItem();
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        int shownCount = 0;

        for (Product product : products) {
            boolean matchCategory = "All".equals(category) || product.category.equalsIgnoreCase(category);
            boolean matchSearch = keyword.isEmpty() || product.name.toLowerCase().contains(keyword);
            if (matchCategory && matchSearch) {
                cardsPanel.add(createProductCard(product));
                shownCount++;
            }
        }
        if (shownCount == 0) {
            JLabel empty = new JLabel("No products found.", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            empty.setForeground(new Color(55, 65, 81));
            cardsPanel.add(empty);
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void refreshCartPanel() {
        cartItemsPanel.removeAll();
        double subtotal = 0;
        Map<Product, Integer> items = getNonZeroCartItems();

        if (items.isEmpty()) {
            JLabel empty = label("Your cart is empty.", Font.PLAIN, 18, new Color(18, 56, 124));
            empty.setBorder(new EmptyBorder(16, 12, 0, 0));
            cartItemsPanel.add(empty);
        } else {
            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product product = entry.getKey();
                int qty = entry.getValue();
                subtotal += product.price * qty;
                cartItemsPanel.add(createCartRow(product, qty));
                cartItemsPanel.add(Box.createVerticalStrut(10));
            }
            cartItemsPanel.add(Box.createVerticalStrut(8));
            cartItemsPanel.add(createOrderSummaryPanel(items, subtotal));
        }

        cartItemsPanel.revalidate();
        cartItemsPanel.repaint();
    }

    private JPanel createCartRow(Product product, int qty) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(new Color(248, 250, 252));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(10, 10, 10, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel itemInfo = new JLabel(product.name + " | Price: " + currencyFormat.format(product.price));
        itemInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel qtyLabel = new JLabel(String.valueOf(qty), SwingConstants.CENTER);
        qtyLabel.setPreferredSize(new Dimension(36, 26));
        JButton minus = new JButton("-");
        JButton plus = new JButton("+");
        styleQuantityButton(minus);
        styleQuantityButton(plus);

        minus.addActionListener(e -> {
            int newQty = Math.max(0, cart.get(product) - 1);
            cart.put(product, newQty);
            updateCartBadge();
            refreshCartPanel();
        });
        plus.addActionListener(e -> {
            cart.put(product, cart.get(product) + 1);
            updateCartBadge();
            refreshCartPanel();
        });

        JLabel lineTotal = new JLabel("Total: " + currencyFormat.format(product.price * qty));
        lineTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.add(minus);
        qtyPanel.add(qtyLabel);
        qtyPanel.add(plus);

        row.add(itemInfo, BorderLayout.WEST);
        row.add(qtyPanel, BorderLayout.CENTER);
        row.add(lineTotal, BorderLayout.EAST);
        return row;
    }

    private JPanel createOrderSummaryPanel(Map<Product, Integer> items, double subtotal) {
        final double deliveryFee = subtotal > 0 ? 49.00 : 0.0;
        final double total = subtotal + deliveryFee;

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(241, 245, 249));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JLabel title = new JLabel("Order Summary");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(15, 23, 42));
        panel.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product product = entry.getKey();
            int qty = entry.getValue();
            double lineTotal = product.price * qty;
            JLabel line = new JLabel(product.name + " x" + qty + "  -  " + currencyFormat.format(lineTotal));
            line.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            line.setForeground(new Color(51, 65, 85));
            body.add(line);
            body.add(Box.createVerticalStrut(4));
        }

        body.add(Box.createVerticalStrut(6));
        body.add(createSummaryLine("Subtotal", currencyFormat.format(subtotal), false));
        body.add(Box.createVerticalStrut(4));
        body.add(createSummaryLine("Delivery Fee", currencyFormat.format(deliveryFee), false));
        body.add(Box.createVerticalStrut(6));
        body.add(createSummaryLine("Total", currencyFormat.format(total), true));

        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSummaryLine(String labelText, String valueText, boolean emphasized) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel left = new JLabel(labelText);
        JLabel right = new JLabel(valueText);
        left.setFont(new Font("Segoe UI", emphasized ? Font.BOLD : Font.PLAIN, emphasized ? 15 : 14));
        right.setFont(new Font("Segoe UI", emphasized ? Font.BOLD : Font.PLAIN, emphasized ? 15 : 14));
        left.setForeground(new Color(15, 23, 42));
        right.setForeground(new Color(15, 23, 42));

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void openPaymentPanel(Map<Product, Integer> items, boolean preOrder) {
        paymentItems = items;
        paymentIsPreOrder = preOrder;

        double total = 0;
        int totalQty = 0;
        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            total += entry.getKey().price * entry.getValue();
            totalQty += entry.getValue();
        }

        LocalDate estimatedDate = preOrder ? LocalDate.now().plusDays(7) : LocalDate.now().plusDays(2);
        paymentSummaryLabel.setText("Items: " + totalQty + " | Amount: " + currencyFormat.format(total));
        paymentDeliveryLabel.setText("Estimated Delivery: " + estimatedDate);
        paymentNameField.setText(currentUser.fullName);
        paymentAddressField.setText(currentUser.address);
        paymentPhoneField.setText(currentUser.phoneNumber);
        showView(PAYMENT_VIEW);
    }

    private void confirmCheckout() {
        String name = paymentNameField.getText().trim();
        String address = paymentAddressField.getText().trim();
        String phone = paymentPhoneField.getText().trim();
        if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel, "Name, address, and phone number are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String paymentOption = (String) paymentOptionBox.getSelectedItem();
        if ("QR Code".equals(paymentOption)) {
            String orderId = generateOrderId();
            String orderItemsSnapshot = summarizeOrderItems();
            double totalAmountSnapshot = computePaymentTotal();
            showQrCheckoutDialog(name, orderId, summarizeOrderItems(), totalAmountSnapshot, () -> {
                finalizeCheckout(name, address, phone, paymentOption);
                showCustomerReceipt(name, orderItemsSnapshot, totalAmountSnapshot, paymentOption, "-", "-");
            });
            return;
        }
        showCashCheckoutDialog(name, address, phone, paymentOption, computePaymentTotal());
    }

    private Map<Product, Integer> getNonZeroCartItems() {
        Map<Product, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            if (entry.getValue() > 0) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void showView(String view) {
        contentLayout.show(contentPanel, view);
    }

    private void updateCartBadge() {
        int total = 0;
        for (int qty : cart.values()) {
            total += qty;
        }
        cartBadgeLabel.setText("Cart (" + total + ")");
    }

    private void showProfilePanel() {
        profileFullNameField.setText(currentUser.fullName);
        profileAddressField.setText(currentUser.address);
        profilePhoneField.setText(currentUser.phoneNumber);
        refreshProfileHistoryTable();
        showView(PROFILE_VIEW);
    }

    private void saveProfileChanges() {
        String fullName = profileFullNameField.getText().trim();
        String address = profileAddressField.getText().trim();
        String phone = profilePhoneField.getText().trim();
        if (fullName.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel, "All profile fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            DataStorage.getInstance().updateCustomerProfile(currentUser.username, fullName, address, phone);
            currentUser.fullName = fullName;
            currentUser.address = address;
            currentUser.phoneNumber = phone;
            userLabel.setText(fullName);
            JOptionPane.showMessageDialog(mainPanel, "Profile updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshProfileHistoryTable();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(mainPanel, exception.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshProfileHistoryTable() {
        if (profileHistoryTable == null) {
            return;
        }
        String customerName = currentUser.fullName == null || currentUser.fullName.isBlank() ? currentUser.username : currentUser.fullName;
        List<DataStorage.Order> orders = DataStorage.getInstance().getOrders();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Order ID", "Item", "Qty", "Status", "Delivery ETA"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (DataStorage.Order order : orders) {
            boolean matchedByUsername = order.customerUsername != null && !order.customerUsername.isBlank()
                    && order.customerUsername.equalsIgnoreCase(currentUser.username);
            boolean matchedByNameFallback = order.customer.equalsIgnoreCase(customerName)
                    || order.customer.equalsIgnoreCase(currentUser.username);
            if (!matchedByUsername && !matchedByNameFallback) {
                continue;
            }
            model.addRow(new Object[]{order.id, order.itemName, order.quantity, order.status, estimateDelivery(order)});
        }
        profileHistoryTable.setModel(model);
    }

    private String estimateDelivery(DataStorage.Order order) {
        try {
            LocalDate orderDate = LocalDate.parse(order.date);
            int leadDays = order.status != null && order.status.toUpperCase().contains("PRE-ORDER") ? 7 : 2;
            return orderDate.plusDays(leadDays).toString();
        } catch (Exception ignored) {
            return "-";
        }
    }

    private void loadProductsFromInventory() {
        products.clear();
        cart.clear();
        for (DataStorage.Item item : DataStorage.getInstance().getItems()) {
            Product product = new Product(
                    item.id,
                    item.name,
                    item.category == null || item.category.isBlank() ? "General" : item.category,
                    item.price,
                    item.quantity,
                    imageForItemName(item.name, item.category)
            );
            products.add(product);
            cart.put(product, 0);
        }
    }

    private String imageForItemName(String itemName, String category) {
        String key = itemName == null ? "" : itemName.trim().toLowerCase();
        if (key.equals("laptop")) return "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=640";
        if (key.contains("desktop computer")) return "https://images.unsplash.com/photo-1587202372616-b43abea06c2a?w=640";
        if (key.equals("printer")) return "https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=640";
        if (key.equals("router")) return "assets/router-item.png";
        if (key.equals("monitor")) return "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=640";

        if (key.equals("bond paper")) return "assets/bondpaper-item.png";
        if (key.equals("ballpen")) return "assets/ballpen-item.png";
        if (key.equals("notebook")) return "assets/notebook-item.png";
        if (key.equals("stapler")) return "assets/stapler-item.png";
        if (key.equals("folders")) return "assets/folders-item.png";

        if (key.equals("hammer")) return "assets/hammer-item.png";
        if (key.equals("screwdriver")) return "https://images.unsplash.com/photo-1581147036325-99d0d27f7f66?w=640";
        if (key.equals("drill machine")) return "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=640";
        if (key.equals("wrench")) return "https://images.unsplash.com/photo-1581147036325-99d0d27f7f66?w=640";
        if (key.equals("measuring tape")) return "https://images.unsplash.com/photo-1582582429416-1f293f8cf4b7?w=640";

        if (key.equals("packaging tape")) return "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=640";
        if (key.equals("carton box")) return "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=640";
        if (key.equals("bubble wrap")) return "https://images.unsplash.com/photo-1601599561213-832382fd07ba?w=640";
        if (key.equals("plastic bags")) return "https://images.unsplash.com/photo-1583947582886-f40ec95dd752?w=640";
        if (key.equals("cleaning solution")) return "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=640";

        if (key.equals("bolts")) return "https://images.unsplash.com/photo-1581092919535-7146ff1a5900?w=640";
        if (key.equals("nuts")) return "https://images.unsplash.com/photo-1581092919535-7146ff1a5900?w=640";
        if (key.equals("screws")) return "https://images.unsplash.com/photo-1581092919535-7146ff1a5900?w=640";
        if (key.equals("bearings")) return "https://images.unsplash.com/photo-1516110833967-0b5716ca1387?w=640";
        if (key.equals("fuses")) return "https://images.unsplash.com/photo-1622675363311-3e1904dc1885?w=640";

        if (key.equals("shoes")) return "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=640";
        if (key.equals("t-shirts")) return "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=640";
        if (key.equals("backpacks")) return "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=640";
        if (key.equals("water bottles")) return "https://images.unsplash.com/photo-1523362628745-0c100150b504?w=640";
        if (key.equals("headphones")) return "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=640";

        String categoryKey = category == null ? "" : category.trim().toLowerCase();
        return switch (categoryKey) {
            case "electronics" -> "https://images.unsplash.com/photo-1527814050087-3793815479db?w=640";
            case "office supplies" -> "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=640";
            case "tools & equipment" -> "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=640";
            case "consumables" -> "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=640";
            case "spare parts" -> "https://images.unsplash.com/photo-1516110833967-0b5716ca1387?w=640";
            case "finished goods" -> "https://images.unsplash.com/photo-1556740738-b6a63e27c4df?w=640";
            default -> "https://images.unsplash.com/photo-1527814050087-3793815479db?w=640";
        };
    }

    private JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private Icon loadProductImage(String url, String itemName) {
        try {
            BufferedImage image;
            if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
                image = javax.imageio.ImageIO.read(new File(url));
            } else {
                URLConnection connection = new URL(url).openConnection();
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(4000);
                image = javax.imageio.ImageIO.read(connection.getInputStream());
            }
            if (image != null) {
                Image scaled = image.getScaledInstance(210, 130, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ignored) {
            // Fallback below.
        }
        BufferedImage fallback = new BufferedImage(210, 130, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = fallback.createGraphics();
        g2.setPaint(new GradientPaint(0, 0, new Color(30, 64, 175), 210, 130, new Color(29, 78, 216)));
        g2.fillRect(0, 0, 210, 130);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString("Image unavailable", 42, 52);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String text = itemName == null ? "Item" : itemName;
        if (text.length() > 24) {
            text = text.substring(0, 24) + "...";
        }
        g2.drawString(text, 12, 78);
        g2.dispose();
        return new ImageIcon(fallback);
    }

    private void styleHeaderButton(JButton button, Color color) {
        button.setFocusPainted(false);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(8, 12, 8, 12));
    }

    private void styleQuantityButton(JButton button) {
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(48, 30));
        button.setMinimumSize(new Dimension(48, 30));
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setBackground(new Color(225, 232, 242));
        button.setForeground(new Color(17, 24, 39));
    }

    private void styleActionButton(JButton button, Color bg) {
        button.setFocusPainted(false);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(8, 10, 8, 10));
        button.setPreferredSize(new Dimension(120, 36));
    }

    private double computePaymentTotal() {
        double total = 0;
        for (Map.Entry<Product, Integer> entry : paymentItems.entrySet()) {
            total += entry.getKey().price * entry.getValue();
        }
        return total;
    }

    private String summarizeOrderItems() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Product, Integer> entry : paymentItems.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.getKey().name).append(" x").append(entry.getValue());
        }
        return builder.toString();
    }

    private String generateOrderId() {
        return "ORD-" + (System.currentTimeMillis() % 1000000);
    }

    private void finalizeCheckout(String name, String address, String phone, String paymentMethod) {
        List<DataStorage.Order> createdOrders = new java.util.ArrayList<>();
        try {
            for (Map.Entry<Product, Integer> entry : paymentItems.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();
                String orderId = generateOrderId();
                DataStorage.Order order = new DataStorage.Order(
                        orderId,
                        name,
                        product.itemCode,
                        quantity,
                        paymentIsPreOrder ? "PRE-ORDER" : "PENDING",
                        LocalDate.now().toString(),
                        true,
                        paymentMethod,
                        currentUser.username,
                        "",
                        ""
                );
                DataStorage.getInstance().addOrder(order);
                createdOrders.add(order);
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(mainPanel, exception.getMessage(), "Checkout Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentUser.fullName = name;
        currentUser.address = address;
        currentUser.phoneNumber = phone;
        userLabel.setText(name);

        String mode = paymentIsPreOrder ? "Pre-order placed" : "Order placed";
        JOptionPane.showMessageDialog(mainPanel, mode + " with " + paymentMethod + ". Orders created: " + createdOrders.size(), "Success", JOptionPane.INFORMATION_MESSAGE);

        for (Product product : paymentItems.keySet()) {
            cart.put(product, 0);
        }
        paymentItems.clear();
        loadProductsFromInventory();
        refreshProductCards();
        updateCartBadge();
        refreshCartPanel();
        refreshProfileHistoryTable();
        showView(SHOP_VIEW);
    }

    private void showQrCheckoutDialog(String customerName, String orderId, String orderItem, double orderTotal, Runnable onConfirm) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(mainPanel), "QR Payment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(new Color(248, 250, 252));

        String qrData = String.format(Locale.US,
                "Customer: %s%nOrder ID: %s%nOrder Item: %s%nOrder Total: %.2f",
                customerName, orderId, orderItem, orderTotal);

        JLabel qrTitle = new JLabel("Scan to pay", SwingConstants.CENTER);
        qrTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        qrTitle.setForeground(new Color(15, 23, 42));
        root.add(qrTitle, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        JLabel qrLabel = new JLabel("", SwingConstants.CENTER);
        qrLabel.setPreferredSize(new Dimension(QR_SIZE, QR_SIZE));
        qrLabel.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
        loadQrCode(qrData, qrLabel);
        center.add(qrLabel, BorderLayout.NORTH);

        JTextArea info = new JTextArea(
                "Customer: " + customerName + "\n" +
                "Order ID: " + orderId + "\n" +
                "Order Item: " + orderItem + "\n" +
                "Order Total: " + currencyFormat.format(orderTotal)
        );
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        info.setBackground(new Color(241, 245, 249));
        info.setBorder(new EmptyBorder(10, 10, 10, 10));
        center.add(info, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JButton cancel = new JButton("Cancel");
        JButton confirm = new JButton("Confirm Payment");
        styleActionButton(cancel, new Color(108, 117, 125));
        styleActionButton(confirm, new Color(22, 163, 74));
        confirm.setPreferredSize(new Dimension(175, 38));
        confirm.setMinimumSize(new Dimension(175, 38));
        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> {
            dialog.dispose();
            onConfirm.run();
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(confirm);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(460, 640);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    private void showCashCheckoutDialog(String name, String address, String phone, String paymentMethod, double totalAmount) {
        String orderItemsSnapshot = summarizeOrderItems();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(mainPanel), "Cash Payment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(new Color(248, 250, 252));

        JLabel title = new JLabel("Cashier", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(15, 23, 42));
        root.add(title, BorderLayout.NORTH);

        JTextField totalField = new JTextField(String.format(Locale.US, "%.2f", totalAmount));
        totalField.setEditable(false);
        JTextField cashField = new JTextField();
        JTextField changeField = new JTextField("0.00");
        changeField.setEditable(false);
        styleCashField(totalField);
        styleCashField(cashField);
        styleCashField(changeField);

        cashField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                String text = cashField.getText().trim();
                if (text.isEmpty()) {
                    changeField.setText("0.00");
                    return;
                }
                try {
                    double cash = Double.parseDouble(text);
                    double change = cash - totalAmount;
                    changeField.setText(String.format(Locale.US, "%.2f", Math.max(change, 0.0)));
                } catch (NumberFormatException ignored) {
                    changeField.setText("0.00");
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(labeledCashField("Order Total", totalField));
        form.add(Box.createVerticalStrut(10));
        form.add(labeledCashField("Cash Received", cashField));
        form.add(Box.createVerticalStrut(10));
        form.add(labeledCashField("Change", changeField));
        root.add(form, BorderLayout.CENTER);

        JButton cancel = new JButton("Cancel");
        JButton confirm = new JButton("Confirm Payment");
        styleActionButton(cancel, new Color(108, 117, 125));
        styleActionButton(confirm, new Color(22, 163, 74));
        confirm.setPreferredSize(new Dimension(175, 38));
        confirm.setMinimumSize(new Dimension(175, 38));
        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> {
            double cash;
            try {
                cash = Double.parseDouble(cashField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Enter a valid cash amount.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (cash < totalAmount) {
                JOptionPane.showMessageDialog(dialog, "Cash amount is less than order total.", "Insufficient Cash", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dialog.dispose();
            finalizeCheckout(name, address, phone, paymentMethod);
            showCustomerReceipt(name, orderItemsSnapshot, totalAmount, paymentMethod, String.format(Locale.US, "%.2f", cash), changeField.getText());
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(confirm);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(460, 380);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    private void loadQrCode(String qrData, JLabel target) {
        try {
            String encoded = URLEncoder.encode(qrData, StandardCharsets.UTF_8);
            URL qrUrl = new URL("https://quickchart.io/qr?size=" + QR_SIZE + "&margin=6&ecLevel=H&format=png&text=" + encoded);
            BufferedImage image = javax.imageio.ImageIO.read(qrUrl);
            if (image != null) {
                Image scaled = image.getScaledInstance(QR_SIZE, QR_SIZE, Image.SCALE_SMOOTH);
                target.setIcon(new ImageIcon(scaled));
                target.setText("");
                return;
            }
        } catch (Exception ignored) {
            // Fallback below.
        }
        target.setIcon(null);
        target.setText("<html><center>QR service unavailable.<br>Please try again.</center></html>");
    }

    private JPanel labeledCashField(String labelText, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(55, 65, 81));
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void styleCashField(JTextField field) {
        field.setForeground(new Color(17, 24, 39));
        field.setCaretColor(new Color(17, 24, 39));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 36));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private void showCustomerReceipt(String customerName, String orderItems, double total, String paymentMethod, String cashReceived, String change) {
        String receipt = String.format(Locale.US,
                "RECEIPT%n" +
                        "Date: %s%n" +
                        "Customer: %s%n" +
                        "Order Item(s): %s%n" +
                        "Order Total: %.2f%n" +
                        "Payment Method: %s%n" +
                        "Cash Received: %s%n" +
                        "Change: %s%n" +
                        "Status: PAID%n",
                LocalDate.now(), customerName, orderItems, total, paymentMethod, cashReceived, change);

        JTextArea area = new JTextArea(receipt);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        JOptionPane.showMessageDialog(mainPanel, new JScrollPane(area), "Payment Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private static class Product {
        private final String itemCode;
        private final String name;
        private final String category;
        private final double price;
        private final int stock;
        private final String imageUrl;

        private Product(String itemCode, String name, String category, double price, int stock, String imageUrl) {
            this.itemCode = itemCode;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
            this.imageUrl = imageUrl;
        }
    }
}
