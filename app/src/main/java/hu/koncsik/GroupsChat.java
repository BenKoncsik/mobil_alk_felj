package hu.koncsik;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Objects;

import hu.koncsik.adapter.ChatItem;
import hu.koncsik.adapter.UserItem;
import hu.koncsik.adapter.UserItemAdapter;
import hu.koncsik.bindInterface.GroupsChatCallback;
import hu.koncsik.extensions.GenerateId;

public class GroupsChat extends AppCompatActivity implements GroupsChatCallback {
    private static final String LOG_TAG = GroupsChat.class.toString();
    private FirebaseUser user;
    private FirebaseAuth mAuth;

    private RecyclerView mRecyclerView;
    private ArrayList<UserItem> mItemsData;
    private UserItemAdapter mAdapter;


    private FirebaseFirestore mFirestore;
    private CollectionReference mUserItems;
    private CollectionReference mMessageItems;

    private FrameLayout redCircle;
    private TextView countTextView;
    private int gridNumber = 1;
    private Integer itemLimit = 5;

    private SharedPreferences preferences;

    private boolean viewRow = true;
    public static String userFirebaseId;
    private ListenerRegistration userListenerRegistration;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabAdapter tabAdapter;

    private ChatItem chatItem = null;

    private MutableLiveData<ArrayList<String>> selectedEmailsLiveData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabl_layout);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        Intent intent = getIntent();
        if(intent.hasExtra("chatItem")) {
            chatItem = (ChatItem) getIntent().getSerializableExtra("chatItem");
            getSelectedEmailsLiveData();
            selectedEmailsLiveData.setValue(chatItem.getMembers());
        }


        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        tabAdapter = new TabAdapter(this, chatItem);
        viewPager.setAdapter(tabAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mUserItems = mFirestore.collection("users");
        mMessageItems = mFirestore.collection("messages");

        getSelectedEmailsLiveData().observe(this, new Observer<ArrayList<String>>() {
            @Override
            public void onChanged(ArrayList<String> selectedEmails) {
                TabLayout.Tab tab = tabLayout.getTabAt(1);
                if (tab != null) {
                    updateTabTitle(tab, 1);
                }
            }
        });

        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> updateTabTitle(tab, position));
        tabLayoutMediator.attach();




    }

    private void updateTabTitle(TabLayout.Tab tab, int position) {
        switch (position) {
            case 0:
                tab.setText("Base Settings");
                break;
            case 1:
                if (selectedEmailsLiveData != null) tab.setText("Add Members " + selectedEmailsLiveData.getValue().size());
                else tab.setText("Add Members 1");
                break;
        }
    }

    @Override
    public MutableLiveData<ArrayList<String>> getSelectedEmailsLiveData() {
        if (selectedEmailsLiveData == null) {
            selectedEmailsLiveData = new MutableLiveData<ArrayList<String>>();
            ArrayList<String> initialEmails = new ArrayList<>();
            initialEmails.add(user.getEmail());
            selectedEmailsLiveData.setValue(initialEmails);
        }
        return selectedEmailsLiveData;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void createGroup(View view) {
        EditText editText = findViewById(R.id.group_name);
        String groupName = editText.getText().toString();
        Log.d(LOG_TAG, "Groups name: " + groupName + "-->Members: " + selectedEmailsLiveData.getValue().size());
        if(groupName.equals("")) {
            Toast.makeText(this, "Need a name for the group", Toast.LENGTH_LONG).show();
            return;
        }
        if(selectedEmailsLiveData == null || selectedEmailsLiveData.getValue().size() <= 1){
            Toast.makeText(this, "Minimum 2 members", Toast.LENGTH_LONG).show();
            return;
        }

        ChatItem chatItem = new ChatItem(groupName, selectedEmailsLiveData.getValue(), true, GenerateId.newId());

        mMessageItems.add(chatItem)
                .addOnSuccessListener(aVoid -> {
                    Log.d(LOG_TAG, "Group Created");
                   mMessageItems.whereEqualTo("id", chatItem.getId()).limit(1).get().addOnSuccessListener(queryDocumentSnapshots -> {
                               for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                   Intent intent = new Intent(this, Chat.class);
                                   intent.putExtra("chatItem", chatItem);
                                   intent.putExtra("id", document.getId());
                                   this.startActivity(intent);
                               }
                   });
                })
                .addOnFailureListener(e -> Log.d(LOG_TAG, "Error create group: " + e));
    }

    public void saveGroup(View view) {
        EditText editText = findViewById(R.id.group_name);
        String groupName = editText.getText().toString();
        Log.d(LOG_TAG, "Groups name: " + groupName + "-->Members: " + selectedEmailsLiveData.getValue().size());
        if(groupName.equals("")) {
            Toast.makeText(this, "Need a name for the group", Toast.LENGTH_LONG).show();
            return;
        }
        if(selectedEmailsLiveData == null || selectedEmailsLiveData.getValue().size() <= 1){
            Toast.makeText(this, "Minimum 2 members", Toast.LENGTH_LONG).show();
            return;
        }
        chatItem.setId(chatItem.getId());
        chatItem.setName(groupName);
        chatItem.setMembers(selectedEmailsLiveData.getValue());

        mMessageItems.whereEqualTo("id", chatItem.getId()).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String documentId = document.getId();
                            mMessageItems.document(documentId).set(chatItem, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(LOG_TAG, "Group Updated");
                                        Intent intent = new Intent(this, Chat.class);
                                        intent.putExtra("chatItem", chatItem);
                                        intent.putExtra("id", documentId);
                                        this.startActivity(intent);
                                    })
                                    .addOnFailureListener(e -> Log.d(LOG_TAG, "Error updating group: " + e));
                        }
                    } else {
                        mMessageItems.add(chatItem)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(LOG_TAG, "Group Created");
                                    chatItem.setId(documentReference.getId());
                                    Intent intent = new Intent(this, Chat.class);
                                    intent.putExtra("cheatItem", chatItem);
                                    intent.putExtra("id", documentReference.getId());
                                    this.startActivity(intent);
                                })
                                .addOnFailureListener(e -> Log.d(LOG_TAG, "Error create group: " + e));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}