package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.chaspy.R;
import com.example.chaspy.ui.viewmodel.SignUpViewModel;

public class SignUpActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etUsername, etPassword, etConfirmPassword;
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
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignUp = findViewById(R.id.btn_sign_up);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progressBar);

        // Set up observers
        setupObservers();

        btnSignUp.setOnClickListener(view -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Check if passwords match
            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);

            String email = username + "@gmail.com";

            // Call ViewModel to handle registration
            signUpViewModel.registerUser(email, password, firstName, lastName);
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
                Toast.makeText(SignUpActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                finish(); // Close activity and return to login screen
            }
        });

        // Observe error messages
        signUpViewModel.getErrorMessage().observe(this, errorMsg -> {
            progressBar.setVisibility(View.GONE);
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(SignUpActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}