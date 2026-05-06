import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {
    private static DataStorage instance;

    private DataStorage() {
        initializeDatabase();
    }

    public static synchronized DataStorage getInstance() {
        if (instance == null) {
            instance = new DataStorage();
        }
        return instance;
    }

    public synchronized void addItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item is required.");
        }

        String sql = "INSERT INTO items (item_code, name, category, quantity, price, aisle, rack, bin_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.id);
            statement.setString(2, item.name);
            statement.setString(3, item.category);
            statement.setInt(4, item.quantity);
            statement.setDouble(5, item.price);
            statement.setString(6, item.aisle);
            statement.setString(7, item.rack);
            statement.setString(8, item.binCode);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to add item.", e);
        }
    }

    public synchronized List<Item> getItems() {
        String sql = "SELECT item_code, name, category, quantity, price, aisle, rack, bin_code FROM items ORDER BY id";
        List<Item> items = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(new Item(
                        resultSet.getString("item_code"),
                        resultSet.getString("name"),
                        resultSet.getString("category"),
                        resultSet.getInt("quantity"),
                        resultSet.getDouble("price"),
                        resultSet.getString("aisle"),
                        resultSet.getString("rack"),
                        resultSet.getString("bin_code")
                ));
            }
            return items;
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to load items.", e);
        }
    }

    public synchronized Item findItemById(String itemId) {
        String sql = "SELECT item_code, name, category, quantity, price, aisle, rack, bin_code FROM items WHERE item_code = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Item(
                            resultSet.getString("item_code"),
                            resultSet.getString("name"),
                            resultSet.getString("category"),
                            resultSet.getInt("quantity"),
                            resultSet.getDouble("price"),
                            resultSet.getString("aisle"),
                            resultSet.getString("rack"),
                            resultSet.getString("bin_code")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to find item.", e);
        }
    }

    public synchronized void updateItem(String originalItemId, Item updatedItem) {
        if (updatedItem == null) {
            throw new IllegalArgumentException("Updated item is required.");
        }

        String sql = "UPDATE items SET item_code = ?, name = ?, category = ?, quantity = ?, price = ?, aisle = ?, rack = ?, bin_code = ?, updated_at = CURRENT_TIMESTAMP WHERE item_code = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, updatedItem.id);
            statement.setString(2, updatedItem.name);
            statement.setString(3, updatedItem.category);
            statement.setInt(4, updatedItem.quantity);
            statement.setDouble(5, updatedItem.price);
            statement.setString(6, updatedItem.aisle);
            statement.setString(7, updatedItem.rack);
            statement.setString(8, updatedItem.binCode);
            statement.setString(9, originalItemId);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Item not found.");
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to update item.", e);
        }
    }

    public synchronized void deleteItem(String itemId) {
        String sql = "DELETE FROM items WHERE item_code = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Item not found.");
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to delete item.", e);
        }
    }

    public synchronized void addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required.");
        }

        String findItemSql = "SELECT id, name, quantity FROM items WHERE item_code = ? OR name = ? LIMIT 1 FOR UPDATE";
        String updateStockSql = "UPDATE items SET quantity = quantity - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        String insertOrderSql = "INSERT INTO orders (order_code, customer_name, customer_username, item_id, quantity, status, order_date, paid, payment_method, courier_name, forwarded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement findItem = connection.prepareStatement(findItemSql)) {
                findItem.setString(1, order.itemName);
                findItem.setString(2, order.itemName);

                try (ResultSet resultSet = findItem.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalArgumentException("Item '" + order.itemName + "' not found in inventory.");
                    }

                    int itemId = resultSet.getInt("id");
                    String itemName = resultSet.getString("name");
                    int availableQuantity = resultSet.getInt("quantity");

                    if (order.quantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be greater than zero.");
                    }
                    if (availableQuantity < order.quantity) {
                        throw new IllegalArgumentException("Insufficient stock! Only " + availableQuantity + " available.");
                    }

                    try (PreparedStatement updateStock = connection.prepareStatement(updateStockSql);
                         PreparedStatement insertOrder = connection.prepareStatement(insertOrderSql)) {
                        updateStock.setInt(1, order.quantity);
                        updateStock.setInt(2, itemId);
                        updateStock.executeUpdate();

                        insertOrder.setString(1, order.id);
                        insertOrder.setString(2, order.customer);
                        insertOrder.setString(3, order.customerUsername == null || order.customerUsername.isBlank() ? null : order.customerUsername);
                        insertOrder.setInt(4, itemId);
                        insertOrder.setInt(5, order.quantity);
                        insertOrder.setString(6, order.status);
                        insertOrder.setDate(7, Date.valueOf(order.date));
                        insertOrder.setBoolean(8, order.paid);
                        insertOrder.setString(9, order.paymentMethod);
                        if (order.courierName == null || order.courierName.isBlank()) {
                            insertOrder.setNull(10, Types.VARCHAR);
                        } else {
                            insertOrder.setString(10, order.courierName);
                        }
                        if (order.forwardedAt == null || order.forwardedAt.isBlank()) {
                            insertOrder.setNull(11, Types.TIMESTAMP);
                        } else {
                            insertOrder.setString(11, order.forwardedAt);
                        }
                        insertOrder.executeUpdate();
                    }

                    connection.commit();
                    order.itemName = itemName;
                }
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof IllegalArgumentException illegalArgumentException) {
                    throw illegalArgumentException;
                }
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to create order.", e);
        }
    }

    public synchronized List<Order> getOrders() {
        String sql = """
                SELECT o.order_code, o.customer_name, o.customer_username, i.name AS item_name, o.quantity, o.status, o.order_date, o.paid, o.payment_method, o.courier_name, o.forwarded_at
                FROM orders o
                JOIN items i ON i.id = o.item_id
                ORDER BY o.id
                """;
        List<Order> orders = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                orders.add(new Order(
                        resultSet.getString("order_code"),
                        resultSet.getString("customer_name"),
                        resultSet.getString("item_name"),
                        resultSet.getInt("quantity"),
                        resultSet.getString("status"),
                        resultSet.getDate("order_date").toLocalDate().toString(),
                        resultSet.getBoolean("paid"),
                        resultSet.getString("payment_method"),
                        resultSet.getString("customer_username"),
                        resultSet.getString("courier_name"),
                        resultSet.getString("forwarded_at")
                ));
            }
            return orders;
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to load orders.", e);
        }
    }

    public synchronized Order findOrderById(String orderId) {
        String sql = """
                SELECT o.order_code, o.customer_name, o.customer_username, i.name AS item_name, o.quantity, o.status, o.order_date, o.paid, o.payment_method, o.courier_name, o.forwarded_at
                FROM orders o
                JOIN items i ON i.id = o.item_id
                WHERE o.order_code = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Order(
                            resultSet.getString("order_code"),
                            resultSet.getString("customer_name"),
                            resultSet.getString("item_name"),
                        resultSet.getInt("quantity"),
                        resultSet.getString("status"),
                        resultSet.getDate("order_date").toLocalDate().toString(),
                        resultSet.getBoolean("paid"),
                        resultSet.getString("payment_method"),
                        resultSet.getString("customer_username"),
                        resultSet.getString("courier_name"),
                        resultSet.getString("forwarded_at")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to find order.", e);
        }
    }

    public synchronized void updateOrderStatus(String orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE order_code = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus);
            statement.setString(2, orderId);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Order not found.");
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to update order status.", e);
        }
    }

    public synchronized void assignCourier(String orderId, String courierName) {
        String sql = """
                UPDATE orders
                SET courier_name = ?, forwarded_at = CURRENT_TIMESTAMP, status = 'FORWARDED TO COURIER'
                WHERE order_code = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courierName);
            statement.setString(2, orderId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Order not found.");
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to assign courier.", e);
        }
    }

    public synchronized void cancelOrder(String orderId) {
        String findOrderSql = "SELECT id, item_id, quantity FROM orders WHERE order_code = ? FOR UPDATE";
        String restoreStockSql = "UPDATE items SET quantity = quantity + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        String deleteOrderSql = "DELETE FROM orders WHERE id = ?";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement findOrder = connection.prepareStatement(findOrderSql)) {
                findOrder.setString(1, orderId);

                try (ResultSet resultSet = findOrder.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalArgumentException("Order not found.");
                    }

                    int orderRowId = resultSet.getInt("id");
                    int itemId = resultSet.getInt("item_id");
                    int quantity = resultSet.getInt("quantity");

                    try (PreparedStatement restoreStock = connection.prepareStatement(restoreStockSql);
                         PreparedStatement deleteOrder = connection.prepareStatement(deleteOrderSql)) {
                        restoreStock.setInt(1, quantity);
                        restoreStock.setInt(2, itemId);
                        restoreStock.executeUpdate();

                        deleteOrder.setInt(1, orderRowId);
                        deleteOrder.executeUpdate();
                    }

                    connection.commit();
                }
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof IllegalArgumentException illegalArgumentException) {
                    throw illegalArgumentException;
                }
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to cancel order.", e);
        }
    }

    public synchronized void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }

        String sql = "INSERT INTO users (username, password, role, full_name, address, phone_number) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.username);
            statement.setString(2, user.password);
            statement.setString(3, user.role.name());
            statement.setString(4, user.fullName);
            statement.setString(5, user.address);
            statement.setString(6, user.phoneNumber);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to add user.", e);
        }
    }

    public synchronized List<User> getUsers() {
        String sql = "SELECT username, password, role, full_name, address, phone_number FROM users ORDER BY idusers";
        List<User> users = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new User(
                        resultSet.getString("username"),
                        resultSet.getString("password"),
                        parseRole(resultSet.getString("role")),
                        resultSet.getString("full_name"),
                        resultSet.getString("address"),
                        resultSet.getString("phone_number")
                ));
            }
            return users;
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to load users.", e);
        }
    }

    public synchronized User findUserByUsername(String username) {
        String sql = "SELECT username, password, role, full_name, address, phone_number FROM users WHERE username = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getString("username"),
                            resultSet.getString("password"),
                            parseRole(resultSet.getString("role")),
                            resultSet.getString("full_name"),
                            resultSet.getString("address"),
                            resultSet.getString("phone_number")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to find user.", e);
        }
    }

    public synchronized void updateUser(String username, String newPassword, UserRole newRole) {
        String sql = """
                UPDATE users
                SET password = CASE WHEN ? = '' THEN password ELSE ? END,
                    role = ?
                WHERE username = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setString(2, newPassword);
            statement.setString(3, newRole.name());
            statement.setString(4, username);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("User not found.");
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to update user.", e);
        }
    }

    public synchronized void updateCustomerProfile(String username, String fullName, String address, String phoneNumber) {
        String sql = """
                UPDATE users
                SET full_name = ?, address = ?, phone_number = ?
                WHERE username = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setString(2, address);
            statement.setString(3, phoneNumber);
            statement.setString(4, username);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("User not found.");
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to update profile.", e);
        }
    }

    public synchronized void deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ? AND LOWER(username) <> 'admin'";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("User not found or protected.");
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to delete user.", e);
        }
    }

    public synchronized User validateUser(String username, String password) {
        String sql = "SELECT username, password, role, full_name, address, phone_number FROM users WHERE username = ? AND password = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getString("username"),
                            resultSet.getString("password"),
                            parseRole(resultSet.getString("role")),
                            resultSet.getString("full_name"),
                            resultSet.getString("address"),
                            resultSet.getString("phone_number")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to validate user.", e);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.JDBC_URL,
                DatabaseConfig.USERNAME,
                DatabaseConfig.PASSWORD
        );
    }

    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found.", e);
        }

        try (Connection connection = openConnection()) {
            ensureUsersTable(connection);
            ensureItemsTable(connection);
            ensureOrdersTable(connection);
            ensureAdminUser(connection);
            ensureDefaultInventoryItems(connection);
        } catch (SQLException e) {
            throw mapDatabaseError("Failed to initialize database.", e);
        }
    }

    private void ensureUsersTable(Connection connection) throws SQLException {
        executeStatement(connection, """
                CREATE TABLE IF NOT EXISTS users (
                    idusers INT NOT NULL AUTO_INCREMENT,
                    username VARCHAR(45) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
                    full_name VARCHAR(100) NULL,
                    address VARCHAR(255) NULL,
                    phone_number VARCHAR(40) NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (idusers)
                )
                """);

        ensureColumn(connection, "users", "role",
                "ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'STAFF'");
        ensureColumn(connection, "users", "created_at",
                "ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        ensureColumn(connection, "users", "full_name",
                "ALTER TABLE users ADD COLUMN full_name VARCHAR(100) NULL");
        ensureColumn(connection, "users", "address",
                "ALTER TABLE users ADD COLUMN address VARCHAR(255) NULL");
        ensureColumn(connection, "users", "phone_number",
                "ALTER TABLE users ADD COLUMN phone_number VARCHAR(40) NULL");
        ensureUniqueIndex(connection, "users", "uq_users_username", "CREATE UNIQUE INDEX uq_users_username ON users (username)");
    }

    private void ensureItemsTable(Connection connection) throws SQLException {
        executeStatement(connection, """
                CREATE TABLE IF NOT EXISTS items (
                    id INT NOT NULL AUTO_INCREMENT,
                    item_code VARCHAR(50) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    category VARCHAR(100) NULL,
                    quantity INT NOT NULL DEFAULT 0,
                    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                    aisle VARCHAR(30) NULL,
                    rack VARCHAR(30) NULL,
                    bin_code VARCHAR(30) NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uq_items_item_code (item_code)
                )
                """);
        ensureColumn(connection, "items", "aisle",
                "ALTER TABLE items ADD COLUMN aisle VARCHAR(30) NULL");
        ensureColumn(connection, "items", "rack",
                "ALTER TABLE items ADD COLUMN rack VARCHAR(30) NULL");
        ensureColumn(connection, "items", "bin_code",
                "ALTER TABLE items ADD COLUMN bin_code VARCHAR(30) NULL");
    }

    private void ensureOrdersTable(Connection connection) throws SQLException {
        executeStatement(connection, """
                CREATE TABLE IF NOT EXISTS orders (
                    id INT NOT NULL AUTO_INCREMENT,
                    order_code VARCHAR(50) NOT NULL,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_username VARCHAR(45) NULL,
                    item_id INT NOT NULL,
                    quantity INT NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    order_date DATE NOT NULL,
                    paid TINYINT(1) NOT NULL DEFAULT 0,
                    payment_method VARCHAR(20) NULL,
                    courier_name VARCHAR(40) NULL,
                    forwarded_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uq_orders_order_code (order_code),
                    CONSTRAINT fk_orders_item FOREIGN KEY (item_id) REFERENCES items (id)
                )
                """);
        ensureColumn(connection, "orders", "paid",
                "ALTER TABLE orders ADD COLUMN paid TINYINT(1) NOT NULL DEFAULT 0");
        ensureColumn(connection, "orders", "payment_method",
                "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) NULL");
        ensureColumn(connection, "orders", "customer_username",
                "ALTER TABLE orders ADD COLUMN customer_username VARCHAR(45) NULL");
        ensureColumn(connection, "orders", "courier_name",
                "ALTER TABLE orders ADD COLUMN courier_name VARCHAR(40) NULL");
        ensureColumn(connection, "orders", "forwarded_at",
                "ALTER TABLE orders ADD COLUMN forwarded_at TIMESTAMP NULL");
    }

    private void ensureDefaultInventoryItems(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM items";
        try (PreparedStatement countStmt = connection.prepareStatement(countSql);
             ResultSet rs = countStmt.executeQuery()) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }
        List<Item> defaults = List.of(
                // Electronics
                new Item("ELEC-001", "Laptop", "Electronics", 20, 42000.00),
                new Item("ELEC-002", "Desktop Computer", "Electronics", 12, 36000.00),
                new Item("ELEC-003", "Printer", "Electronics", 18, 8500.00),
                new Item("ELEC-004", "Router", "Electronics", 30, 2500.00),
                new Item("ELEC-005", "Monitor", "Electronics", 24, 7800.00),

                // Office supplies
                new Item("OFF-001", "Bond Paper", "Office supplies", 100, 280.00),
                new Item("OFF-002", "Ballpen", "Office supplies", 200, 25.00),
                new Item("OFF-003", "Notebook", "Office supplies", 140, 65.00),
                new Item("OFF-004", "Stapler", "Office supplies", 60, 145.00),
                new Item("OFF-005", "Folders", "Office supplies", 120, 35.00),

                // tools & equipment
                new Item("TOOL-001", "Hammer", "tools & equipment", 50, 320.00),
                new Item("TOOL-002", "Screwdriver", "tools & equipment", 85, 120.00),
                new Item("TOOL-003", "Drill Machine", "tools & equipment", 22, 3400.00),
                new Item("TOOL-004", "Wrench", "tools & equipment", 48, 260.00),
                new Item("TOOL-005", "Measuring Tape", "tools & equipment", 75, 140.00),

                // Consumables
                new Item("CON-001", "Packaging Tape", "Consumables", 160, 85.00),
                new Item("CON-002", "Carton Box", "Consumables", 130, 45.00),
                new Item("CON-003", "Bubble Wrap", "Consumables", 90, 210.00),
                new Item("CON-004", "Plastic Bags", "Consumables", 220, 30.00),
                new Item("CON-005", "Cleaning Solution", "Consumables", 70, 180.00),

                // Spare Parts
                new Item("SP-001", "Bolts", "Spare Parts", 300, 4.00),
                new Item("SP-002", "Nuts", "Spare Parts", 300, 3.00),
                new Item("SP-003", "Screws", "Spare Parts", 500, 2.50),
                new Item("SP-004", "Bearings", "Spare Parts", 120, 95.00),
                new Item("SP-005", "Fuses", "Spare Parts", 150, 18.00),

                // Finished goods
                new Item("FG-001", "Shoes", "Finished goods", 80, 1299.00),
                new Item("FG-002", "T-Shirts", "Finished goods", 140, 399.00),
                new Item("FG-003", "Backpacks", "Finished goods", 60, 899.00),
                new Item("FG-004", "Water Bottles", "Finished goods", 110, 220.00),
                new Item("FG-005", "Headphones", "Finished goods", 55, 1599.00)
        );
        String insertSql = "INSERT INTO items (item_code, name, category, quantity, price, aisle, rack, bin_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        for (Item item : defaults) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setString(1, item.id);
                insertStmt.setString(2, item.name);
                insertStmt.setString(3, item.category);
                insertStmt.setInt(4, item.quantity);
                insertStmt.setDouble(5, item.price);
                insertStmt.setString(6, "");
                insertStmt.setString(7, "");
                insertStmt.setString(8, "");
                insertStmt.executeUpdate();
            }
        }
    }

    private void ensureAdminUser(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            if (resultSet.getInt(1) == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO users (username, password, role) VALUES ('admin', '1234', 'ADMIN')")) {
                    insert.executeUpdate();
                }
            } else {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET role = 'ADMIN' WHERE username = 'admin' AND (role IS NULL OR role <> 'ADMIN')")) {
                    update.executeUpdate();
                }
            }
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String alterSql) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ? AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.SCHEMA);
            statement.setString(2, tableName);
            statement.setString(3, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                    executeStatement(connection, alterSql);
                }
            }
        }
    }

    private void ensureUniqueIndex(Connection connection, String tableName, String indexName, String createSql) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = ? AND index_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.SCHEMA);
            statement.setString(2, tableName);
            statement.setString(3, indexName);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                    executeStatement(connection, createSql);
                }
            }
        }
    }

    private void executeStatement(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private IllegalStateException mapDatabaseError(String message, SQLException exception) {
        if (exception.getErrorCode() == 1062) {
            return new IllegalStateException("A record with that unique value already exists.");
        }
        if (exception.getErrorCode() == 1451) {
            return new IllegalStateException("This record is linked to other data and cannot be deleted.");
        }
        return new IllegalStateException(message + " " + exception.getMessage(), exception);
    }

    private UserRole parseRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return UserRole.STAFF;
        }
        return UserRole.valueOf(roleValue.toUpperCase());
    }

    public static class User {
        public String username;
        public String password;
        public UserRole role;
        public String fullName;
        public String address;
        public String phoneNumber;

        public User(String username, String password, UserRole role) {
            this(username, password, role, "", "", "");
        }

        public User(String username, String password, UserRole role, String fullName, String address, String phoneNumber) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.fullName = fullName == null ? "" : fullName;
            this.address = address == null ? "" : address;
            this.phoneNumber = phoneNumber == null ? "" : phoneNumber;
        }
    }

    public static class Item {
        public String id;
        public String name;
        public String category;
        public int quantity;
        public double price;
        public String aisle;
        public String rack;
        public String binCode;

        public Item(String id, String name, String category, int quantity, double price) {
            this(id, name, category, quantity, price, "", "", "");
        }

        public Item(String id, String name, String category, int quantity, double price, String aisle, String rack, String binCode) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.price = price;
            this.aisle = aisle == null ? "" : aisle;
            this.rack = rack == null ? "" : rack;
            this.binCode = binCode == null ? "" : binCode;
        }
    }

    public static class Order {
        public String id;
        public String customer;
        public String itemName;
        public int quantity;
        public String status;
        public String date;
        public boolean paid;
        public String paymentMethod;
        public String customerUsername;
        public String courierName;
        public String forwardedAt;

        public Order(String id, String customer, String itemName, int quantity, String status, String date) {
            this(id, customer, itemName, quantity, status, date, false, "");
        }

        public Order(String id, String customer, String itemName, int quantity, String status, String date, boolean paid) {
            this(id, customer, itemName, quantity, status, date, paid, "");
        }

        public Order(String id, String customer, String itemName, int quantity, String status, String date, boolean paid, String paymentMethod) {
            this(id, customer, itemName, quantity, status, date, paid, paymentMethod, "", "", "");
        }

        public Order(String id, String customer, String itemName, int quantity, String status, String date, boolean paid, String paymentMethod, String courierName, String forwardedAt) {
            this(id, customer, itemName, quantity, status, date, paid, paymentMethod, "", courierName, forwardedAt);
        }

        public Order(String id, String customer, String itemName, int quantity, String status, String date, boolean paid, String paymentMethod, String customerUsername, String courierName, String forwardedAt) {
            this.id = id;
            this.customer = customer;
            this.itemName = itemName;
            this.quantity = quantity;
            this.status = status;
            this.date = date;
            this.paid = paid;
            this.paymentMethod = paymentMethod == null ? "" : paymentMethod;
            this.customerUsername = customerUsername == null ? "" : customerUsername;
            this.courierName = courierName == null ? "" : courierName;
            this.forwardedAt = forwardedAt == null ? "" : forwardedAt;
        }
    }
}
