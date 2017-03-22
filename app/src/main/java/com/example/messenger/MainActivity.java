package com.example.messenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = getAuthStateListener();

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            Fragment initialFragment = new LoginFragment();
            initialFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, initialFragment).commit();
        }
    }

    @NonNull
    private FirebaseAuth.AuthStateListener getAuthStateListener() {
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    loggedIn();
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    private void loggedIn() {
        Intent intent = new Intent(getBaseContext(), getLastView());
        startActivity(intent);
        finish();
    }

    private Class<?> getLastView() {
        SharedPreferences prefs = getSharedPreferences("lastView", MODE_PRIVATE);
        String lastView = prefs.getString("lastView", "");
        Log.w("!!!!", lastView + " ");
        switch (lastView) {
            case "ChatActivity":
                return ChatActivity.class;
            case "CreateTopicActivity":
                return CreateTopicActivity.class;
            case "ProfileActivity":
                return ProfileActivity.class;
            case "TopicsActivity":
                return TopicsActivity.class;
            default:
                return TopicsActivity.class;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth != null) {
            mAuth.addAuthStateListener(mAuthListener);
        } else {
            mAuth = FirebaseAuth.getInstance();
            mAuthListener = getAuthStateListener();
            mAuth.addAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void handleLoginButtonSwitchClick(View view) {
        changeFragmentTo(new LoginFragment());
    }

    public void handleRegisterButtonSwitchClick(View view) {
        changeFragmentTo(new RegisterFragment());
    }

    private void changeFragmentTo(Fragment newFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.commit();
    }

}
