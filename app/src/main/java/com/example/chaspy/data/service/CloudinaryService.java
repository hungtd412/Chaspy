package com.example.chaspy.data.service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.chaspy.config.CloudinaryConfig;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Task;

import java.util.Map;
import java.util.UUID;

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
}
