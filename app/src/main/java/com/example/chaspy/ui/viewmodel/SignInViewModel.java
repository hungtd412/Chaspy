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

    public SignInViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
        isUserSignedIn = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        savedAccountsLiveData = new MutableLiveData<>();
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
    public String getSavedUsername() {
        return preferencesManager.getUsername();
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
    
    public String getPasswordForUsername(String username) {
        List<AccountItem> accounts = preferencesManager.getSavedAccounts();
        for (AccountItem account : accounts) {
            if (account.getUsername().equals(username)) {
                return account.getPassword();
            }
        }
        return "";
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
    public void signInUser(String username, String password, boolean rememberAccount) {
        if (validateInput(username, password)) {
            userRepository.signInUser(username + "@gmail.com", password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Sign-in successful
                    FirebaseUser firebaseUser = task.getResult().getUser();
                    if (firebaseUser != null) {
                        if (rememberAccount) {
                            preferencesManager.saveLoginCredentials(username, password, true);
                        } else {
                            // Just save login state without remember flag
                            preferencesManager.saveLoginCredentials(username, password, false);
                        }
                        isUserSignedIn.setValue(true);
                    }
                } else {
                    // Sign-in failed
                    errorMessage.setValue("Sign-in failed: " + task.getException().getMessage());
                }
            });
        }
    }

    // Delete an account from saved accounts
    public void deleteSavedAccount(String username) {
        // Remove from SharedPreferences
        preferencesManager.removeFromSavedAccounts(username);
        
        // If this was the last used account, clear last used account
        if (preferencesManager.getLastUsedAccount().equals(username)) {
            preferencesManager.clearLastUsedAccount();
        }
        
        // Reload saved accounts to update the LiveData
        loadSavedAccounts();
    }
}
