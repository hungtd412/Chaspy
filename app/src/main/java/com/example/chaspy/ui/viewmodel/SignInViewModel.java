package com.example.chaspy.ui.viewmodel;

import android.text.TextUtils;
import android.util.Patterns;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.chaspy.repository.UserRepository;
import com.google.firebase.auth.AuthResult;
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

    // Validate input fields (email and password)
    public boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.setValue("Invalid email address.");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters.");
            return false;
        }
        return true;
    }

    // Sign in the user
    public void signInUser(String email, String password) {
        if (validateInput(email, password)) {
            userRepository.signInUser(email, password).addOnCompleteListener(task -> {
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
