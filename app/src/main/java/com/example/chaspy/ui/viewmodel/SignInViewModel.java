package com.example.chaspy.ui.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chaspy.data.manager.SharedPreferencesManager;
import com.example.chaspy.data.manager.SharedPreferencesManager.AccountItem;
import com.example.chaspy.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class SignInViewModel extends AndroidViewModel {

    private UserRepository userRepository;
    private MutableLiveData<Boolean> isUserSignedIn;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<List<AccountItem>> savedAccountsLiveData;
    private SharedPreferencesManager preferencesManager;
    private MutableLiveData<Boolean> needEmailVerification;
    private MutableLiveData<Boolean> verificationEmailResent;
    private String currentEmail; // Store current username
    private String currentPassword; // Store current password

    public SignInViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
        isUserSignedIn = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        savedAccountsLiveData = new MutableLiveData<>();
        needEmailVerification = new MutableLiveData<>();
        verificationEmailResent = new MutableLiveData<>();
        preferencesManager = new SharedPreferencesManager(application);
        
        // Load saved accounts immediately
        loadSavedAccounts();
    }

    public MutableLiveData<Boolean> getIsUserSignedIn() {
        return isUserSignedIn;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<List<AccountItem>> getSavedAccountsLiveData() {
        return savedAccountsLiveData;
    }
    
    public MutableLiveData<Boolean> getNeedEmailVerification() {
        return needEmailVerification;
    }
    
    public MutableLiveData<Boolean> getVerificationEmailResent() {
        return verificationEmailResent;
    }
    
    public void loadSavedAccounts() {
        List<AccountItem> accounts = preferencesManager.getSavedAccounts();
        savedAccountsLiveData.setValue(accounts);
    }

    // Check if user is already logged in
    public boolean checkAutoLogin() {
        if (preferencesManager.isLoggedIn()) {
            isUserSignedIn.setValue(true);
            return true;
        }
        return false;
    }

    // Get saved credentials
    public String getSavedEmail() {
        return preferencesManager.getEmail();
    }

    public String getSavedPassword() {
        return preferencesManager.getPassword();
    }
    
    public String getLastUsedAccount() {
        return preferencesManager.getLastUsedAccount();
    }

    public boolean isRememberAccount() {
        return preferencesManager.isRememberAccount();
    }
    
    public String getPasswordForEmail(String email) {
        List<AccountItem> accounts = preferencesManager.getSavedAccounts();
        for (AccountItem account : accounts) {
            if (account.getEmail().equals(email)) {
                return account.getPassword();
            }
        }
        return "";
    }

    // Validate input fields (username and password)
    public boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            errorMessage.setValue("Email cannot be empty.");
            return false;
        }
        
        if (email.length() < 3) {
            errorMessage.setValue("Email must be at least 3 characters.");
            return false;
        }
        
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters.");
            return false;
        }
        return true;
    }

    // Sign in the user
    public void signInUser(String email, String password, boolean rememberAccount) {
        if (validateInput(email, password)) {
            // Store current email and password for potential verification email resending
            currentEmail = email;
            currentPassword = password;
            
            userRepository.signInUser(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Sign-in successful
                    FirebaseUser firebaseUser = task.getResult().getUser();
                    if (firebaseUser != null) {
                        // Check if email is verified
                        firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                            if (userRepository.isEmailVerified(firebaseUser)) {
                                if (rememberAccount) {
                                    preferencesManager.saveLoginCredentials(email, password, true);
                                } else {
                                    // Just save login state without remember flag
                                    preferencesManager.saveLoginCredentials(email, password, false);
                                }
                                isUserSignedIn.setValue(true);
                            } else {
                                // Email not verified
                                needEmailVerification.setValue(true);
                                userRepository.signOut();
                            }
                        });
                    }
                } else {
                    // Sign-in failed
                    errorMessage.setValue("Sign-in failed: " + task.getException().getMessage());
                }
            });
        }
    }

    // Method to resend verification email
    public void resendVerificationEmail() {
        if (TextUtils.isEmpty(currentEmail)) {
            errorMessage.setValue("Email cannot be empty.");
            return;
        }
        
        if (TextUtils.isEmpty(currentPassword)) {
            errorMessage.setValue("Password is required to resend verification email.");
            return;
        }
        
        // Create email from username

        userRepository.resendVerificationEmail(currentEmail, currentPassword)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    verificationEmailResent.setValue(true);
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    if (errorMsg.contains("password") || errorMsg.contains("credential")) {
                        errorMessage.setValue("Incorrect password. Please try signing in again.");
                    } else {
                        errorMessage.setValue("Failed to resend verification email: " + errorMsg);
                    }
                }
            });
    }

    // Delete an account from saved accounts
    public void deleteSavedAccount(String email) {
        // Remove from SharedPreferences
        preferencesManager.removeFromSavedAccounts(email);
        
        // If this was the last used account, clear last used account
        if (preferencesManager.getLastUsedAccount().equals(email)) {
            preferencesManager.clearLastUsedAccount();
        }
        
        // Reload saved accounts to update the LiveData
        loadSavedAccounts();
    }
}
