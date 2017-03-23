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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.messenger.database.Topic;
import com.example.messenger.helper.DefaultValues;
import com.example.messenger.helper.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class CreateTopicActivity extends AppCompatActivity {

    private EditText topicNameField;
    private ImageView topicImageField;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private StorageReference storageRef;

    private static final int REQUEST_CAMERA = 1;
    private static final int SELECT_FILE = 2;

    private static final int REQUEST_STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_topic);

        Utils.markAsLastView(this);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("topics");
        storageRef = FirebaseStorage.getInstance().getReference();

        topicNameField = (EditText) findViewById(R.id.new_topic_topic_name);
        topicImageField = (ImageView) findViewById(R.id.new_topic_image_view);
    }

    public void createNewTopic(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final Topic topic = new Topic(
                topicNameField.getText().toString(),
                user != null ? user.getUid() : ""
        );
        topic.setMessageSaveDuration(DefaultValues.NEW_MESSAGE_DISPLAY_DURATION_IN_MILLIS);
        if (isTopicAcceptable(topic)) {
            addOrJoin(topic);
        } else {
            notifyMistakes(topic);
        }
    }

    private void addOrJoin(final Topic topic) {
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isTopicUnique(topic, dataSnapshot)) {
                    createTopic(topic);
                    joinTopic(topic);
                } else {
                    askToJoinTopic(topic);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getBaseContext(), "Error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void askToJoinTopic(final Topic topic) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Topic exists!").setMessage("Do you want to join it?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                joinTopic(topic);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void joinTopic(Topic topic) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("topic", topic.getName());
        startActivity(intent);
    }

    private void createTopic(Topic topic) {
        myRef.child(topic.getName()).setValue(topic);
        uploadImage(topic);
    }

    private boolean isTopicUnique(Topic topic, DataSnapshot dataSnapshot) {
        for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
            if (topic.getName().equals(childSnapshot.getKey())) return false;
        }
        return true;
    }

    public void uploadImage(Topic topic) {
        StorageReference myRef = storageRef.child(topic.getImage());

        topicImageField.setDrawingCacheEnabled(true);
        topicImageField.buildDrawingCache();
        Bitmap bitmap = topicImageField.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        myRef.putBytes(data);
    }

    private void notifyMistakes(Topic topic) {
        if (topic == null) {
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
            return;
        }
        if (topic.getName() == null || topic.getName().trim().isEmpty()) {
            Toast.makeText(this, "Topic must have a name", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isTopicAcceptable(Topic topic) {
        return topic != null && topic.getName() != null && !topic.getName().trim().isEmpty();
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
                        if (ContextCompat.checkSelfPermission(CreateTopicActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(CreateTopicActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
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
                topicImageField.setImageBitmap(image);
            } else if (requestCode == SELECT_FILE) {
                Uri selectedImageUri = data.getData();
                String imagePath = getRealPathFromUri(selectedImageUri);
                Bitmap image = BitmapFactory.decodeFile(imagePath);
                topicImageField.setImageBitmap(image);
            }
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
