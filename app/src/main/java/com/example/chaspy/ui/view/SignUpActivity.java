package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;
import com.example.chaspy.ui.viewmodel.SignUpViewModel;
import com.example.chaspy.R;

public class SignUpActivity extends AppCompatActivity {

    private SignUpViewModel signUpViewModel;
    private EditText emailEditText, passwordEditText, firstNameEditText, lastNameEditText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        progressBar = findViewById(R.id.progressBar);

        signUpViewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

        // Observe the registration result
        signUpViewModel.getIsUserRegistered().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isRegistered) {
                if (isRegistered != null && isRegistered) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SignUpActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                    // Navigate to another screen or finish the activity
                }
            }
        });

        signUpViewModel.getErrorMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Sign up button click listener
    public void onSignUpClick(View view) {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);

        signUpViewModel.registerUser(email, password, firstName, lastName);
    }

    public void switchToSignInActivity(View view) {
        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
        startActivity(intent);
    }
}
