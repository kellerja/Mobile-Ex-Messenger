package com.example.messenger.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.messenger.R;
import com.example.messenger.database.Topic;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.concurrent.RejectedExecutionException;

import static com.example.messenger.helper.Utils.getDpAsPixels;

public class TopicView extends GridLayout {

    private TextView topicName;
    private TextView authorName;
    private ImageView imageView;
    private TextView userCountView;
    private TextView lastMessageTimeView;

    public TopicView(Context context) {
        super(context);
    }

    public TopicView(Context context, Topic topic) {
        super(context);

        String topicNameText = topic.getName();
        String userUid = topic.getUserUid();
        String imageURL = topic.getImage();

        setTopicView();

        setImageView(imageURL);
        setTopicNameTextView(topicNameText);
        setUserTextView(userUid);
        setUserCountTextView(topic);
        setLastMessageTimeView(topic);

        addView(imageView);
        addView(topicName);
        addView(authorName);
        addView(userCountView);
        addView(lastMessageTimeView);
    }

    private void setLastMessageTimeView(Topic topic) {
        String lastMessageTimeViewMessage = topic.getLastMessageTimestamp() != null ? "Last message: " + SimpleDateFormat.getDateTimeInstance().format(topic.getLastMessageTimestamp()) : "No messages";
        LayoutParams lastMessageTimeViewParams = new LayoutParams(
                GridLayout.spec(1),
                GridLayout.spec(2)
        );
        lastMessageTimeViewParams.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        lastMessageTimeView = new TextView(getContext());
        lastMessageTimeView.setLayoutParams(lastMessageTimeViewParams);
        lastMessageTimeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        lastMessageTimeView.setText(lastMessageTimeViewMessage);
    }

    private void setUserCountTextView(Topic topic) {
        Integer activeUsersCount = topic.getActiveUsers() == null ? 0 : topic.getActiveUsers().size();
        Integer usersCount = topic.getUsers() == null ? 0 : topic.getUsers().size();
        String userCountViewMessage = activeUsersCount.toString() + " of " + usersCount.toString() + " users active.";
        LayoutParams userCountViewParams = new LayoutParams(
                GridLayout.spec(1),
                GridLayout.spec(1)
        );
        userCountViewParams.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        userCountView = new TextView(getContext());
        userCountView.setLayoutParams(userCountViewParams);
        userCountView.setText(userCountViewMessage);
        userCountView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    }

    private void setTopicView() {
        LinearLayout.LayoutParams topicViewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        setLayoutParams(topicViewParams);
        setColumnCount(3);
        setRowCount(2);
    }

    private void setImageView(String imageURL) {
        LayoutParams imageViewParams = new LayoutParams(
                GridLayout.spec(0, 2),
                GridLayout.spec(0)
        );
        imageViewParams.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        imageViewParams.width = getDpAsPixels(getContext(), 50);
        imageViewParams.height = getDpAsPixels(getContext(), 50);
        imageView = new ImageView(getContext());
        imageView.setLayoutParams(imageViewParams);

        if (imageURL == null) {
            imageView.setImageResource(R.drawable.ic_add_a_photo_black_24dp);
            return;
        }
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imageURL);
        final long ONE_MEBIBYTE = 1024 * 1024;
        try {
            imageRef.getBytes(ONE_MEBIBYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imageView.setImageBitmap(bitmap);
                }
            });
        } catch (RejectedExecutionException e) {
        }
    }

    private void setUserTextView(String userUid) {
        LayoutParams authorNameParams = new LayoutParams(
                GridLayout.spec(0),
                GridLayout.spec(2)
        );
        authorNameParams.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
        authorName = new TextView(getContext());
        authorName.setText(userUid);
        authorName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        authorName.setLayoutParams(authorNameParams);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users/" + userUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("name").getValue() != null) {
                    String authorText = "By: " + dataSnapshot.child("name").getValue();
                    authorName.setText(authorText);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void setTopicNameTextView(String topicNameText) {
        LayoutParams topicNameParams = new LayoutParams(
                GridLayout.spec(0),
                GridLayout.spec(1)
        );
        topicNameParams.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        topicName = new TextView(getContext());
        topicName.setText(topicNameText);
        topicName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        topicName.setLayoutParams(topicNameParams);
    }
}
