package com.example.messenger;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.messenger.helper.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Random;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView displayNameView;
    private EditText editDisplayNameView;
    private TextView emailView;
    private TextView oldPasswordMessageView;
    private EditText editOldPasswordView;
    private Button editPasswordButton;
    private TextView passwordView;
    private EditText editPasswordView;
    private TextView passwordAgainMessageView;
    private EditText editPasswordAgainView;
    private Button editModeButton;
    private Button saveButton;
    private Button cancelButton;

    private FirebaseUser user;

    private static final int REQUEST_CAMERA = 1;
    private static final int SELECT_FILE = 2;

    private static final int REQUEST_STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Utils.markAsLastView(this);

        user = FirebaseAuth.getInstance().getCurrentUser();

        imageView = (ImageView) findViewById(R.id.profile_image);
        displayNameView = (TextView) findViewById(R.id.profile_display_name);
        editDisplayNameView = (EditText) findViewById(R.id.profile_edit_display_name);
        emailView = (TextView) findViewById(R.id.profile_email);
        oldPasswordMessageView = (TextView) findViewById(R.id.profile_old_password_message);
        editOldPasswordView = (EditText) findViewById(R.id.profile_edit_old_password);
        editPasswordButton = (Button) findViewById(R.id.profile_edit_password_button);
        passwordView = (TextView) findViewById(R.id.profile_password);
        editPasswordView = (EditText) findViewById(R.id.profile_edit_password);
        passwordAgainMessageView = (TextView) findViewById(R.id.profile_password_again);
        editPasswordAgainView = (EditText) findViewById(R.id.profile_edit_password_again);
        editModeButton = (Button) findViewById(R.id.profile_edit_button);
        saveButton = (Button) findViewById(R.id.profile_save_button);
        cancelButton = (Button) findViewById(R.id.profile_edit_cancel);

        if (user != null) {
            emailView.setText(user.getEmail());
            if (user.getPhotoUrl() != null) {
                StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(user.getPhotoUrl().toString());
                final long ONE_MEBIBYTE = 1024 * 1024;
                imageRef.getBytes(ONE_MEBIBYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
            displayNameView.setText(user.getDisplayName());
        }
    }

    public void toggleProfile(View view) {
        boolean isModeEdit = displayNameView.getVisibility() == View.VISIBLE;
        displayNameView.setVisibility(isModeEdit ? View.GONE : View.VISIBLE);
        editDisplayNameView.setVisibility(isModeEdit ? View.VISIBLE : View.GONE);
        passwordView.setVisibility(isModeEdit ? View.GONE : View.VISIBLE);
        editPasswordView.setVisibility(View.GONE);
        passwordAgainMessageView.setVisibility(View.GONE);
        editPasswordAgainView.setVisibility(View.GONE);
        editPasswordButton.setVisibility(isModeEdit ? View.VISIBLE : View.GONE);
        editModeButton.setVisibility(isModeEdit ? View.GONE : View.VISIBLE);
        cancelButton.setVisibility(isModeEdit ? View.VISIBLE : View.GONE);
        saveButton.setVisibility(isModeEdit ? View.VISIBLE : View.GONE);
        oldPasswordMessageView.setVisibility(View.GONE);
        editOldPasswordView.setVisibility(View.GONE);
    }

    public void commit(View view) {
        if (user == null) {
            return;
        }
        final String displayName = editDisplayNameView.getText().toString();
        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.isEmpty() ?
                        (user.getDisplayName() == null || user.getDisplayName().isEmpty() ?
                                "Member" + new Random().nextInt() : user.getDisplayName()) :
                        displayName).build();
        user.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users/" + user.getUid() + "/name");
                    userRef.setValue(displayName);
                    displayNameView.setText(displayName);
                } else {
                    Toast.makeText(getBaseContext(), "Failed to update display name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (editPasswordButton.getVisibility() == View.GONE) {
            final String password = editPasswordView.getText().toString();
            String passwordAgain = editPasswordAgainView.getText().toString();
            String oldPassword = editOldPasswordView.getText().toString();
            if (isPasswordAcceptable(password, passwordAgain)) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
                user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        user.updatePassword(password).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getBaseContext(), "Password change failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            } else {
                identifyPasswordMistakes(password, passwordAgain);
            }
        }
        toggleProfile(view);
    }

    public void logOut(View view) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private boolean isPasswordAcceptable(String password, String passwordAgain) {
        return !password.isEmpty() && password.equals(passwordAgain);
    }

    private void identifyPasswordMistakes(String password, String passwordAgain) {
        if (password.isEmpty()) {
            Toast.makeText(this, "Password must not be empty", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(passwordAgain)) {
            Toast.makeText(this, "Password and password again must match", Toast.LENGTH_SHORT).show();
        }
    }

    public void showPasswordEdit(View view) {
        oldPasswordMessageView.setVisibility(View.VISIBLE);
        editOldPasswordView.setVisibility(View.VISIBLE);
        editPasswordButton.setVisibility(View.GONE);
        editPasswordView.setVisibility(View.VISIBLE);
        passwordAgainMessageView.setVisibility(View.VISIBLE);
        editPasswordAgainView.setVisibility(View.VISIBLE);
    }

    public void uploadImage() {
        StorageReference myRef = FirebaseStorage.getInstance().getReference().child("images/users/" + user.getUid() + ".jpg");

        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = imageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = myRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getBaseContext(), "Upload failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUrl).build();
                user.updateProfile(userProfileChangeRequest);
            }
        });
    }

    public void chooseImage(View view) {
        final String[] items = {"Take Photo", "Choose from Library", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (items[which]) {
                    case "Take Photo":
                        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(takePhotoIntent, REQUEST_CAMERA);
                        break;
                    case "Choose from Library":
                        if (ContextCompat.checkSelfPermission(ProfileActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_STORAGE_PERMISSION);
                            dialog.dismiss();
                        } else {
                            openPhotoSelect();
                        }
                        break;
                    default:
                        dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openPhotoSelect();
            }
        }
    }

    private void openPhotoSelect() {
        Intent choosePhotoIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        choosePhotoIntent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(choosePhotoIntent, "Select file"),
                SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                Bitmap image = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(image);
            } else if (requestCode == SELECT_FILE) {
                Uri selectedImageUri = data.getData();
                String imagePath = getRealPathFromUri(selectedImageUri);
                Bitmap image = BitmapFactory.decodeFile(imagePath);
                imageView.setImageBitmap(image);
            }
            uploadImage();
        }
    }

    private String getRealPathFromUri(Uri contentUri) {
        String selectedImagePath = null;
        String[] projection = { MediaStore.MediaColumns.DATA };
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            selectedImagePath = cursor.getString(column_index);
        }
        cursor.close();
        return selectedImagePath;
    }
}
