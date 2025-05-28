package com.example.chaspy.config;

import android.content.Context;
import com.cloudinary.android.MediaManager;
import com.example.chaspy.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    private static boolean isInitialized = false;
    public static Map<String, String> config;

    public static void init(Context context) {
        if (!isInitialized) {
            config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            config.put("secure", "true");

            try {
                MediaManager.init(context, config);
                isInitialized = true;
            } catch (IllegalStateException e) {
                // Already initialized
            }
        }
    }

    /**
     * Check if Cloudinary is initialized
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
}
