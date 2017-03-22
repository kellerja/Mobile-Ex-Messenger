package com.example.messenger;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class LoginFragment extends Fragment {

    private Button loginButton;
    private EditText emailField;
    private EditText passwordField;

    private FirebaseAuth mAuth;

    private static final String TAG = "LoginFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_fragment, container, false);

        loginButton = (Button) view.findViewById(R.id.login_button);
        emailField = (EditText) view.findViewById(R.id.login_email);
        passwordField = (EditText) view.findViewById(R.id.login_password);

        mAuth = FirebaseAuth.getInstance();

        setLoginButtonHandler();

        return view;
    }

    private void setLoginButtonHandler() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoginConditionsFulfilled()) {
                    signIn(emailField.getText().toString(), passwordField.getText().toString());
                }
            }
        });
    }

    private boolean isLoginConditionsFulfilled() {
        return !emailField.getText().toString().isEmpty() && !passwordField.getText().toString().isEmpty();
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(getActivity(), R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        changeActivity();
                    }
                });
    }

    private void changeActivity() {
        Intent intent = new Intent(getActivity(), TopicsActivity.class);
        startActivity(intent);
        getActivity().finish();
    }
}
