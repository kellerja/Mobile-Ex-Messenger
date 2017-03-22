package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class MenuFragment extends Fragment{

    private ImageButton topicsButton;
    private ImageButton createTopicButton;
    private ImageButton chatButton;
    private ImageButton profileButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu_fragment, container, false);

        topicsButton = (ImageButton) view.findViewById(R.id.menu_topics_button);
        createTopicButton = (ImageButton) view.findViewById(R.id.menu_create_topic_button);
        chatButton = (ImageButton) view.findViewById(R.id.menu_chat_button);
        profileButton = (ImageButton) view.findViewById(R.id.menu_profile_button);

        setTopicsButtonHandler();
        setCreateTopicButtonHandler();
        setChatButtonHandler();
        setProfileButtonHandler();

        return view;
    }

    private void changeActivity(Class activityToChangeTo) {
        Intent intent = new Intent(getActivity(), activityToChangeTo);
        startActivity(intent);
    }

    private void setTopicsButtonHandler() {
        topicsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeActivity(TopicsActivity.class);
            }
        });
    }

    private void setCreateTopicButtonHandler() {
        createTopicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeActivity(CreateTopicActivity.class);
            }
        });
    }

    private void setChatButtonHandler() {
        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeActivity(ChatActivity.class);
            }
        });
    }

    private void setProfileButtonHandler() {
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeActivity(ProfileActivity.class);
            }
        });
    }
}
