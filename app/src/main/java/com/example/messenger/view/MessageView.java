package com.example.messenger.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.messenger.database.Message;
import com.example.messenger.helper.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.concurrent.RejectedExecutionException;


public class MessageView extends LinearLayout {

    private LinearLayout messageHolder;
    private TextView userInfo;
    private View separator;
    private TextView message;
    private ImageView image;
    private View bottomSeparator;

    public MessageView(Context context) {
        super(context);
    }

    public MessageView(Context context, Message message) {
        super(context);

        String userUid = message.getUser();
        String messageText = message.getMessage();

        setMessageView();

        ViewGroup.LayoutParams textViewParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        setUserTextView(userUid, textViewParams);

        setSeparator();

        if (message.getType() == Message.IMAGE) {
            ViewGroup.LayoutParams imageParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            image = new ImageView(getContext());
            image.setLayoutParams(imageParams);

            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(message.getMessage());
            final long ONE_MEBIBYTE = 1024 * 1024;
            try {
                imageRef.getBytes(ONE_MEBIBYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        image.setImageBitmap(bitmap);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
            } catch (RejectedExecutionException e) {
            }

        } else {
            setMessageTextView(messageText, textViewParams);
        }

        setBottomSeparator();

        setMessageHolderLinearLayout(message.getType() == Message.IMAGE ? image : this.message);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(userUid)) {
            LinearLayout.LayoutParams alignTextToRightParams = new LinearLayout.LayoutParams(
                    0,
                    0,
                    1
            );
            View alignTextToRight = new View(getContext());
            alignTextToRight.setLayoutParams(alignTextToRightParams);
            addView(alignTextToRight);
        }

        addView(messageHolder);
    }

    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        setVisibility(GONE);
    }

    private void setMessageView() {
        setOrientation(LinearLayout.HORIZONTAL);
        setWeightSum(2);
        LayoutParams backgroundParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        setLayoutParams(backgroundParams);
    }

    private void setMessageHolderLinearLayout(View content) {
        LayoutParams messageHolderParams = new LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        messageHolder = new LinearLayout(getContext());
        messageHolder.setOrientation(LinearLayout.VERTICAL);
        messageHolder.setLayoutParams(messageHolderParams);
        messageHolder.setBackgroundColor(Color.BLUE);
        messageHolder.addView(userInfo);
        messageHolder.addView(separator);
        messageHolder.addView(content);
        messageHolder.addView(bottomSeparator);
    }

    private void setBottomSeparator() {
        LayoutParams bottomSeparatorParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.getDpAsPixels(getContext(), 3)
        );
        bottomSeparator = new View(getContext());
        bottomSeparator.setBackgroundColor(Color.rgb(170, 170, 170));
        bottomSeparator.setLayoutParams(bottomSeparatorParams);
    }

    private void setMessageTextView(String messageText, ViewGroup.LayoutParams textViewParams) {
        message = new TextView(getContext());
        message.setText(messageText);
        message.setTextColor(Color.WHITE);
        message.setLayoutParams(textViewParams);
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    }

    private void setSeparator() {
        ViewGroup.LayoutParams separatorParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.getDpAsPixels(getContext(), 1)
        );
        separator = new View(getContext());
        separator.setLayoutParams(separatorParams);
        separator.setBackgroundColor(Color.BLACK);
    }

    private void setUserTextView(String userUid, ViewGroup.LayoutParams textViewParams) {
        userInfo = new TextView(getContext());
        userInfo.setLayoutParams(textViewParams);
        userInfo.setText(userUid);
        userInfo.setTextColor(Color.WHITE);
        userInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users/" + userUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userInfo.setText((String) dataSnapshot.child("name").getValue());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}
