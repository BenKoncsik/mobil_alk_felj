package hu.koncsik;

import static hu.koncsik.HomeFragment.gridNumber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import hu.koncsik.adapter.ChatItem;
import hu.koncsik.adapter.GroupItemAdapter;

public class GroupListFragment extends Fragment {

    private static final String LOG_TAG = GroupListFragment.class.getName();
    private FirebaseUser user;

    private RecyclerView mRecyclerView;
    private ArrayList<ChatItem> mItemsData;
    private GroupItemAdapter mAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    private Integer itemLimit = 5;

    private boolean viewRow = true;
    public static String userFirebaseId;
    private ListenerRegistration userListenerRegistration;

    public GroupListFragment() {

    }


    public static GroupListFragment newInstance() {
        GroupListFragment fragment = new GroupListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {}
    }
    public void changeLayout(){
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.setSpanCount(gridNumber);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_group_list_fregment, container, false);
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        userFirebaseId = user.getEmail();
        ;
        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), gridNumber));
        mItemsData = new ArrayList<>();
        mAdapter = new GroupItemAdapter(getContext(), mItemsData);
        mRecyclerView.setAdapter(mAdapter);


        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("messages");

        queryData();

        setHasOptionsMenu(true);

        attachGroupsListener();
        return rootView;
    }
    private void queryData() {
        mItemsData.clear();
        mItems.orderBy("name", Query.Direction.DESCENDING).limit(itemLimit)
                .whereEqualTo("group", true)
                .whereArrayContains("members", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                ChatItem item = document.toObject(ChatItem.class);
               mItemsData.add(item);
            }

            if (mItemsData.size() == 0) {
                queryData();
            }
            mAdapter.notifyDataSetChanged();
        });
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(LOG_TAG, s);
                mAdapter.getFilter().filter(s);
                return false;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                Log.d(LOG_TAG, "Logout clicked!");
                updateStatus(false);
                detachUserListener();
                FirebaseAuth.getInstance().signOut();
                getActivity().finish();
                return true;
            case R.id.new_group:
                Log.d(LOG_TAG, "new Group!");
                Intent intent = new Intent(getContext(), GroupsChat.class);
                startActivity(intent);

                return true;
            case R.id.refresh:
                Log.d(LOG_TAG, "Refresh group List!");
                queryData();
            case R.id.grid:
                if(gridNumber == 1) gridNumber = 2;
                else gridNumber = 1;
                changeLayout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        /*final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        redCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_red_circle);
        countTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(alertMenuItem);
            }
        });
        return super.onPrepareOptionsMenu(menu);*/

    }

    private void updateStatus(boolean status) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            Query userQuery = mFirestore.collection("users").whereEqualTo("email", firebaseUser.getEmail());
            userQuery.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                        DocumentReference userRef = documentSnapshot.getReference();
                        userRef.update("active", status)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(LOG_TAG, "Status updated-->" + status);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(LOG_TAG, "Error updated status-->" + e);
                                });
                    } else {
                        Log.e(LOG_TAG, "No user found with the specified email");
                    }
                } else {
                    Log.e(LOG_TAG, "Error getting user", task.getException());
                }
            });
        } else {
            Log.e(LOG_TAG, "Firebase user is null");
        }
    }



    private void attachGroupsListener() {
        CollectionReference userCollection = mFirestore.collection("messages");
        Query userQuery = userCollection.orderBy("name", Query.Direction.DESCENDING).whereEqualTo("group", true).whereArrayContains("members", user.getEmail());
        userListenerRegistration = userQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(LOG_TAG, "Listen failed.", error);
                return;
            }

            if (value != null) {
                ArrayList<ChatItem> chatItems = new ArrayList<>();
                for (DocumentSnapshot document : value.getDocuments()) {
                    ChatItem chatItem = document.toObject(ChatItem.class);
                    chatItems.add(chatItem);
                }
                mAdapter.updateGroups(chatItems);
            } else {
                Log.d(LOG_TAG, "Current data: null");
            }
        });
    }
    private void detachUserListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        detachUserListener();

    }

    @Override
    public void onPause() {
        super.onPause();
        detachUserListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        attachGroupsListener();
    }
    @Override
    public void onStop() {
        super.onStop();
        detachUserListener();
    }

    @Override
    public void onStart() {
        super.onStart();
        attachGroupsListener();
    }



}