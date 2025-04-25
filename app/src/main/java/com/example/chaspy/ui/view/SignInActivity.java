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
import com.example.chaspy.ui.viewmodel.SignInViewModel;
import com.example.chaspy.R;

public class SignInActivity extends AppCompatActivity {

    private SignInViewModel signInViewModel;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        progressBar = findViewById(R.id.progressBar);

        signInViewModel = new ViewModelProvider(this).get(SignInViewModel.class);

        // Observe the sign-in status
        signInViewModel.getIsUserSignedIn().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSignedIn) {
                if (isSignedIn != null && isSignedIn) {
                    Toast.makeText(SignInActivity.this, "User signed in successfully", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    // Navigate to another activity or home screen
                    // For example:
                    // startActivity(new Intent(SignInActivity.this, HomeActivity.class));
                    // finish();
                }
            }
        });

        // Observe the error message
        signInViewModel.getErrorMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Sign in button click listener
    public void onSignInClick(View view) {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);

        signInViewModel.signInUser(email, password);
    }

    public void switchToSignUpActivity(View view) {
        // Use Intent to switch to SignUpActivity
        Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(intent);
    }
}
