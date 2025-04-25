package com.example.chaspy.ui.viewmodel;

import android.text.TextUtils;
import android.util.Patterns;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.chaspy.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

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

    // Validate the email, password, and name fields
    public boolean validateInput(String email, String password, String firstName, String lastName) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.setValue("Invalid email address.");
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
                    FirebaseUser firebaseUser = task.getResult().getUser();
                    // Save additional user data
                    userRepository.saveUserData(firebaseUser, firstName, lastName)
                            .addOnCompleteListener(saveTask -> {
                                if (saveTask.isSuccessful()) {
                                    isUserRegistered.setValue(true);
                                } else {
                                    errorMessage.setValue("Failed to save user data.");
                                }
                            });
                } else {
                    errorMessage.setValue("Registration failed: " + task.getException().getMessage());
                }
            });
        }
    }
}
