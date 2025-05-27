package com.example.chaspy.ui.viewmodel;

import android.text.TextUtils;
import android.util.Patterns;
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.chaspy.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class SignUpViewModel extends ViewModel {

    private static UserRepository userRepository;
    private MutableLiveData<Boolean> isUserRegistered;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Pair<String, String>> validationErrors;
    private MutableLiveData<Boolean> verificationEmailSent;

    public SignUpViewModel() {
        userRepository = new UserRepository();
        isUserRegistered = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        validationErrors = new MutableLiveData<>();
        verificationEmailSent = new MutableLiveData<>();
    }

    public MutableLiveData<Boolean> getIsUserRegistered() {
        return isUserRegistered;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public MutableLiveData<Pair<String, String>> getValidationErrors() {
        return validationErrors;
    }
    
    public MutableLiveData<Boolean> getVerificationEmailSent() {
        return verificationEmailSent;
    }

    private boolean validateInput(String email, String password, String confirmPassword, String firstName, String lastName) {
        // First name validation
        if (TextUtils.isEmpty(firstName)) {
            validationErrors.setValue(new Pair<>("firstName", "First name is required"));
            return false;
        }
        
        if (firstName.length() < 2) {
            validationErrors.setValue(new Pair<>("firstName", "First name must be at least 2 characters"));
            return false;
        }
        
        // Last name validation
        if (TextUtils.isEmpty(lastName)) {
            validationErrors.setValue(new Pair<>("lastName", "Last name is required"));
            return false;
        }
        
        if (lastName.length() < 2) {
            validationErrors.setValue(new Pair<>("lastName", "Last name must be at least 2 characters"));
            return false;
        }

        // Email validation
        if (TextUtils.isEmpty(email)) {
            validationErrors.setValue(new Pair<>("email", "Email is required"));
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            validationErrors.setValue(new Pair<>("email", "Please enter a valid email address"));
            return false;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            validationErrors.setValue(new Pair<>("password", "Password is required"));
            return false;
        }
        
        if (password.length() < 6) {
            validationErrors.setValue(new Pair<>("password", "Password must be at least 6 characters"));
            return false;
        }
        
        // Confirm password validation
        if (TextUtils.isEmpty(confirmPassword)) {
            validationErrors.setValue(new Pair<>("confirmPassword", "Please confirm your password"));
            return false;
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            validationErrors.setValue(new Pair<>("confirmPassword", "Passwords do not match"));
            return false;
        }
        
        return true;
    }

    // Register the user with improved validation and email verification
    public void registerUser(String email, String password, String confirmPassword, String firstName, String lastName) {
        if (validateInput(email, password, confirmPassword, firstName, lastName)) {
            userRepository.registerUser(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Get the Firebase user
                    FirebaseUser user = task.getResult().getUser();
                    
                    // Send verification email
                    userRepository.sendEmailVerification(user)
                        .addOnCompleteListener(verificationTask -> {
                            if (verificationTask.isSuccessful()) {
                                verificationEmailSent.setValue(true);
                                
                                // Save additional user data
                                userRepository.saveUserData(task, firstName, lastName)
                                    .addOnCompleteListener(saveTask -> {
                                        if (saveTask.isSuccessful()) {
                                            isUserRegistered.setValue(true);
                                        } else {
                                            errorMessage.setValue("Failed to save user data: " + 
                                                (saveTask.getException() != null ? saveTask.getException().getMessage() : "Unknown error"));
                                        }
                                    });
                            } else {
                                errorMessage.setValue("Failed to send verification email: " + 
                                    (verificationTask.getException() != null ? verificationTask.getException().getMessage() : "Unknown error"));
                            }
                        });
                } else {
                    // Check specifically for email already in use error
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        errorMessage.setValue("Email address is already registered. Please use a different email or sign in.");
                    } else {
                        errorMessage.setValue("Registration failed: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
        }
    }
}
