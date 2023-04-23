package hu.koncsik;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Objects;

import hu.koncsik.adapter.ChatItem;
import hu.koncsik.servic.NotificationService;
import hu.koncsik.servic.StatusService;


public class HomeActivity extends AppCompatActivity{
    private static final String LOG_TAG = HomeActivity.class.toString();
    private FirebaseUser user;
    private FirebaseAuth mAuth;


    private FirebaseFirestore mFirestore;
    private CollectionReference mUserItems;
    private CollectionReference mMessageItems;



    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabAdapterCheat tabAdapter;

    private MutableLiveData<ArrayList<String>> selectedEmailsLiveData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabl_layout);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
        }
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        tabAdapter = new TabAdapterCheat(this);
        viewPager.setAdapter(tabAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mUserItems = mFirestore.collection("users");
        mMessageItems = mFirestore.collection("messages");



        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> updateTabTitle(tab, position));
        tabLayoutMediator.attach();

        Intent intent = new Intent(this, StatusService.class);
        startService(intent);
        Intent intentNoty = new Intent(this, NotificationService.class);
        startService(intentNoty);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HomeActivity.this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    101);
        }
    }
    public void startChatFragment(ChatItem chatItem, String id) {
        Intent intent = new Intent(this, Chat.class);
        intent.putExtra("chatItem", chatItem);
        intent.putExtra("id", id);
        startActivity(intent);
    }
    private void updateTabTitle(TabLayout.Tab tab, int position) {
        switch (position) {
            case 0:
                tab.setText("Private Messages");
                break;
            case 1:
                tab.setText("Groups Messages");
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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