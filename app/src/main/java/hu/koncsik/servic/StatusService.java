package hu.koncsik.servic;


import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class StatusService extends Service {
    private static final String LOG_TAG = StatusService.class.toString();
    private final IBinder binder = new StatusBinder();

    private static int MESSAGE_INTERVAL = 1200000;
    private static final int MESSAGE_INTERVAL_MOBIL_DATA = 60000;
    private static final int MESSAGE_INTERVAL_FOREGROUND_WIFI = 10000;
    private Handler mHandler;
    private Runnable mRunnable;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                int timeOut = SetTimeOut();
                Log.d(LOG_TAG, "Status service check user is run-->"+timeOut);
                updateStatus(isApplicationInBackground());
                mHandler.postDelayed(this, timeOut);
            }
        };
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.postDelayed(mRunnable, SetTimeOut());
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        updateStatus(false);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        updateStatus(false);
        mHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }

    private boolean isApplicationInBackground() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = am.getRunningAppProcesses();
        if (appProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : appProcesses) {
                if (processInfo.processName.equals(getPackageName())) {
                    return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                }
            }
        }
        return false;
    }


    private static boolean old_status = false;
    private void updateStatus(boolean status) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore mFirestore =  FirebaseFirestore.getInstance();
        if(old_status == status) return;
        if (firebaseUser != null) {
            old_status = status;
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

    private int SetTimeOut(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return MESSAGE_INTERVAL_FOREGROUND_WIFI;
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return MESSAGE_INTERVAL_MOBIL_DATA;
            }
        }
        return MESSAGE_INTERVAL;
    }

    public class StatusBinder extends Binder {
            public StatusService getService() {
                return StatusService.this;
            }
        }


}
