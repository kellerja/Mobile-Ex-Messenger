package com.example.messenger.helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;

public class Utils {

    public static int getDpAsPixels(Context context, int dps) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps, context.getResources().getDisplayMetrics());
    }

    public static void addUserToDb(FirebaseUser user) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        HashMap<String, String> info = new HashMap<>();
        info.put("name", user.getDisplayName());
        info.put("image", user.getPhotoUrl() == null ? null : user.getPhotoUrl().getPath());
        info.put("email", user.getEmail());
        userRef.setValue(info);
    }

    public static void markAsLastView(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(DefaultValues.SHARED_PREFERENCES_LAST_VIEW, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DefaultValues.SHARED_PREFERENCES_LAST_VIEW, activity.getClass().getSimpleName());
        editor.apply();
    }
}
