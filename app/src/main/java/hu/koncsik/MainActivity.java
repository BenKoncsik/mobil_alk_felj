package hu.koncsik;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import hu.koncsik.adapter.UserItem;

import android.view.Menu;
import android.view.MenuItem;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();
    private static final String PREF_KEY = MainActivity.class.getPackage().toString();
    private static final int RC_SIGN_IN = 123;
     private static final int SECRET_KEY = 69;

    EditText userNameET;
    EditText passwordET;

    private SharedPreferences preferences;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

        userNameET = findViewById(R.id.editTextUserName);
        passwordET = findViewById(R.id.editTextPassword);

        preferences = getSharedPreferences(PREF_KEY, MODE_PRIVATE);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("users");
        Log.i(LOG_TAG, "onCreate");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Log.i(LOG_TAG, "login with google");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(LOG_TAG, "firebaseAuthWithGoogle:" + account.getId());
              firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(LOG_TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(LOG_TAG, "signInWithCredential:success");
                        updateUser();
                        startCheat();
                    } else {
                        Log.w(LOG_TAG, "signInWithCredential:failure", task.getException());
                    }
                });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logout) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void register(View view) {
        Intent intent = new Intent(this, Registration.class);
         intent.putExtra("SECRET_KEY", SECRET_KEY);
        startActivity(intent);
    }

    public void login(View view) {
        String email = userNameET.getText().toString();
        String password = passwordET.getText().toString();
    if(email.equals("") || password.equals("")) {
        Toast.makeText(this, "Not filed email or password input :( ", Toast.LENGTH_LONG).show();
        return;
    }
         Log.i(LOG_TAG, "Logged: " + email + ", password: " + password);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Log.d(LOG_TAG, "Login done!");
                    updateUser();
                    startCheat();
                } else {
                    Toast.makeText(MainActivity.this, "Invalid email or password!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateUser(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mItems.whereEqualTo("email", user.getEmail()).limit(1).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                for (QueryDocumentSnapshot userItem : queryDocumentSnapshots) {

                    userItem.getReference().update("lastActive", new Date(), "active", true)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(LOG_TAG, "Not exist success add");
                            })
                            .addOnFailureListener(e -> {
                                Log.d(LOG_TAG, "Not exists fail add");
                            });
                    break;
                }
            }else{
                UserItem newUser = new UserItem(user.getDisplayName(), user.getEmail(), new Date());
                mItems.add(newUser)
                        .addOnSuccessListener(documentReference -> {
                            Log.d(LOG_TAG, "Exists success add");
                        })
                        .addOnFailureListener(e -> {
                            Log.d(LOG_TAG, "Exists fail add");
                        });
            }
        });
    }

    public void loginWithGoogle(View view) {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startCheat();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userName", userNameET.getText().toString());
        editor.putString("password", passwordET.getText().toString());
        editor.apply();

        Log.i(LOG_TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    private void startCheat() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("SECRET_KEY", SECRET_KEY);
        startActivity(intent);
    }
}