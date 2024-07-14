package fr.xyness.SCS.API;

import fr.xyness.SCS.SimpleClaimSystem;

public class SimpleClaimSystemAPI_Provider {
    private static SimpleClaimSystemAPI apiInstance;

    public static void initialize(SimpleClaimSystem instance) {
        if (apiInstance == null) {
            apiInstance = new SimpleClaimSystemAPI_Impl(instance);
        }
    }

    public static SimpleClaimSystemAPI getAPI() {
        if (apiInstance == null) {
            throw new IllegalStateException("API not initialized. Call initialize() first.");
        }
        return apiInstance;
    }
}
