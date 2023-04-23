package hu.koncsik.assync;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.ref.WeakReference;
import java.sql.Array;
import java.util.concurrent.CountDownLatch;

public class GetEmailToName  extends AsyncTask<Void, Void, Void> {
    private static final String LOG_TAG = GetEmailToName.class.toString();
    private Context context;
    private String email;
    private TextView textView;

    public GetEmailToName(String email, TextView textView, Context context) {
        this.email = email;
        this.textView = textView;
        this.context = context;
    }


    @Override
    protected Void doInBackground(Void... voids) {
        FirebaseFirestore firestorm = FirebaseFirestore.getInstance();
        CollectionReference usersRef = firestorm.collection("users");

        final CountDownLatch latch = new CountDownLatch(1);

        usersRef.whereEqualTo("email", email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                            String name = documentSnapshot.getString("name");
                            if (textView != null) textView.setText(name);
                        } else {
                            Log.w(LOG_TAG, "No user found with email: " + email);
                            if (textView != null) textView.setText(email);
                        }
                    } else {
                        Log.w(LOG_TAG, "Error getting documents: ", task.getException());
                        if (textView != null) textView.setText(email);
                    }
                });
        return null;
    }




}
