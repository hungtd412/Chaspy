package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.chaspy.R;
import com.example.chaspy.ui.viewmodel.SignUpViewModel;

public class SignUpActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private SignUpViewModel signUpViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize ViewModel
        signUpViewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

        // Liên kết với layout
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignUp = findViewById(R.id.btn_sign_up);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progressBar);

        // Set up observers
        setupObservers();

        btnSignUp.setOnClickListener(view -> {
            // Get input values
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);

            // Call ViewModel to handle validation and registration
            signUpViewModel.registerUser(email, password, confirmPassword, firstName, lastName);
        });

        // Set click listener for login text to navigate to SignInActivity
        tvLogin.setOnClickListener(view -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish(); // Close current activity
        });
    }

    private void setupObservers() {
        // Observe registration success
        signUpViewModel.getIsUserRegistered().observe(this, isRegistered -> {
            progressBar.setVisibility(View.GONE);
            if (isRegistered) {
                showVerificationDialog();
            }
        });
        
        // Observe verification email sent status
        signUpViewModel.getVerificationEmailSent().observe(this, isEmailSent -> {
            if (isEmailSent) {
                Toast.makeText(SignUpActivity.this, "Verification email sent", Toast.LENGTH_SHORT).show();
            }
        });

        // Observe validation errors
        signUpViewModel.getValidationErrors().observe(this, errorPair -> {
            if (errorPair != null) {
                // Apply error to specific field
                switch (errorPair.first) {
                    case "firstName":
                        etFirstName.setError(errorPair.second);
                        etFirstName.requestFocus();
                        break;
                    case "lastName":
                        etLastName.setError(errorPair.second);
                        etLastName.requestFocus();
                        break;
                    case "email":
                        etEmail.setError(errorPair.second);
                        etEmail.requestFocus();
                        break;
                    case "password":
                        etPassword.setError(errorPair.second);
                        etPassword.requestFocus();
                        break;
                    case "confirmPassword":
                        etConfirmPassword.setError(errorPair.second);
                        etConfirmPassword.requestFocus();
                        break;
                }
                progressBar.setVisibility(View.GONE);
            }
        });

        // Observe general error messages
        signUpViewModel.getErrorMessage().observe(this, errorMsg -> {
            progressBar.setVisibility(View.GONE);
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(SignUpActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void showVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Your Email");
        builder.setMessage("A verification email has been sent to your email address. " +
                "Please verify your email to complete registration.\n\n" +
                "You can sign in after verifying your email.");
        builder.setPositiveButton("Go to Sign In", (dialog, which) -> {
            dialog.dismiss();
            // Navigate to sign in screen
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish(); // Close current activity
        });
        builder.setCancelable(false);
        builder.show();
    }
}
