package com.example.chaspy.ui.viewmodel;

import android.text.TextUtils;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.chaspy.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

public class SignInViewModel extends ViewModel {

    private UserRepository userRepository;
    private MutableLiveData<Boolean> isUserSignedIn;
    private MutableLiveData<String> errorMessage;

    public SignInViewModel() {
        userRepository = new UserRepository();
        isUserSignedIn = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
    }

    public MutableLiveData<Boolean> getIsUserSignedIn() {
        return isUserSignedIn;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Validate input fields (username and password)
    public boolean validateInput(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            errorMessage.setValue("Username cannot be empty.");
            return false;
        }
        
        if (username.length() < 3) {
            errorMessage.setValue("Username must be at least 3 characters.");
            return false;
        }
        
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters.");
            return false;
        }
        return true;
    }

    // Sign in the user
    public void signInUser(String username, String password) {
        if (validateInput(username, password)) {
            userRepository.signInUser(username + "@gmail.com", password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Sign-in successful
                    FirebaseUser firebaseUser = task.getResult().getUser();
                    if (firebaseUser != null) {
                        isUserSignedIn.setValue(true);
                    }
                } else {
                    // Sign-in failed
                    errorMessage.setValue("Sign-in failed: " + task.getException().getMessage());
                }
            });
        }
    }
}
