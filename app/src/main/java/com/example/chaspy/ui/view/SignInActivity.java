package com.example.chaspy.ui.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.chaspy.R;
import com.example.chaspy.data.manager.SharedPreferencesManager.AccountItem;
import com.example.chaspy.ui.viewmodel.SignInViewModel;

import java.util.List;

public class SignInActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private CheckBox cbRememberAccount;
    private SignInViewModel signInViewModel;
    private PopupWindow accountSuggestionsPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        setupForgotPasswordListener();

        // Initialize ViewModel
        signInViewModel = new ViewModelProvider(this).get(SignInViewModel.class);

        // Link UI with variables
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvSignUp = findViewById(R.id.tv_sign_up);
        cbRememberAccount = findViewById(R.id.cb_remember_account);

        // Check if user is already logged in
        if (signInViewModel.checkAutoLogin()) {
            startActivity(new Intent(SignInActivity.this, ConversationActivity.class));
            finish();
            return;
        }

        // Set up field focus listeners to show account suggestions
        setupFieldFocusListeners();

        // Set up observers
        setupObservers();

        // Handle login button
        btnLogin.setOnClickListener(view -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean rememberAccount = cbRememberAccount.isChecked();

            // Use ViewModel to handle login
            signInViewModel.signInUser(username, password, rememberAccount);
        });

        // Handle navigation to SignUpActivity
        tvSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Load saved credentials if available
        if (signInViewModel.isRememberAccount()) {
            etUsername.setText(signInViewModel.getSavedUsername());
            etPassword.setText(signInViewModel.getSavedPassword());
            cbRememberAccount.setChecked(true);
        }
    }

    private void setupFieldFocusListeners() {
        etUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                List<AccountItem> accounts = signInViewModel.getSavedAccountsLiveData().getValue();
                if (accounts != null && !accounts.isEmpty()) {
                    showAccountSuggestions(etUsername, accounts);
                }
            } else {
                dismissAccountSuggestionsPopup();
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // If username is already filled, try to auto-fill password
                String username = etUsername.getText().toString().trim();
                if (!username.isEmpty()) {
                    String savedPassword = signInViewModel.getPasswordForUsername(username);
                    if (!savedPassword.isEmpty()) {
                        etPassword.setText(savedPassword);
                    }
                } else {
                    // If username is empty, show account suggestions
                    List<AccountItem> accounts = signInViewModel.getSavedAccountsLiveData().getValue();
                    if (accounts != null && !accounts.isEmpty()) {
                        showAccountSuggestions(etPassword, accounts);
                    }
                }
            } else {
                dismissAccountSuggestionsPopup();
            }
        });
    }

    private void showAccountSuggestions(EditText anchor, List<AccountItem> accounts) {
        // Dismiss any existing popup
        dismissAccountSuggestionsPopup();

        // Create container for suggestion items
        LinearLayout suggestionContainer = new LinearLayout(this);
        suggestionContainer.setOrientation(LinearLayout.VERTICAL);
        suggestionContainer.setBackgroundResource(android.R.color.white);

        // Add border to the suggestion container
        suggestionContainer.setElevation(8f);

        // Create popup window
        accountSuggestionsPopup = new PopupWindow(
                suggestionContainer,
                anchor.getWidth(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        // Add suggestion items
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AccountItem account : accounts) {
            View suggestionItem = inflater.inflate(R.layout.account_suggestion_item, suggestionContainer, false);
            TextView tvUsername = suggestionItem.findViewById(R.id.tv_account_username);
            ImageButton btnDeleteAccount = suggestionItem.findViewById(R.id.btn_delete_account);
            tvUsername.setText(account.getUsername());

            // Handle normal click - fill the fields
            suggestionItem.setOnClickListener(v -> {
                etUsername.setText(account.getUsername());
                etPassword.setText(account.getPassword());
                cbRememberAccount.setChecked(true);
                dismissAccountSuggestionsPopup();
            });

            // Handle long click - show delete button
            suggestionItem.setOnLongClickListener(v -> {
                // Show the delete button
                btnDeleteAccount.setVisibility(View.VISIBLE);
                return true;
            });

            // Handle delete button click
            btnDeleteAccount.setOnClickListener(v -> {
                // Show confirmation dialog
                showDeleteAccountConfirmation(account.getUsername());
            });

            suggestionContainer.addView(suggestionItem);
        }

        // Show popup below the anchor view
        accountSuggestionsPopup.setOutsideTouchable(true);
        accountSuggestionsPopup.setElevation(10f);
        accountSuggestionsPopup.showAsDropDown(anchor, 0, 0, Gravity.TOP);
    }

    private void showDeleteAccountConfirmation(String username) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to remove \"" + username + "\" from saved accounts?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete the account from saved accounts
                    signInViewModel.deleteSavedAccount(username);

                    // Show confirmation toast
                    Toast.makeText(this, "Account removed from saved accounts", Toast.LENGTH_SHORT).show();

                    // Dismiss the popup
                    dismissAccountSuggestionsPopup();

                    // Clear the fields if they match the deleted account
                    if (etUsername.getText().toString().equals(username)) {
                        etUsername.setText("");
                        etPassword.setText("");
                        cbRememberAccount.setChecked(false);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void dismissAccountSuggestionsPopup() {
        if (accountSuggestionsPopup != null && accountSuggestionsPopup.isShowing()) {
            accountSuggestionsPopup.dismiss();
        }
    }

    private void setupObservers() {
        // Load saved accounts
        signInViewModel.loadSavedAccounts();

        // Observe changes to saved accounts
        signInViewModel.getSavedAccountsLiveData().observe(this, accounts -> {
            // If username field is not empty, check if its account was just deleted
            String currentUsername = etUsername.getText().toString();
            if (!currentUsername.isEmpty()) {
                boolean accountExists = false;
                for (AccountItem account : accounts) {
                    if (account.getUsername().equals(currentUsername)) {
                        accountExists = true;
                        break;
                    }
                }

                // Clear fields if account doesn't exist anymore
                if (!accountExists) {
                    etUsername.setText("");
                    etPassword.setText("");
                    cbRememberAccount.setChecked(false);
                }
            }
        });

        // Observe login success
        signInViewModel.getIsUserSignedIn().observe(this, isSignedIn -> {
            if (isSignedIn) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ConversationActivity.class));
                finish();
            }
        });

        // Observe need email verification
        signInViewModel.getNeedEmailVerification().observe(this, needVerification -> {
            if (needVerification) {
                showEmailVerificationDialog();
            }
        });

        // Observe error messages
        signInViewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupForgotPasswordListener() {
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordPopup());
    }
    private void showForgotPasswordPopup() {
        try {
            // Inflate the popup layout
            View popupView = getLayoutInflater().inflate(R.layout.popup_forgot_password, null);

            // Create the popup window with exact dimensions from the layout file
            int width = getResources().getDimensionPixelSize(R.dimen.popup_width);

            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    width,  // Use fixed width of 300dp
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            // Set elevation for shadow
            popupWindow.setElevation(10f);

            // Find views in the popup
            TextView tvCancel = popupView.findViewById(R.id.tv_cancel);

            // Handle cancel button click
            tvCancel.setOnClickListener(v -> popupWindow.dismiss());

            // Make the popup focusable to get key events
            popupWindow.setFocusable(true);

            // Set background to handle outside touches
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            // Show the popup centered in the screen
            View rootView = findViewById(android.R.id.content);
            popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);

            // Try to dim the background - in a safer way
            try {
                View container = (View) popupWindow.getContentView().getParent();
                if (container != null) {
                    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) container.getLayoutParams();
                    params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    params.dimAmount = 0.5f;
                    wm.updateViewLayout(container, params);
                }
            } catch (Exception e) {
                // Just log the error but don't crash
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing popup", Toast.LENGTH_SHORT).show();
        }
    }
    private void showEmailVerificationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_verification, null);
        TextView tvMessage = dialogView.findViewById(R.id.tv_verification_message);
        Button btnResend = dialogView.findViewById(R.id.btn_resend_verification);
        Button btnOk = dialogView.findViewById(R.id.btn_ok);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_resend);

        tvMessage.setText("Please verify your email before signing in. A verification email has been sent to your registered email address.");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        dialog.show();
        btnResend.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            signInViewModel.resendVerificationEmail();
        });
        btnOk.setOnClickListener(v -> dialog.dismiss());
        signInViewModel.getVerificationEmailResent().observe(this, isResent -> {
            if (isResent) {
                progressBar.setVisibility(View.GONE);
            }
        });
        signInViewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SignInActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
        signInViewModel.getNeedEmailVerification().observe(this, needVerification -> {
            if (!needVerification) {
                dialog.dismiss();
            }
        });
    }
}