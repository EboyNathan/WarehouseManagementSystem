public enum UserRole {
    ADMIN,
    STAFF,
    RECEIVER,
    CUSTOMER;

    public String getDisplayName() {
        return switch (this) {
            case ADMIN -> "Admin";
            case STAFF -> "Staff";
            case RECEIVER -> "Receiver";
            case CUSTOMER -> "Customer";
        };
    }
}
