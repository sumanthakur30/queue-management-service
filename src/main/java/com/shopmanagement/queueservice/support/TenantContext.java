package com.shopmanagement.queueservice.support;

import java.util.List;

import com.shopmanagement.queueservice.filter.RequestIdFilter;

public final class TenantContext {

    private TenantContext() {
    }

    public static Long requireTenantId() {
        Long tenantId = RequestIdFilter.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    public static String requireShopId() {
        String shopId = RequestIdFilter.getCurrentShopId();
        if (shopId == null || shopId.isBlank()) {
            throw new IllegalStateException("Missing shop context");
        }
        return shopId;
    }

    public static void requireAnyPermission(String... permissions) {
        String role = RequestIdFilter.getCurrentRole();
        if ("SUPER_ADMIN".equals(role) || "SHOP_OWNER".equals(role) || "CLINIC_ADMIN".equals(role)) {
            return;
        }
        List<String> granted = RequestIdFilter.getCurrentPermissions();
        for (String permission : permissions) {
            if (granted.contains(permission)) {
                return;
            }
        }
        throw new SecurityException("Forbidden: missing required permission");
    }
}
