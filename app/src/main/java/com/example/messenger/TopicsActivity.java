package com.example.messenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.messenger.database.Topic;
import com.example.messenger.helper.Utils;
import com.example.messenger.view.TopicView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public class TopicsActivity extends AppCompatActivity {

    private static final String TAG = "TopicsActivity";
    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private LinearLayout content;
    private Button searchButton;
    private EditText searchField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topics);

        Utils.markAsLastView(this);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("topics");

        content = (LinearLayout) findViewById(R.id.topics_content);
        searchButton = (Button) findViewById(R.id.topics_search_button);
        searchField = (EditText) findViewById(R.id.topics_search_input);

        receiveTopicsAndPopulateData();
    }

    public void search(View view) {
        myRef.orderByChild("name").startAt(searchField.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                receiveCurrentTimeAndPopulateContent(dataSnapshot);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void receiveTopicsAndPopulateData() {
        myRef.orderByChild("priority").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshotTopics) {
                receiveCurrentTimeAndPopulateContent(dataSnapshotTopics);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void receiveCurrentTimeAndPopulateContent(final DataSnapshot dataSnapshotTopics) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final DatabaseReference timeRef = database.getReference(user != null ? "users/" + user.getUid() : "temp");
        timeRef.child("currentTime").setValue(ServerValue.TIMESTAMP);
        timeRef.child("currentTime").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshotTime) {
                timeRef.child("currentTime").removeValue();
                Long currentTime = (Long) dataSnapshotTime.getValue();
                content.removeAllViewsInLayout();
                for (DataSnapshot topicSnapshot: dataSnapshotTopics.getChildren()) {
                    final Topic topic = topicSnapshot.getValue(Topic.class);
                    if (topic.getUserUid() != null && topic.getLastMessageTimestamp() != null &&
                            topic.getLastMessageTimestamp() + topic.getMessageSaveDuration() < currentTime) {
                        myRef.child(topic.getName()).removeValue();
                    } else {
                        removeMessagesWhenLastMessageOld(currentTime, topic);
                        populateContent(topic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void removeMessagesWhenLastMessageOld(Long currentTime, final Topic topic) {
        if (isTopicMessagesOld(currentTime, topic)) {
            myRef.child(topic.getName()).child("messages").removeValue();
            myRef.child(topic.getName()).child("lastMessageTimestamp").removeValue();
        }
    }

    private boolean isTopicMessagesOld(Long currentTime, Topic topic) {
        return topic.getLastMessageTimestamp() != null && topic.getMessageSaveDuration() != null &&
                topic.getActiveUsers() == null &&
                topic.getLastMessageTimestamp() + topic.getMessageSaveDuration() < currentTime;
    }

    private void populateContent(final Topic topic) {
        TopicView newTopic = new TopicView(this, topic);
        content.addView(newTopic);
        newTopic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChatWithTopic(topic);
            }
        });
    }

    private void openChatWithTopic(Topic topic) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("topic", topic.getName());
        startActivity(intent);
    }

}
