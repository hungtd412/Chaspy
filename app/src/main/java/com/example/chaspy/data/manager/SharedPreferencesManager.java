package com.example.chaspy.data.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "ChasPyPreferences";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ACCOUNT = "rememberAccount";
    private static final String KEY_SAVED_ACCOUNTS = "savedAccounts";
    private static final String KEY_LAST_USED_ACCOUNT = "lastUsedAccount";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveLoginCredentials(String username, String password, boolean rememberAccount) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER_ACCOUNT, rememberAccount);
        
        // If remember account is checked, add to saved accounts
        if (rememberAccount) {
            addToSavedAccounts(username, password);
        }
        
        // Save this as last used account
        editor.putString(KEY_LAST_USED_ACCOUNT, username);
        
        editor.apply();
    }

    private void addToSavedAccounts(String username, String password) {
        List<AccountItem> savedAccounts = getSavedAccounts();
        
        // Check if the account already exists
        boolean found = false;
        for (int i = 0; i < savedAccounts.size(); i++) {
            if (savedAccounts.get(i).getUsername().equals(username)) {
                // Update existing account with new password
                savedAccounts.get(i).setPassword(password);
                found = true;
                break;
            }
        }
        
        // Add new account if not found
        if (!found) {
            savedAccounts.add(new AccountItem(username, password));
        }
        
        // Save updated list
        String json = gson.toJson(savedAccounts);
        sharedPreferences.edit().putString(KEY_SAVED_ACCOUNTS, json).apply();
    }
    
    public List<AccountItem> getSavedAccounts() {
        String json = sharedPreferences.getString(KEY_SAVED_ACCOUNTS, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<AccountItem>>() {}.getType();
        return gson.fromJson(json, type);
    }
    
    public void removeFromSavedAccounts(String username) {
        List<AccountItem> savedAccounts = getSavedAccounts();
        List<AccountItem> updatedAccounts = new ArrayList<>();
        
        for (AccountItem account : savedAccounts) {
            if (!account.getUsername().equals(username)) {
                updatedAccounts.add(account);
            }
        }
        
        String json = gson.toJson(updatedAccounts);
        sharedPreferences.edit().putString(KEY_SAVED_ACCOUNTS, json).apply();
        
        // Check if the deleted account is the currently remembered one
        if (username.equals(getUsername()) && isRememberAccount()) {
            // Clear the remembered credentials
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_USERNAME);
            editor.remove(KEY_PASSWORD);
            editor.putBoolean(KEY_REMEMBER_ACCOUNT, false);
            editor.apply();
        }
    }

    public void clearLoginCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_IS_LOGGED_IN);
        editor.apply();
    }
    
    public void setLoggedOut() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }
    
    public void setRememberAccount(boolean remember) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_REMEMBER_ACCOUNT, remember);
        if (!remember) {
            // Remove the current account from saved accounts
            String currentUsername = getUsername();
            if (!currentUsername.isEmpty()) {
                removeFromSavedAccounts(currentUsername);
            }
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isRememberAccount() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ACCOUNT, false);
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    public String getPassword() {
        return sharedPreferences.getString(KEY_PASSWORD, "");
    }
    
    public String getLastUsedAccount() {
        return sharedPreferences.getString(KEY_LAST_USED_ACCOUNT, "");
    }
    
    public void clearLastUsedAccount() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_LAST_USED_ACCOUNT);
        editor.apply();
    }
    
    // Account item class to store username and password pairs
    public static class AccountItem {
        private String username;
        private String password;
        
        public AccountItem(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}

