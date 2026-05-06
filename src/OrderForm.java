import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderForm {
    private static final String OVERVIEW_CARD = "overview";
    private static final String CREATE_CARD = "create";
    private static final String STATUS_CARD = "status";
    private static final String SEARCH_CARD = "search";
    private static final String CANCEL_CARD = "cancel";
    private static final int QR_DISPLAY_SIZE = 340;

    private final UserRole userRole;
    private final CardLayout contentLayout = new CardLayout();

    private JPanel mainPanel;
    private JPanel contentPanel;
    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JLabel statusLabel;

    private JTextField createOrderIdField;
    private JTextField createCustomerField;
    private JComboBox<String> createItemBox;
    private JSpinner createQuantitySpinner;
    private JComboBox<String> createPriorityBox;

    private JComboBox<String> statusOrderIdBox;
    private JComboBox<String> statusComboBox;
    private JTextArea statusPreviewArea;

    private JTextField searchField;
    private JTextArea searchResultsArea;

    private JComboBox<String> cancelOrderIdBox;
    private JTextArea cancelPreviewArea;

    public OrderForm() {
        this(UserRole.ADMIN);
    }

    public OrderForm(UserRole userRole) {
        this.userRole = userRole;
        if (mainPanel == null) {
            buildUi();
        } else {
            configureBoundUi();
        }
        showOverview();
    }

    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout(0, 18));
        mainPanel.setBackground(ModuleTheme.PAGE_BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        titleLabel = new JLabel("Order Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        titleLabel.setForeground(ModuleTheme.TITLE_COLOR);

        descriptionLabel = new JLabel("Select an order option from the top menu.");
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        descriptionLabel.setForeground(ModuleTheme.SUBTITLE_COLOR);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(descriptionLabel);

        contentPanel = new JPanel(contentLayout);
        contentPanel.setOpaque(false);
        contentPanel.add(createOverviewPanel(), OVERVIEW_CARD);
        contentPanel.add(createCreatePanel(), CREATE_CARD);
        contentPanel.add(createStatusPanel(), STATUS_CARD);
        contentPanel.add(createSearchPanel(), SEARCH_CARD);
        contentPanel.add(createCancelPanel(), CANCEL_CARD);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.add(statusLabel, BorderLayout.WEST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createOverviewPanel() {
        JPanel panel = createSurfacePanel();
        panel.setLayout(new BorderLayout(16, 0));
        JLabel message = new JLabel("<html><div style='width:520px;'>Use the Orders dropdown in the top navigation bar to open Create Order, Update Status, Search Order, or Cancel Order in this same panel.</div></html>");
        message.setFont(new Font("Segoe UI", Font.BOLD, 22));
        message.setForeground(ModuleTheme.SURFACE_TEXT);
        JPanel textWrap = new JPanel(new BorderLayout());
        textWrap.setOpaque(false);
        textWrap.add(message, BorderLayout.NORTH);
        panel.add(textWrap, BorderLayout.WEST);
        panel.add(createOverviewIllustrationPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent createOverviewIllustrationPanel() {
        JComponent illustration = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int pad = 18;
                int frameW = w - pad * 2;
                int frameH = h - pad * 2;

                g2.setColor(new Color(22, 57, 104, 60));
                g2.fillRoundRect(pad, pad, frameW, frameH, 12, 12);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.drawRoundRect(pad, pad, frameW, frameH, 12, 12);

                int left = pad + 24;
                int top = pad + 24;
                int colGap = 16;
                int colW = (frameW - 48 - colGap * 2) / 3;
                int cardH = 74;

                drawInfoCard(g2, left, top, colW, cardH, new Color(122, 217, 194, 180));
                drawInfoCard(g2, left + colW + colGap, top, colW, cardH, new Color(123, 179, 255, 180));
                drawInfoCard(g2, left + (colW + colGap) * 2, top, colW, cardH, new Color(251, 200, 111, 180));

                int listY = top + cardH + 20;
                int rowH = 42;
                for (int i = 0; i < 4; i++) {
                    int y = listY + i * (rowH + 10);
                    g2.setColor(new Color(255, 255, 255, 38));
                    g2.fillRoundRect(left, y, frameW - 48, rowH, 8, 8);
                    g2.setColor(new Color(255, 255, 255, 90));
                    g2.drawRoundRect(left, y, frameW - 48, rowH, 8, 8);
                    g2.fillRoundRect(left + 10, y + 11, 16, 16, 4, 4);
                    g2.fillRoundRect(left + 36, y + 13, 140, 12, 6, 6);
                    g2.fillRoundRect(left + frameW - 180, y + 13, 86, 12, 6, 6);
                }
                g2.dispose();
            }

            private void drawInfoCard(Graphics2D g2, int x, int y, int w, int h, Color color) {
                g2.setColor(color);
                g2.fillRoundRect(x, y, w, h, 10, 10);
                g2.setColor(new Color(255, 255, 255, 110));
                g2.drawRoundRect(x, y, w, h, 10, 10);
                g2.fillRoundRect(x + 10, y + 12, 24, 24, 6, 6);
                g2.fillRoundRect(x + 42, y + 15, w - 52, 10, 5, 5);
                g2.fillRoundRect(x + 42, y + 33, w - 72, 8, 4, 4);
            }
        };
        illustration.setOpaque(false);
        illustration.setPreferredSize(new Dimension(520, 280));
        return illustration;
    }

    private void configureBoundUi() {
        mainPanel.setBackground(ModuleTheme.PAGE_BACKGROUND);
        titleLabel.setText("Order Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        titleLabel.setForeground(ModuleTheme.TITLE_COLOR);
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        descriptionLabel.setForeground(ModuleTheme.SUBTITLE_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        contentPanel.removeAll();
        contentPanel.setLayout(contentLayout);
        contentPanel.setOpaque(false);
        contentPanel.add(createOverviewPanel(), OVERVIEW_CARD);
        contentPanel.add(createCreatePanel(), CREATE_CARD);
        contentPanel.add(createStatusPanel(), STATUS_CARD);
        contentPanel.add(createSearchPanel(), SEARCH_CARD);
        contentPanel.add(createCancelPanel(), CANCEL_CARD);
    }

    private JPanel createCreatePanel() {
        JPanel panel = createSurfacePanel();
        panel.setLayout(new BorderLayout(0, 16));

        createOrderIdField = new JTextField();
        createCustomerField = new JTextField();
        createItemBox = new JComboBox<>();
        createQuantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        createPriorityBox = new JComboBox<>(new String[]{"Normal", "Express", "Critical"});

        JButton createButton = createActionButton("Create Order");
        createButton.addActionListener(event -> handleCreateOrder());

        panel.add(createSectionTitle("Create Order"), BorderLayout.NORTH);
        panel.add(createFormStack(
                "Order ID", createOrderIdField,
                "Customer Name", createCustomerField,
                "Item Name or ID", createItemBox,
                "Quantity", createQuantitySpinner,
                "Priority", createPriorityBox
        ), BorderLayout.CENTER);
        panel.add(createButtonRow(createButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = createSurfacePanel();
        panel.setLayout(new BorderLayout(0, 16));

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        topPanel.setOpaque(false);

        statusOrderIdBox = new JComboBox<>();
        statusComboBox = new JComboBox<>(new String[]{"Pending", "Processing", "Shipped", "Delivered", "Returned"});
        topPanel.add(labeledCombo("Order ID", statusOrderIdBox));
        topPanel.add(labeledCombo("New Status", statusComboBox));

        statusPreviewArea = createTextArea();
        JButton updateButton = createActionButton("Update Status");
        updateButton.addActionListener(event -> handleUpdateStatus());

        panel.add(createSectionTitle("Update Order Status"), BorderLayout.NORTH);
        panel.add(topPanel, BorderLayout.BEFORE_FIRST_LINE);
        panel.add(new JScrollPane(statusPreviewArea), BorderLayout.CENTER);
        panel.add(createButtonRow(updateButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = createSurfacePanel();
        panel.setLayout(new BorderLayout(0, 16));

        JPanel topPanel = new JPanel(new BorderLayout(12, 0));
        topPanel.setOpaque(false);
        searchField = new JTextField();
        JButton searchButton = createActionButton("Search");
        searchButton.addActionListener(event -> handleSearchOrders());
        topPanel.add(labeledField("Search by order ID or customer", searchField), BorderLayout.CENTER);
        topPanel.add(searchButton, BorderLayout.EAST);

        searchResultsArea = createTextArea();
        panel.add(createSectionTitle("Search Order"), BorderLayout.NORTH);
        panel.add(topPanel, BorderLayout.BEFORE_FIRST_LINE);
        panel.add(new JScrollPane(searchResultsArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCancelPanel() {
        JPanel panel = createSurfacePanel();
        panel.setLayout(new BorderLayout(0, 16));

        JPanel topPanel = new JPanel(new BorderLayout(12, 0));
        topPanel.setOpaque(false);
        cancelOrderIdBox = new JComboBox<>();
        JButton previewButton = createSecondaryButton("Preview Order");
        previewButton.addActionListener(event -> handlePreviewCancel());
        topPanel.add(labeledCombo("Order ID", cancelOrderIdBox), BorderLayout.CENTER);
        topPanel.add(previewButton, BorderLayout.EAST);

        cancelPreviewArea = createTextArea();
        JButton cancelButton = createDangerButton("Cancel Order");
        cancelButton.addActionListener(event -> handleCancelOrder());

        panel.add(createSectionTitle("Cancel Order"), BorderLayout.NORTH);
        panel.add(topPanel, BorderLayout.BEFORE_FIRST_LINE);
        panel.add(new JScrollPane(cancelPreviewArea), BorderLayout.CENTER);
        panel.add(createButtonRow(cancelButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSurfacePanel() {
        return ModuleTheme.createSurfacePanel();
    }

    private JLabel createSectionTitle(String text) {
        return ModuleTheme.createSectionTitle(text);
    }

    private JPanel labeledField(String labelText, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = ModuleTheme.createFieldLabel(labelText);
        panel.add(label, BorderLayout.NORTH);
        ModuleTheme.styleInputField(field);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JPanel labeledCombo(String labelText, JComboBox<String> comboBox) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = ModuleTheme.createFieldLabel(labelText);
        panel.add(label, BorderLayout.NORTH);
        ModuleTheme.styleComboBox(comboBox);
        panel.add(comboBox, BorderLayout.CENTER);
        return panel;
    }

    private JPanel labeledSpinner(String labelText, JSpinner spinner) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = ModuleTheme.createFieldLabel(labelText);
        panel.add(label, BorderLayout.NORTH);
        spinner.setPreferredSize(new Dimension(0, 34));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            ModuleTheme.styleInputField(defaultEditor.getTextField());
        }
        panel.add(spinner, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFormStack(Object... fields) {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        for (int i = 0; i < fields.length; i += 2) {
            String label = (String) fields[i];
            JComponent component = (JComponent) fields[i + 1];
            if (component instanceof JComboBox<?> comboBox) {
                stack.add(labeledCombo(label, (JComboBox<String>) comboBox));
            } else if (component instanceof JSpinner spinner) {
                stack.add(labeledSpinner(label, spinner));
            } else {
                stack.add(labeledField(label, (JTextField) component));
            }
            if (i + 2 < fields.length) {
                stack.add(Box.createVerticalStrut(12));
            }
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(stack, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel createButtonRow(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(button);
        return panel;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        ModuleTheme.styleTextArea(area);
        return area;
    }

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        ModulePanelFactory.styleActionButton(button, new Color(83, 109, 254));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        ModulePanelFactory.styleActionButton(button, new Color(92, 107, 128));
        return button;
    }

    private JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        ModulePanelFactory.styleActionButton(button, new Color(211, 47, 47));
        return button;
    }

    private void showOverview() {
        descriptionLabel.setText("Select an order option from the top menu.");
        clearStatus();
        contentLayout.show(contentPanel, OVERVIEW_CARD);
    }

    private void showCreatePanel() {
        descriptionLabel.setText("Create a new order in this panel.");
        clearStatus();
        createOrderIdField.setText("ORD-" + (DataStorage.getInstance().getOrders().size() + 101));
        createCustomerField.setText("");
        reloadCreateItemOptions();
        createQuantitySpinner.setValue(1);
        createPriorityBox.setSelectedIndex(0);
        contentLayout.show(contentPanel, CREATE_CARD);
    }

    private void showStatusPanel() {
        descriptionLabel.setText("Update an order status in this panel.");
        clearStatus();
        reloadStatusOrderOptions();
        statusComboBox.setSelectedIndex(0);
        statusPreviewArea.setText("");
        contentLayout.show(contentPanel, STATUS_CARD);
    }

    private void showSearchPanel() {
        descriptionLabel.setText("Search order records in this panel.");
        clearStatus();
        searchField.setText("");
        searchResultsArea.setText("");
        contentLayout.show(contentPanel, SEARCH_CARD);
    }

    private void showCancelPanel() {
        descriptionLabel.setText("Preview and cancel an order in this panel.");
        clearStatus();
        reloadCancelOrderOptions();
        cancelPreviewArea.setText("");
        contentLayout.show(contentPanel, CANCEL_CARD);
    }

    private void handleCreateOrder() {
        try {
            String orderId = createOrderIdField.getText().trim();
            String customer = createCustomerField.getText().trim();
            String itemQuery = (String) createItemBox.getSelectedItem();
            int quantity = (Integer) createQuantitySpinner.getValue();
            String status = "PENDING (" + createPriorityBox.getSelectedItem() + ")";
            String date = LocalDate.now().toString();

            if (orderId.isEmpty() || customer.isEmpty() || itemQuery == null || itemQuery.isBlank()) {
                throw new IllegalArgumentException("Order ID, customer, and item are required.");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }

            DataStorage.Item item = findItem(itemQuery);
            if (item == null) {
                throw new IllegalArgumentException("Item '" + itemQuery + "' not found in inventory.");
            }

            double orderTotal = item.price * quantity;
            String paymentMethod = showPaymentDialog(orderId, customer, item.name, quantity, orderTotal);
            if (paymentMethod == null) {
                showError("Payment was cancelled.");
                return;
            }

            DataStorage.Order newOrder = new DataStorage.Order(orderId, customer, item.name, quantity, status, date, true, paymentMethod);
            DataStorage.getInstance().addOrder(newOrder);
            showCreatePanel();
            showReceipt(orderId, customer, item.name, quantity, orderTotal, paymentMethod);
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "Order " + orderId + " created successfully.",
                    "Order Created",
                    JOptionPane.INFORMATION_MESSAGE
            );
            showSuccess("Order " + orderId + " created successfully.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private String showPaymentDialog(String orderId, String customer, String itemName, int quantity, double totalPrice) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(mainPanel), "Order Payment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel summaryLabel = new JLabel(String.format(Locale.US,
                "<html><b>Order:</b> %s &nbsp;&nbsp; <b>Customer:</b> %s<br><b>Item:</b> %s x%d &nbsp;&nbsp; <b>Total:</b> %.2f</html>",
                orderId, customer, itemName, quantity, totalPrice));
        root.add(summaryLabel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Pay by QR", createQrPaymentPanel(orderId, customer, itemName, totalPrice));
        JPanel cashPanel = createCashierPaymentPanel(totalPrice);
        tabs.addTab("Pay by Cash", cashPanel);
        tabs.putClientProperty("cashField", cashPanel.getClientProperty("cashField"));
        root.add(tabs, BorderLayout.CENTER);

        final String[] paymentMethod = {null};
        JButton paidButton = createActionButton("Confirm Payment");
        paidButton.setPreferredSize(new Dimension(170, 38));
        paidButton.setMinimumSize(new Dimension(170, 38));
        paidButton.addActionListener(event -> {
            if (tabs.getSelectedIndex() == 1) {
                JTextField cashField = (JTextField) tabs.getClientProperty("cashField");
                if (cashField == null) {
                    showError("Cash input is unavailable.");
                    return;
                }
                double cash;
                try {
                    cash = Double.parseDouble(cashField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Enter a valid cash amount.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (cash < totalPrice) {
                    JOptionPane.showMessageDialog(dialog, "Cash amount is less than order total.", "Insufficient Cash", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                paymentMethod[0] = "CASH";
            } else {
                paymentMethod[0] = "QR";
            }
            dialog.dispose();
        });

        JButton cancelButton = createSecondaryButton("Cancel");
        cancelButton.addActionListener(event -> dialog.dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.add(cancelButton);
        buttonRow.add(paidButton);
        root.add(buttonRow, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(640, 680);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
        return paymentMethod[0];
    }

    private JPanel createQrPaymentPanel(String orderId, String customer, String itemName, double totalPrice) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        String qrData = String.format(Locale.US,
                "Customer: %s%nOrder ID: %s%nOrder Item: %s%nOrder Total: %.2f",
                customer, orderId, itemName, totalPrice);

        JLabel qrLabel = new JLabel("", SwingConstants.CENTER);
        qrLabel.setPreferredSize(new Dimension(QR_DISPLAY_SIZE, QR_DISPLAY_SIZE));
        qrLabel.setMinimumSize(new Dimension(280, 280));
        qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qrLabel.setVerticalAlignment(SwingConstants.CENTER);
        qrLabel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        loadQrCodeToLabel(qrData, qrLabel);

        JPanel qrWrap = new JPanel(new GridBagLayout());
        qrWrap.setOpaque(false);
        qrWrap.add(qrLabel);

        JTextArea infoArea = new JTextArea("Scan to pay.\n" + qrData);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);
        infoArea.setRows(5);
        ModuleTheme.styleTextArea(infoArea);

        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setPreferredSize(new Dimension(0, 130));

        panel.add(qrWrap, BorderLayout.CENTER);
        panel.add(infoScroll, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCashierPaymentPanel(double totalPrice) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JTextField totalField = new JTextField(String.format(Locale.US, "%.2f", totalPrice));
        totalField.setEditable(false);
        JTextField cashField = new JTextField();
        JTextField changeField = new JTextField("0.00");
        changeField.setEditable(false);
        ModuleTheme.styleInputField(totalField);
        ModuleTheme.styleInputField(cashField);
        ModuleTheme.styleInputField(changeField);
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
                    double change = cash - totalPrice;
                    changeField.setText(String.format(Locale.US, "%.2f", Math.max(change, 0.0)));
                } catch (NumberFormatException ignored) {
                    changeField.setText("0.00");
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        panel.add(labeledCashField("Order Total", totalField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledCashField("Cash Received", cashField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledCashField("Change", changeField));

        panel.putClientProperty("cashField", cashField);
        return panel;
    }

    private JPanel labeledCashField(String labelText, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = ModuleTheme.createFieldLabel(labelText);
        label.setForeground(new Color(55, 65, 81));
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void styleCashField(JTextField field) {
        field.setForeground(new Color(17, 24, 39));
        field.setCaretColor(new Color(17, 24, 39));
        field.setPreferredSize(new Dimension(0, 36));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private void loadQrCodeToLabel(String qrData, JLabel targetLabel) {
        try {
            String encodedData = URLEncoder.encode(qrData, StandardCharsets.UTF_8);
            int qrSize = QR_DISPLAY_SIZE;
            URL qrUrl = new URL("https://quickchart.io/qr?size=" + qrSize + "&margin=6&ecLevel=H&format=png&text=" + encodedData);
            BufferedImage image = javax.imageio.ImageIO.read(qrUrl);
            if (image != null) {
                int width = Math.max(1, targetLabel.getPreferredSize().width);
                int height = Math.max(1, targetLabel.getPreferredSize().height);
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                targetLabel.setIcon(new ImageIcon(scaledImage));
                targetLabel.setText("");
                return;
            }
        } catch (Exception ignored) {
            // If QR API is unavailable, show fallback text.
        }
        targetLabel.setIcon(null);
        targetLabel.setText("<html><center>QR service unavailable.<br>Use Pay by Cash instead.</center></html>");
    }

    private void showReceipt(String orderId, String customer, String itemName, int quantity, double totalPrice, String paymentMethod) {
        String receipt = String.format(Locale.US,
                "RECEIPT%n" +
                "Date: %s%n" +
                "Order ID: %s%n" +
                "Customer: %s%n" +
                "Item: %s%n" +
                "Quantity: %d%n" +
                "Total: %.2f%n" +
                "Payment Method: %s%n" +
                "Status: PAID%n",
                LocalDate.now(), orderId, customer, itemName, quantity, totalPrice, paymentMethod);

        JTextArea area = new JTextArea(receipt);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        JOptionPane.showMessageDialog(mainPanel, new JScrollPane(area), "Payment Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleUpdateStatus() {
        String orderId = (String) statusOrderIdBox.getSelectedItem();
        if (orderId == null || orderId.isBlank()) {
            showError("Select an order ID first.");
            return;
        }

        DataStorage.Order order = DataStorage.getInstance().findOrderById(orderId);
        if (order == null) {
            statusPreviewArea.setText("");
            showError("Order ID '" + orderId + "' not found.");
            return;
        }

        String newStatus = (String) statusComboBox.getSelectedItem();
        try {
            DataStorage.getInstance().updateOrderStatus(orderId, newStatus);
            statusPreviewArea.setText(
                    "Order ID: " + order.id + "\n" +
                    "Customer: " + order.customer + "\n" +
                    "Item: " + order.itemName + "\n" +
                    "Date: " + order.date + "\n" +
                    "Updated Status: " + newStatus
            );
            showSuccess("Order " + orderId + " updated successfully.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void handleSearchOrders() {
        String query = searchField.getText().trim().toLowerCase();
        List<DataStorage.Order> matches = new ArrayList<>();
        for (DataStorage.Order order : DataStorage.getInstance().getOrders()) {
            if (query.isEmpty()
                    || order.id.toLowerCase().contains(query)
                    || order.customer.toLowerCase().contains(query)) {
                matches.add(order);
            }
        }

        if (matches.isEmpty()) {
            searchResultsArea.setText("No orders found.");
            showError("No orders matched the search.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (DataStorage.Order order : matches) {
            builder.append("Order ID: ").append(order.id).append('\n');
            builder.append("Customer: ").append(order.customer).append('\n');
            builder.append("Item: ").append(order.itemName).append('\n');
            builder.append("Quantity: ").append(order.quantity).append('\n');
            builder.append("Status: ").append(order.status).append('\n');
            builder.append("Date: ").append(order.date).append("\n\n");
        }
        searchResultsArea.setText(builder.toString().trim());
        showSuccess(matches.size() + " order(s) found.");
    }

    private void handlePreviewCancel() {
        String orderId = (String) cancelOrderIdBox.getSelectedItem();
        if (orderId == null || orderId.isBlank()) {
            showError("Enter an order ID first.");
            return;
        }

        DataStorage.Order order = DataStorage.getInstance().findOrderById(orderId);
        if (order == null) {
            cancelPreviewArea.setText("");
            showError("Order ID '" + orderId + "' not found.");
            return;
        }

        cancelPreviewArea.setText(
                "Order ID: " + order.id + "\n" +
                "Customer: " + order.customer + "\n" +
                "Item: " + order.itemName + "\n" +
                "Quantity: " + order.quantity + "\n" +
                "Status: " + order.status + "\n" +
                "Date: " + order.date
        );
        showSuccess("Order " + orderId + " is ready to cancel.");
    }

    private void handleCancelOrder() {
        String orderId = (String) cancelOrderIdBox.getSelectedItem();
        if (orderId == null || orderId.isBlank()) {
            showError("Enter an order ID first.");
            return;
        }

        try {
            DataStorage.getInstance().cancelOrder(orderId);
            cancelPreviewArea.setText("");
            reloadCancelOrderOptions();
            JOptionPane.showMessageDialog(mainPanel, "Order " + orderId + " was cancelled successfully.", "Order Cancelled", JOptionPane.INFORMATION_MESSAGE);
            showSuccess("Order " + orderId + " cancelled successfully.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private DataStorage.Item findItem(String itemQuery) {
        for (DataStorage.Item item : DataStorage.getInstance().getItems()) {
            if (item.id.equalsIgnoreCase(itemQuery) || item.name.equalsIgnoreCase(itemQuery)) {
                return item;
            }
        }
        return null;
    }

    private void reloadCreateItemOptions() {
        createItemBox.removeAllItems();
        for (DataStorage.Item item : DataStorage.getInstance().getItems()) {
            createItemBox.addItem(item.name);
        }
        if (createItemBox.getItemCount() > 0) {
            createItemBox.setSelectedIndex(0);
        }
    }

    private void reloadCancelOrderOptions() {
        cancelOrderIdBox.removeAllItems();
        for (DataStorage.Order order : DataStorage.getInstance().getOrders()) {
            cancelOrderIdBox.addItem(order.id);
        }
        if (cancelOrderIdBox.getItemCount() > 0) {
            cancelOrderIdBox.setSelectedIndex(0);
        }
    }

    private void reloadStatusOrderOptions() {
        statusOrderIdBox.removeAllItems();
        for (DataStorage.Order order : DataStorage.getInstance().getOrders()) {
            statusOrderIdBox.addItem(order.id);
        }
        if (statusOrderIdBox.getItemCount() > 0) {
            statusOrderIdBox.setSelectedIndex(0);
        }
    }

    private void clearStatus() {
        statusLabel.setText(" ");
    }

    private void showSuccess(String message) {
        statusLabel.setForeground(new Color(46, 125, 50));
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(new Color(211, 47, 47));
        statusLabel.setText(message);
    }

    public void showAction(String actionKey) {
        switch (actionKey) {
            case "create" -> showCreatePanel();
            case "status" -> showStatusPanel();
            case "search" -> showSearchPanel();
            case "cancel" -> showCancelPanel();
            default -> throw new IllegalArgumentException("Unknown order action: " + actionKey);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
