import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainMenu {
    private static final Color PAGE_BACKGROUND = new Color(244, 246, 249);
    private static final Color HEADER_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(28, 41, 64);
    private static final Color TEXT_SECONDARY = new Color(102, 115, 136);
    private static final Color ACCENT_COLOR = new Color(210, 72, 72);
    private static final Color DIVIDER_COLOR = new Color(226, 232, 240);

    private final DataStorage.User currentUser;
    private final UserRole userRole;
    private final InventoryForm inventoryForm;
    private final OrderForm orderForm;
    private final UserForm userForm;
    private final ReportForm reportForm;
    private final ReceiverForm receiverForm;
    private final Runnable logoutHandler;
    private final CardLayout contentLayout = new CardLayout();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private JPanel mainPanel;
    private JPanel contentPanel;
    private JButton inventoryButton;
    private JButton ordersButton;
    private JButton usersButton;
    private JButton reportsButton;
    private JButton logoutButton;
    private JButton exitButton;
    private JLabel brandLabel;
    private JLabel userInfoLabel;
    private String activeModule;

    public MainMenu() {
        this(new DataStorage.User("admin", "", UserRole.ADMIN), null);
    }

    public MainMenu(UserRole userRole) {
        this(new DataStorage.User(userRole.getDisplayName().toLowerCase(), "", userRole), null);
    }

    public MainMenu(DataStorage.User currentUser) {
        this(currentUser, null);
    }

    public MainMenu(DataStorage.User currentUser, Runnable logoutHandler) {
        this.currentUser = currentUser;
        this.userRole = currentUser.role;
        this.logoutHandler = logoutHandler;
        this.inventoryForm = new InventoryForm(userRole);
        this.orderForm = new OrderForm(userRole);
        this.userForm = new UserForm(userRole);
        this.reportForm = new ReportForm(userRole);
        this.receiverForm = new ReceiverForm();

        if (mainPanel == null) {
            buildUi();
        } else {
            configureBoundUi();
        }
        wireActions();
        applyRoleAccess();
        showModule(userRole == UserRole.RECEIVER ? "receiver" : "inventory");
    }

    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(PAGE_BACKGROUND);

        JPanel headerPanel = new JPanel(new BorderLayout(24, 0));
        headerPanel.setBackground(HEADER_BACKGROUND);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER_COLOR),
                new EmptyBorder(18, 24, 18, 24)
        ));

        brandLabel = new JLabel("WAREHOUSE MANAGEMENT SYSTEM");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandLabel.setForeground(TEXT_PRIMARY);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 26, 0));
        navPanel.setOpaque(false);

        inventoryButton = createNavButton("Inventory");
        ordersButton = createNavButton("Orders");
        usersButton = createNavButton("Users");
        reportsButton = createNavButton("Reports");

        navButtons.put("inventory", inventoryButton);
        navButtons.put("orders", ordersButton);
        navButtons.put("users", usersButton);
        navButtons.put("reports", reportsButton);

        navPanel.add(inventoryButton);
        navPanel.add(ordersButton);
        navPanel.add(usersButton);
        navPanel.add(reportsButton);

        JPanel accountPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        accountPanel.setOpaque(false);

        userInfoLabel = new JLabel(currentUser.username + " (" + userRole.getDisplayName() + ")");
        userInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userInfoLabel.setForeground(TEXT_SECONDARY);

        logoutButton = new JButton("Logout");
        exitButton = new JButton("Exit");
        styleUtilityButton(logoutButton, new Color(102, 115, 136));
        styleUtilityButton(exitButton, new Color(210, 72, 72));

        accountPanel.add(userInfoLabel);
        accountPanel.add(logoutButton);
        accountPanel.add(exitButton);

        headerPanel.add(brandLabel, BorderLayout.WEST);
        headerPanel.add(navPanel, BorderLayout.CENTER);
        headerPanel.add(accountPanel, BorderLayout.EAST);

        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(PAGE_BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        contentPanel.add(wrapContent(inventoryForm.getMainPanel()), "inventory");
        contentPanel.add(wrapContent(orderForm.getMainPanel()), "orders");
        contentPanel.add(wrapContent(userForm.getMainPanel()), "users");
        contentPanel.add(wrapContent(reportForm.getMainPanel()), "reports");
        contentPanel.add(wrapContent(receiverForm.getMainPanel()), "receiver");

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel wrapContent(JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private void configureBoundUi() {
        mainPanel.setBackground(PAGE_BACKGROUND);
        if (brandLabel != null) {
            brandLabel.setText("WAREHOUSE MANAGEMENT SYSTEM");
            brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            brandLabel.setForeground(TEXT_PRIMARY);
        }
        if (userInfoLabel != null) {
            userInfoLabel.setText(currentUser.username + " (" + userRole.getDisplayName() + ")");
            userInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            userInfoLabel.setForeground(TEXT_SECONDARY);
        }

        navButtons.clear();
        styleNavButton(inventoryButton, "Inventory");
        styleNavButton(ordersButton, "Orders");
        styleNavButton(usersButton, "Users");
        styleNavButton(reportsButton, "Reports");

        navButtons.put("inventory", inventoryButton);
        navButtons.put("orders", ordersButton);
        navButtons.put("users", usersButton);
        navButtons.put("reports", reportsButton);

        styleUtilityButton(logoutButton, new Color(102, 115, 136));
        styleUtilityButton(exitButton, new Color(210, 72, 72));

        contentPanel.removeAll();
        contentPanel.setLayout(contentLayout);
        contentPanel.setBackground(PAGE_BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        contentPanel.add(wrapContent(inventoryForm.getMainPanel()), "inventory");
        contentPanel.add(wrapContent(orderForm.getMainPanel()), "orders");
        contentPanel.add(wrapContent(userForm.getMainPanel()), "users");
        contentPanel.add(wrapContent(reportForm.getMainPanel()), "reports");
        contentPanel.add(wrapContent(receiverForm.getMainPanel()), "receiver");
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        styleNavButton(button, text);
        return button;
    }

    private void styleNavButton(JButton button, String text) {
        if (button == null) {
            return;
        }
        button.setText(text);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(new EmptyBorder(8, 2, 8, 2));
        button.setForeground(TEXT_PRIMARY);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleUtilityButton(JButton button, Color background) {
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
    }

    private void wireActions() {
        inventoryButton.addActionListener(event -> showNavigationMenu(inventoryButton, "inventory", inventoryActions()));
        ordersButton.addActionListener(event -> showNavigationMenu(ordersButton, "orders", orderActions()));
        usersButton.addActionListener(event -> showNavigationMenu(usersButton, "users", userActions()));
        reportsButton.addActionListener(event -> showNavigationMenu(reportsButton, "reports", reportActions()));

        logoutButton.addActionListener(event -> handleLogout());
        exitButton.addActionListener(event -> System.exit(0));
    }

    private NamedRunnable[] inventoryActions() {
        return new NamedRunnable[]{
                action("Add Item", () -> inventoryForm.showAction("add")),
                action("Edit Item", () -> inventoryForm.showAction("edit")),
                action("Search Item", () -> inventoryForm.showAction("search")),
                action("Delete Item", () -> inventoryForm.showAction("delete"))
        };
    }

    private NamedRunnable[] orderActions() {
        if (userRole == UserRole.RECEIVER) {
            return new NamedRunnable[]{
                    action("Order Queue", () -> {
                        showModule("receiver");
                        receiverForm.showQueueView();
                    }),
                    action("Assign Courier", () -> {
                        showModule("receiver");
                        receiverForm.showQueueView();
                    }),
                    action("History", () -> {
                        showModule("receiver");
                        receiverForm.showHistoryView();
                    })
            };
        }
        return new NamedRunnable[]{
                action("Create Order", () -> orderForm.showAction("create")),
                action("Update Status", () -> orderForm.showAction("status")),
                action("Search Order", () -> orderForm.showAction("search")),
                action("Cancel Order", () -> orderForm.showAction("cancel"))
        };
    }

    private NamedRunnable[] userActions() {
        return new NamedRunnable[]{
                action("Add User", () -> userForm.showAction("add")),
                action("Edit User", () -> userForm.showAction("edit")),
                action("Search User", () -> userForm.showAction("search")),
                action("Delete User", () -> userForm.showAction("delete"))
        };
    }

    private NamedRunnable[] reportActions() {
        return new NamedRunnable[]{
                action("Daily Inventory", () -> reportForm.showAction("daily")),
                action("Low Stock Alerts", () -> reportForm.showAction("low-stock")),
                action("Inventory Report", () -> reportForm.showAction("inventory")),
                action("Order Report", () -> reportForm.showAction("orders")),
                action("Sales Report", () -> reportForm.showAction("sales"))
        };
    }

    private NamedRunnable action(String label, Runnable runnable) {
        return new NamedRunnable(label, runnable);
    }

    private void showNavigationMenu(JButton source, String moduleKey, NamedRunnable[] actions) {
        showModule(moduleKey);

        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(DIVIDER_COLOR));

        for (NamedRunnable namedAction : actions) {
            JMenuItem item = new JMenuItem(namedAction.label);
            item.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            item.addActionListener(event -> {
                showModule(moduleKey);
                namedAction.run();
            });
            menu.add(item);
        }

        menu.show(source, 0, source.getHeight());
    }

    private void applyRoleAccess() {
        if (userRole == UserRole.STAFF) {
            usersButton.setEnabled(false);
            usersButton.setToolTipText("Only admins can access user management.");
        } else if (userRole == UserRole.RECEIVER) {
            inventoryButton.setEnabled(false);
            usersButton.setEnabled(false);
            reportsButton.setEnabled(false);
            ordersButton.setText("Dispatch");
            ordersButton.setToolTipText("Receiver dispatch workflow");
        }
    }

    private void showModule(String moduleKey) {
        contentLayout.show(contentPanel, moduleKey);
        activeModule = moduleKey;
        refreshNavigationState();
    }

    private void refreshNavigationState() {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeModule);
            JButton button = entry.getValue();
            button.setForeground(active ? ACCENT_COLOR : TEXT_PRIMARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, active ? ACCENT_COLOR : HEADER_BACKGROUND),
                    new EmptyBorder(8, 2, 8, 2)
            ));
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(mainPanel, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (logoutHandler != null) {
                logoutHandler.run();
                return;
            }

            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(mainPanel);
            if (frame != null) {
                LoginForm loginForm = new LoginForm();
                frame.setTitle("Login");
                frame.setContentPane(loginForm.getMainPanel());
                frame.revalidate();
                frame.repaint();
            }
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginForm::showLoginWindow);
    }

    private static class NamedRunnable implements Runnable {
        private final String label;
        private final Runnable delegate;

        private NamedRunnable(String label, Runnable delegate) {
            this.label = label;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            delegate.run();
        }
    }
}
