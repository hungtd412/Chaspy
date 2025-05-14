package com.example.chaspy.ui.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.chaspy.R;
import com.example.chaspy.ui.viewmodel.SignUpViewModel;

public class SignUpActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etUsername, etPassword;
    private Button btnSignUp;
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
        etUsername = findViewById(R.id.et_email); // Assuming you'll update the ID later
        etPassword = findViewById(R.id.et_password);
        btnSignUp = findViewById(R.id.btn_sign_up);
        progressBar = findViewById(R.id.progressBar);

        // Set up observers
        setupObservers();

        btnSignUp.setOnClickListener(view -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);
            
            // Call ViewModel to handle registration
            signUpViewModel.registerUser(username, password, firstName, lastName);
        });
    }
    
    private void setupObservers() {
        // ... existing code ...
    }
}
