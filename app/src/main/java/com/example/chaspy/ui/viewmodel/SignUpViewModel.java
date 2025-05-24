package com.example.chaspy.ui.viewmodel;

import android.text.TextUtils;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.chaspy.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class SignUpViewModel extends ViewModel {

    private static UserRepository userRepository;
    private MutableLiveData<Boolean> isUserRegistered;
    private MutableLiveData<String> errorMessage;

    public SignUpViewModel() {
        userRepository = new UserRepository();
        isUserRegistered = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
    }

    public MutableLiveData<Boolean> getIsUserRegistered() {
        return isUserRegistered;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public boolean validateInput(String email, String password, String firstName, String lastName) {
        // Extract username from email
        String username = email.replace("@gmail.com", "");
        
        if (TextUtils.isEmpty(username) || username.length() < 3) {
            errorMessage.setValue("Username must be at least 3 characters.");
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters.");
            return false;
        }
        
        if (TextUtils.isEmpty(firstName) || firstName.length() < 2) {
            errorMessage.setValue("First name must be at least 2 characters.");
            return false;
        }
        
        if (TextUtils.isEmpty(lastName) || lastName.length() < 2) {
            errorMessage.setValue("Last name must be at least 2 characters.");
            return false;
        }
        return true;
    }

    // Register the user
    public void registerUser(String email, String password, String firstName, String lastName) {
        if (validateInput(email, password, firstName, lastName)) {
            userRepository.registerUser(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Save additional user data
                    userRepository.saveUserData(task, firstName, lastName)
                            .addOnCompleteListener(saveTask -> {
                                if (saveTask.isSuccessful()) {
                                    isUserRegistered.setValue(true);
                                } else {
                                    errorMessage.setValue("Failed to save user data.");
                                }
                            });
                } else {
                    // Check specifically for email already in use error
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        String username = email.replace("@gmail.com", "");
                        errorMessage.setValue("Username '" + username + "' is already taken. Please choose a different username.");
                    } else {
                        errorMessage.setValue("Registration failed: " + task.getException().getMessage());
                    }
                }
            });
        }
    }
}
