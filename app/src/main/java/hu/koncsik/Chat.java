package hu.koncsik;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


import java.util.List;

import hu.koncsik.adapter.ChatItem;
import hu.koncsik.adapter.Message_1;
import hu.koncsik.adapter.MessageAdapter;

public class Chat extends AppCompatActivity {

    private static final String LOG_TAG = Chat.class.toString();
    private ChatItem chat;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageButton sendMessageButton;
    private MessageAdapter messageAdapter;
    private List<Message_1> messages;
    private FirebaseFirestore mFirestore;
    private DocumentReference mItems;
    private FirebaseAuth mAuth;
    private FirebaseUser loggedUser;
    private String messageId;
    private ListenerRegistration listenerRegistration;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheat);
        messagesRecyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.message_input);
        sendMessageButton = findViewById(R.id.send_button);

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        loggedUser = FirebaseAuth.getInstance().getCurrentUser();

        chat = (ChatItem) getIntent().getSerializableExtra("cheatItem");
        messageId = (String) getIntent().getStringExtra("id");

        Log.d(LOG_TAG, "Cheat is on: " + chat.getMembers().get(0) + "<-->" + chat.getMembers().get(1) + "_id_" + messageId);
        messages = chat.getMessages();
        mItems = mFirestore.collection("messages").document(messageId);



        messageAdapter = new MessageAdapter(this, messages, loggedUser.getEmail());
        messagesRecyclerView.setAdapter(messageAdapter);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = messageInput.getText().toString().trim();

                if (!messageText.isEmpty()) {
                    Message_1 newMessage = new Message_1(messageText, loggedUser.getEmail());
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    messageInput.setText("");

                    mItems.update("messages", FieldValue.arrayUnion(newMessage))
                            .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Message added successfully"))
                            .addOnFailureListener(e -> {
                                messages.remove(newMessage);
                                Toast.makeText(Chat.this, "Error send message :(", Toast.LENGTH_LONG).show();
                                Log.w(LOG_TAG, "Error adding message", e);
                            });
                }
            }
        });


    }
    private void attachCheatListener() {
        listenerRegistration = mItems.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(LOG_TAG, "Listen failed.", error);
                return;
            }

            if (value != null && value.exists()) {
                updateCheat(value);
                Log.d(LOG_TAG, "Changes message");
            } else {
                Log.d(LOG_TAG, "Current data: null");
            }
        });
    }
    private void updateCheat(DocumentSnapshot documentSnapshot){
        chat = documentSnapshot.toObject(ChatItem.class);
        messages = chat.getMessages();
        messageAdapter.notifyDataSetChanged();
        messageAdapter.updateMessages(messages);
        messagesRecyclerView.scrollToPosition(messages.size() - 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachCheatListener();
    }
}

