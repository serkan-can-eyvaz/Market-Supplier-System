package com.example.marketsupplier.security;

public enum UserRoles {
    ADMIN("ADMIN", "System Administrator", 100),
    MANAGER("MANAGER", "Market Manager", 80),
    SUPPLIER("SUPPLIER", "Product Supplier", 60),
    CUSTOMER("CUSTOMER", "End Customer", 40),
    GUEST("GUEST", "Guest User", 20),
    SYSTEM("SYSTEM", "System User", 90);

    private final String role;
    private final String description;
    private final int priority;

    UserRoles(String role, String description, int priority) {
        this.role = role;
        this.description = description;
        this.priority = priority;
    }

    public String getRole() {
        return role;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public boolean hasHigherPriorityThan(UserRoles other) {
        return this.priority > other.priority;
    }

    public boolean hasEqualOrHigherPriorityThan(UserRoles other) {
        return this.priority >= other.priority;
    }

    public static UserRoles fromString(String role) {
        for (UserRoles userRole : UserRoles.values()) {
            if (userRole.role.equalsIgnoreCase(role)) {
                return userRole;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }

    public static boolean isValidRole(String role) {
        try {
            fromString(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
