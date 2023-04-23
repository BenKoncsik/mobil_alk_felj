package hu.koncsik;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
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
import java.util.Objects;

import hu.koncsik.adapter.UserItem;
import hu.koncsik.adapter.UserItemAdapter;

public class HomeFragment extends Fragment {
    private static final String LOG_TAG = HomeFragment.class.getName();
    private FirebaseUser user;
    private FirebaseAuth mAuth;

    private RecyclerView mRecyclerView;
    private ArrayList<UserItem> mItemsData;
    private UserItemAdapter mAdapter;


    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    private FrameLayout redCircle;
    private TextView countTextView;
    private int gridNumber = 1;
    private Integer itemLimit = 5;

    private SharedPreferences preferences;

    private boolean viewRow = true;
    public static String userFirebaseId;
    private ListenerRegistration userListenerRegistration;
    public HomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        userFirebaseId = user.getEmail();
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), gridNumber));
        mItemsData = new ArrayList<>();
        mAdapter = new UserItemAdapter(getContext(), mItemsData, false);
        mRecyclerView.setAdapter(mAdapter);


        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("users");

        queryData();

        setHasOptionsMenu(true);
        updateStatus(true);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        //getContext().registerReceiver(powerReceiver, filter);

        attachUserListener();
        return view;
    }



    BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();

            if (intentAction == null)
                return;

            switch (intentAction) {
                case Intent.ACTION_POWER_CONNECTED:
                    itemLimit = 10;
                    queryData();
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    itemLimit = 5;
                    queryData();
                    break;
            }
        }
    };

    private void queryData() {
        mItemsData.clear();
        mItems.orderBy("lastActive", Query.Direction.DESCENDING).limit(itemLimit).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                UserItem item = document.toObject(UserItem.class);
                if(!item.getEmail().equals(user.getEmail()))mItemsData.add(item);
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
                Intent intent = new Intent(getContext(), MainActivity.class);
                getContext().startActivity(intent);
                return true;
            case R.id.new_group:
                Log.d(LOG_TAG, "new Group!");
                startActivity(new Intent(getContext(), GroupsChat.class));

                return true;
            case R.id.refresh:
                Log.d(LOG_TAG, "Refresh user List!");
                queryData();
           /* case R.id.view_selector:
                if (viewRow) {
                    changeSpanCount(item, R.drawable.ic_view_grid, 1);
                } else {
                    changeSpanCount(item, R.drawable.ic_view_row, 2);
                }
                return true;*/
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



    private void attachUserListener() {
        CollectionReference userCollection = mFirestore.collection("users");
        Query userQuery = userCollection.orderBy("lastActive", Query.Direction.DESCENDING);
        userListenerRegistration = userQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(LOG_TAG, "Listen failed.", error);
                return;
            }

            if (value != null) {
                ArrayList<UserItem> userItems = new ArrayList<>();
                for (DocumentSnapshot document : value.getDocuments()) {
                    UserItem userItem = document.toObject(UserItem.class);
                    if(!userItem.getEmail().equals(user.getEmail()))userItems.add(userItem);
                }
                mAdapter.updateUsers(userItems);
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
        attachUserListener();
    }
    @Override
    public void onStop() {
        super.onStop();
        detachUserListener();
    }

    @Override
    public void onStart() {
        super.onStart();
        attachUserListener();
    }


}