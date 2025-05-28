package com.example.chaspy.data.service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.utils.ObjectUtils;
import com.example.chaspy.config.CloudinaryConfig;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Task;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CloudinaryService {
    private static final String TAG = "CloudinaryService";
    
    // Initialize Cloudinary's MediaManager
    public static void initCloudinary(Context context) {
        CloudinaryConfig.init(context);
    }
    
    // Upload an image to Cloudinary
    public Task<String> uploadImage(Context context, Uri imageUri) {
        // Ensure Cloudinary is initialized
        initCloudinary(context);
        
        // Create a task completion source to handle the async operation
        TaskCompletionSource<String> uploadTaskSource = new TaskCompletionSource<>();
        
        // Generate a unique file name to avoid conflicts
        String fileName = "profile_" + UUID.randomUUID().toString();
        
        // Start the upload process
        String requestId = MediaManager.get().upload(imageUri)
            .option("folder", "chaspy/avatars")
            .option("public_id", fileName)
            .option("resource_type", "image")
            .callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    Log.d(TAG, "Upload started for image: " + imageUri);
                }
                
                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    double progress = (double) bytes / totalBytes;
                    Log.d(TAG, "Upload progress: " + Math.round(progress * 100) + "%");
                }
                
                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String secureUrl = (String) resultData.get("secure_url");
                    Log.d(TAG, "Upload successful. URL: " + secureUrl);
                    uploadTaskSource.setResult(secureUrl);
                }
                
                @Override
                public void onError(String requestId, ErrorInfo error) {
                    Log.e(TAG, "Upload error: " + error.getDescription());
                    uploadTaskSource.setException(new Exception("Failed to upload image: " + error.getDescription()));
                }
                
                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                    Log.d(TAG, "Upload rescheduled due to: " + error.getDescription());
                }
            })
            .dispatch();
            
        return uploadTaskSource.getTask();
    }
    
    // Delete an image from Cloudinary using its public_id
    public Task<Boolean> deleteImage(Context context, String publicId) {
        // Ensure Cloudinary is initialized
        initCloudinary(context);
        
        // Create a task completion source to handle the async operation
        TaskCompletionSource<Boolean> deleteTaskSource = new TaskCompletionSource<>();
        
        if (publicId == null || publicId.isEmpty()) {
            deleteTaskSource.setException(new IllegalArgumentException("Public ID cannot be null or empty"));
            return deleteTaskSource.getTask();
        }
        
        // Create an executor to run the Cloudinary API call in a background thread
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Call the destroy method with correct parameters
                Map result = MediaManager.get().getCloudinary().uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
                );
                
                // Check result and set task result accordingly
                String status = (String) result.get("result");
                if ("ok".equals(status)) {
                    Log.d(TAG, "Image deleted successfully: " + publicId);
                    deleteTaskSource.setResult(true);
                } else {
                    Log.e(TAG, "Failed to delete image: " + result);
                    deleteTaskSource.setResult(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during image deletion", e);
                deleteTaskSource.setException(e);
            }
        });
        
        return deleteTaskSource.getTask();
    }
    
    // Extract public_id from Cloudinary URL
    public String extractPublicIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Pattern to extract public ID from Cloudinary URL
        // Example URL: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/chaspy/avatars/profile_abc123.jpg
        Pattern pattern = Pattern.compile("cloudinary\\.com/[^/]+/image/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+$");
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    // Check if the URL belongs to a default profile picture
    public boolean isDefaultProfileImage(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        return url.contains("default_profile");
    }
}

