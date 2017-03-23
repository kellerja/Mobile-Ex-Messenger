package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.example.messenger.helper.Utils.addUserToDb;

public class RegisterFragment extends Fragment {

    private Button registerButton;
    private EditText emailField;
    private EditText displayNameField;
    private EditText passwordField;
    private EditText passwordAgainField;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private static final String TAG = "RegisterFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register_fragment, container, false);

        registerButton = (Button) view.findViewById(R.id.register_button);
        emailField = (EditText) view.findViewById(R.id.register_email);
        displayNameField = (EditText) view.findViewById(R.id.register_display_name);
        passwordField = (EditText) view.findViewById(R.id.register_password);
        passwordAgainField = (EditText) view.findViewById(R.id.register_password_again);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        setRegisterButtonHandler();

        return view;
    }

    private void setRegisterButtonHandler() {
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().toString().isEmpty()) {
                    Toast.makeText(getActivity(), R.string.empty_email_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (passwordField.getText().toString().isEmpty()) {
                    Toast.makeText(getActivity(), R.string.empty_password_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!passwordField.getText().toString().equals(passwordAgainField.getText().toString())) {
                    Toast.makeText(getActivity(), R.string.password_mismatch_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                createAccount(emailField.getText().toString(), passwordField.getText().toString());
            }
        });
    }

    private void createAccount(final String email, final String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Toast.makeText(getActivity(), R.string.create_account_failed,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            FirebaseUser user = task.getResult().getUser();
                            updateUserInfo(user);
                            signIn(email, password);
                        }
                    }
                });
    }

    private void updateUserInfo(FirebaseUser user) {
        String displayName = displayNameField.getText().toString().isEmpty() ?
                "Member" + new Random().nextInt() : displayNameField.getText().toString();
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();
        user.updateProfile(profileChangeRequest);
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("name", displayName);
        usersRef.child(user.getUid()).setValue(userInfo);
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
                        addUserToDb(task.getResult().getUser());
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
