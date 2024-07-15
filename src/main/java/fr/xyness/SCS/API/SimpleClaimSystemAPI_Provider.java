package fr.xyness.SCS.API;

import fr.xyness.SCS.SimpleClaimSystem;

/**
 * Provider class for the SimpleClaimSystemAPI.
 * This class is used to initialize and provide access to the API implementation.
 */
public class SimpleClaimSystemAPI_Provider {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Instance of SimpleClaimSystemAPI */
    private static SimpleClaimSystemAPI apiInstance;
    
    
    // *******************
    // *  Other Methods  *
    // *******************
    

    /**
     * Initializes the SimpleClaimSystemAPI with the provided SimpleClaimSystem instance.
     * This method must be called before accessing the API.
     *
     * @param instance the instance of the SimpleClaimSystem to use for API initialization
     */
    public static void initialize(SimpleClaimSystem instance) {
        if (apiInstance == null) {
            apiInstance = new SimpleClaimSystemAPI_Impl(instance);
        }
    }

    /**
     * Returns the initialized SimpleClaimSystemAPI instance.
     * Throws an IllegalStateException if the API has not been initialized.
     *
     * @return the initialized SimpleClaimSystemAPI instance
     * @throws IllegalStateException if the API has not been initialized
     */
    public static SimpleClaimSystemAPI getAPI() {
        if (apiInstance == null) {
            throw new IllegalStateException("API not initialized. Call initialize() first.");
        }
        return apiInstance;
    }
}
