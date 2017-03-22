package com.example.messenger;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.messenger.database.Message;
import com.example.messenger.database.Topic;
import com.example.messenger.helper.Utils;
import com.example.messenger.view.MessageView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;


public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private Button sendButton;
    private EditText message;
    private LinearLayout content;
    private ScrollView scrollView;
    private Button imageButton;
    private ImageView imageView;
    private Button imageCancelButton;

    private int messageType;

    private FirebaseDatabase database;
    private DatabaseReference messagesRef;
    private DatabaseReference topicRef;

    private static final int REQUEST_CAMERA = 1;
    private static final int SELECT_FILE = 2;

    private static final int REQUEST_STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setTopicName(getTopicName());

        Utils.markAsLastView(this);

        messageType = Message.TEXT;

        sendButton = (Button) findViewById(R.id.chat_send_button);
        message = (EditText) findViewById(R.id.chat_input_box);
        content = (LinearLayout) findViewById(R.id.chat_content);
        scrollView = (ScrollView) findViewById(R.id.chat_scroll_view);
        imageButton = (Button)findViewById(R.id.chat_image_button);
        imageView = (ImageView) findViewById(R.id.chat_input_image);
        imageCancelButton = (Button) findViewById(R.id.chat_image_cancel_button);

        database = FirebaseDatabase.getInstance();

        final String topic = getTopicName();
        messagesRef = database.getReference("topics/" + topic + "/messages");
        topicRef = database.getReference("topics/" + topic);

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            topicRef.orderByChild("name").equalTo(topic).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getKey() == null) {
                        database.getReference("topics/default/users/" + user.getUid()).setValue(1);
                        database.getReference("topics/default/activeUsers/" + user.getUid()).setValue(1);
                    } else {
                        database.getReference("topics/" + topic + "/users/" + user.getUid()).setValue(1);
                        database.getReference("topics/" + topic + "/activeUsers/" + user.getUid()).setValue(1);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(message.getText().toString());
                message.setText("");
            }
        });

        database.getReference("topics").orderByChild("name").equalTo(topic).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getChildrenCount() != 0) {
                    receiveMessageDurationAndPopulateContent(user);
                } else {
                    setTopicName("default");
                    messagesRef = database.getReference("topics/default/messages");
                    topicRef = database.getReference("topics/default");
                    receiveMessageDurationAndPopulateContent(user);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void setTopicName(String topicName) {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("topicName", topicName);
        editor.apply();
    }

    private void receiveMessageDurationAndPopulateContent(final FirebaseUser user) {
        topicRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshotTopic) {
                final Long messageDuration = (Long) dataSnapshotTopic.child("messageSaveDuration").getValue();
                receiveMessagesAndPopulateContent(messageDuration, user);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void receiveMessagesAndPopulateContent(final Long messageDuration, final FirebaseUser user) {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshotMessage) {
                receiveCurrentTimeAndPopulateContent(dataSnapshotMessage, user, messageDuration);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void receiveCurrentTimeAndPopulateContent(final DataSnapshot dataSnapshotMessage, FirebaseUser user, final Long messageDuration) {
        final DatabaseReference timeRef = database.getReference("users/" + (user != null ? user.getUid() : "temp"));
        timeRef.child("currentTime").setValue(ServerValue.TIMESTAMP);
        timeRef.child("currentTime").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshotTime) {
                Long currentTime = (Long) dataSnapshotTime.getValue();
                timeRef.child("currentTime").removeValue();
                content.removeAllViewsInLayout();
                for (DataSnapshot messageSnapShot: dataSnapshotMessage.getChildren()) {
                    Message message = messageSnapShot.getValue(Message.class);
                    if (message.getTimestampLong() != null && messageDuration != null &&
                            message.getTimestampLong() + messageDuration < currentTime) {
                        messagesRef.child(messageSnapShot.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                changeTopicPriorityBy(1);
                            }
                        });
                    } else {
                        populateContent(message, messageDuration, currentTime);
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private String getTopicName() {
        Intent intent = getIntent();
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String topic = preferences.getString("topicName", "default");
        if (intent != null && intent.getExtras() != null && intent.getExtras().getString("topic") != null) {
            topic = intent.getExtras().getString("topic");
        }
        return topic;
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference activeUsersRef = database.getReference("topics/" + getTopicName() + "/activeUsers");
        if (user != null) {
            activeUsersRef.child(user.getUid()).removeValue();
        }
    }

    private void populateContent(final Message message, final Long duration, final Long currentTime) {
        final MessageView messageView = new MessageView(content.getContext(), message);
        content.addView(messageView);
        final Handler handler = new Handler();
        final long stopTime = message.getTimestampLong() + duration;
        final long timeTick = 1000;
        final AtomicLong time = new AtomicLong(currentTime);
        messageView.setAlpha(1 - (currentTime - message.getTimestampLong()) / duration.floatValue());
        final float alphaTick = messageView.getAlpha() / ((stopTime - currentTime) / timeTick);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (stopTime - time.longValue() > 0) {
                    fade(alphaTick, messageView);
                    time.addAndGet(timeTick);
                    handler.postDelayed(this, timeTick);
                } else {
                    messageView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void fade(float alphaTick, MessageView messageView) {
        if (messageView.getAlpha() - alphaTick > 0) {
            messageView.setAlpha(messageView.getAlpha() - alphaTick);
        }
    }

    private void sendMessage(String message) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userIdentifier = user != null ? user.getUid() : "";
        Message messageContainer = new Message(userIdentifier, message);
        if (messageType == Message.IMAGE) {
            String imageLocation = "images/topic/" + getTopicName() + "/chat/" + userIdentifier + new Random().nextInt() + ".jpg";
            messageContainer.setType(messageType);
            messageContainer.setMessage(imageLocation);

            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference myRef = storageRef.child(imageLocation);

            imageView.setDrawingCacheEnabled(true);
            imageView.buildDrawingCache();
            Bitmap bitmap = imageView.getDrawingCache();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            myRef.putBytes(data);

            imageView.setImageResource(R.drawable.ic_add_a_photo_black_24dp);
        }
        messagesRef.push().setValue(messageContainer);
        topicRef.child("lastMessageTimestamp").setValue(ServerValue.TIMESTAMP);
        changeTopicPriorityBy(-1);
    }

    private void changeTopicPriorityBy(final int value) {
        topicRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Topic topic = dataSnapshot.getValue(Topic.class);
                if (topic != null && topic.getUserUid() != null && topic.getPriority() + value > 0) {
                    topicRef.child("priority").setValue(topic.getPriority() + value);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void changeToImageSending(View view) {
        messageType = Message.IMAGE;
        message.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        imageCancelButton.setVisibility(View.VISIBLE);
        chooseImage();
    }

    public void changeToMessageSending(View view) {
        messageType = Message.TEXT;
        imageView.setVisibility(View.GONE);
        imageCancelButton.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
    }

    private void chooseImage() {
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
                        if (ContextCompat.checkSelfPermission(ChatActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(ChatActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
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
