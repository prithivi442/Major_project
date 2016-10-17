package me.prithivi.friendlocator;

import android.os.Handler;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class chat extends Activity {
    ListView lvChat;
    static final int POLL_INTERVAL = 1000;
    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;
    ArrayList<Message> mMessages;
    ChatListAdapter mAdapter;
    // Keep track of initial load to scroll to the bottom of the ListView
    boolean mFirstLoad;
    EditText etMessage;
    Button btSend;
    // Setup message field and posting
    void setupMessagePosting() {
        // Find the text field and button
        etMessage = (EditText) findViewById(R.id.etMessage);
        btSend = (Button) findViewById(R.id.btSend);
        lvChat = (ListView) findViewById(R.id.lvChat);
        mMessages = new ArrayList<>();
        // Automatically scroll to the bottom when a data set change notification is received and only if the last item is already visible on screen. Don't scroll to the bottom otherwise.
        lvChat.setTranscriptMode(1);
        mFirstLoad = true;
        final String userId = ParseUser.getCurrentUser().getObjectId();
        mAdapter = new ChatListAdapter(chat.this, userId, mMessages);
        lvChat.setAdapter(mAdapter);
        // When send button is clicked, create message object on Parse
        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = etMessage.getText().toString();
                //ParseObject message = ParseObject.create("Message");
                //message.put(Message.USER_ID_KEY, userId);
                //message.put(Message.BODY_KEY, data);
                // Using new `Message` Parse-backed model now
                Message message = new Message();
                message.setBody(data);
                message.setUserId(ParseUser.getCurrentUser().getObjectId());
                message.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Toast.makeText(chat.this, "Successfully created message on Parse",
                                Toast.LENGTH_SHORT).show();
                        refreshMessages();
                    }
                });
                etMessage.setText(null);
            }
        });
    }



    // Query messages from Parse so we can load them into the chat adapter
    void refreshMessages() {
        // Construct query to execute
        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        // Configure limit and sort order
        query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);
        query.orderByDescending("createdAt");
        // Execute query to fetch all messages from Parse asynchronously
        // This is equivalent to a SELECT query with SQL
        query.findInBackground(new FindCallback<Message>() {
            public void done(List<Message> messages, ParseException e) {
                if (e == null) {
                    mMessages.clear();
                    Collections.reverse(messages);
                    mMessages.addAll(messages);
                    mAdapter.notifyDataSetChanged(); // update adapter
                    // Scroll to the bottom of the list on initial load
                    if (mFirstLoad) {
                        lvChat.setSelection(mAdapter.getCount() - 1);
                        mFirstLoad = false;
                    }
                } else {
                    Log.e("message", "Error Loading Messages" + e);
                }
            }
        });
    }
     // milliseconds
    Handler mHandler = new Handler();  // android.os.Handler
    Runnable mRefreshMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            mHandler.postDelayed(this, POLL_INTERVAL);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if (ParseUser.getCurrentUser() != null) {
            startWithCurrentUser();
        } else {
            Log.v("chat","unsucessful bundle");
        }
        mHandler.postDelayed(mRefreshMessagesRunnable, POLL_INTERVAL);
    }
    // Query messages from Parse so we can load them into the chat adapter
    // Get the userId from the cached currentUser object
    void startWithCurrentUser() {
        setupMessagePosting();
    }

}
