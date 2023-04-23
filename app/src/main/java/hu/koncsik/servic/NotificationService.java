package hu.koncsik.servic;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import hu.koncsik.Chat;
import hu.koncsik.HomeActivity;
import hu.koncsik.R;
import hu.koncsik.adapter.ChatItem;

public class NotificationService extends Service {
    private static final String LOG_TAG = NotificationService.class.toString();
    private ListenerRegistration registration;

    private static Map<String, Integer> notificationIds = new HashMap<>();
    private static Map<String, List<String>> notReadMessages = new HashMap<>();
    private static AtomicInteger notificationIdCounter = new AtomicInteger(0);



    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        createDownloadChannel();
        Log.d(LOG_TAG, "Notification service check user is run");
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        monitorPrivateMessages(firebaseUser.getEmail());
        monitorGroupMessages(firebaseUser.getEmail());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void monitorPrivateMessages(String userEmail){
        FirebaseFirestore firestorm = FirebaseFirestore.getInstance();
        CollectionReference messagesRef = firestorm.collection("messages");
        registration = messagesRef.whereArrayContains("members", userEmail)
                .whereEqualTo("group", false)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(LOG_TAG, "Listen failed.", e);
                        return;
                    }

                    for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                        DocumentSnapshot documentSnapshot = dc.getDocument();
                        ChatItem chatItem = documentSnapshot.toObject(ChatItem.class);
                        switch (dc.getType()) {
                            case ADDED:
                            case MODIFIED:
                                Log.d(LOG_TAG, "Notification service detected private message changes");
                                if(chatItem.getMessages() != null && !chatItem.getMessages().isEmpty() && !chatItem.getMessages().get(chatItem.getMessages().size()-1).getSender().equals(userEmail)){
                                    new SendNotificationAsyncTask().execute(chatItem);
                                }
                                break;
                        }
                    }
                });
    }

    private void monitorGroupMessages(String userEmail){
        FirebaseFirestore firestorm = FirebaseFirestore.getInstance();
        CollectionReference messagesRef = firestorm.collection("messages");
        registration = messagesRef.whereArrayContains("members", userEmail)
                .whereEqualTo("group", true)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(LOG_TAG, "Listen failed.", e);
                        return;
                    }

                    for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                        DocumentSnapshot documentSnapshot = dc.getDocument();
                        ChatItem chatItem = documentSnapshot.toObject(ChatItem.class);
                        switch (dc.getType()) {
                            case ADDED:
                            case MODIFIED:
                                Log.d(LOG_TAG, "Notification service detected groups message changes");
                                if(chatItem.getMessages() != null && !chatItem.getMessages().isEmpty() && !chatItem.getMessages().get(chatItem.getMessages().size()-1).getSender().equals(userEmail)) {
                                    new SendNotificationAsyncTask().execute(chatItem);
                                }
                                break;
                        }
                    }
                });
    }
    private String emailToName(String email) {
        FirebaseFirestore firestorm = FirebaseFirestore.getInstance();
        CollectionReference usersRef = firestorm.collection("users");
        final String[] name = new String[1];
        name[0] = email;
        final CountDownLatch latch = new CountDownLatch(1);

        usersRef.whereEqualTo("email", email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                            name[0] = documentSnapshot.getString("name");
                        } else {
                            Log.w(LOG_TAG, "No user found with email: " + email);
                        }
                    } else {
                        Log.w(LOG_TAG, "Error getting documents: ", task.getException());
                    }
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Error waiting for result: ", e);
        }

        return name[0];
    }


    private void sendNotification(String id, String name, String text, ChatItem chatItem) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        PendingIntent pendingIntent = getClickIntent(id, chatItem);
        PendingIntent deletePendingIntent = getDeleteIntent(id);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "FirebaseChannelId")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDeleteIntent(deletePendingIntent)
                .setContentTitle(name)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);


        int notificationId;
        if (notificationIds.containsKey(id)) {
            notificationId = notificationIds.get(id);
                List<String> messages = new ArrayList<>(notReadMessages.get(id));
                messages.add(text);
                notReadMessages.put(id, messages);
                sendNotification(id, name, messages, notificationId, chatItem);
                return;
        } else {
            if(!notReadMessages.containsKey(id)){
                ArrayList<String> newNotReadMessage = new ArrayList<>();
                newNotReadMessage.add(text);
                notReadMessages.put(id, newNotReadMessage);
            }
            notificationId = new Random().nextInt();
            notificationIds.put(id, notificationId);
        }
        notificationManager.notify(notificationId, builder.build());
    }

    private void sendNotification(String id, String name, List<String> texts, int notificationId, ChatItem chatItem) {
        if(!notReadMessages.containsKey(id)){
            notReadMessages.put(id, texts);
        }
        PendingIntent pendingIntent = getClickIntent(id,chatItem);
        PendingIntent deletePendingIntent = getDeleteIntent(id);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                .setBigContentTitle(name + " messages")
                .setSummaryText("New messages");

        for (String text : texts) {
            inboxStyle.addLine(text);
        }


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "FirebaseChannelId")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDeleteIntent(deletePendingIntent)
                .setContentTitle(name)
                .setContentText(texts.get(0))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(inboxStyle);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(notificationId, builder.build());
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FirebaseChannel";
            String description = "Channel for Firebase messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("FirebaseChannelId", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void createDownloadChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Downland Chanel";
            String description = "Channel for Firebase messages";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("DownlandChanel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }


    private PendingIntent getDeleteIntent(String id) {
        Intent deleteIntent = new Intent(this, DeleteNotificationReceiver.class);
        deleteIntent.setAction(DeleteNotificationReceiver.DELETE_NOTIFICATION_ACTION);
        deleteIntent.putExtra("id", id);
        int requestCode = notificationIdCounter.getAndIncrement();
        return PendingIntent.getBroadcast(this, requestCode, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent getClickIntent(String id, ChatItem chatItem) {
        Intent clickIntent = new Intent(this, NotificationClickReceiver.class);
        clickIntent.setAction(NotificationClickReceiver.NOTIFICATION_CLICK_ACTION);
        clickIntent.putExtra("id", id);
        clickIntent.putExtra("chatItem", chatItem);
        int requestCode = notificationIdCounter.getAndIncrement();
        return PendingIntent.getBroadcast(this, requestCode, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }


    public class SendNotificationAsyncTask extends AsyncTask<ChatItem, Void, Void> {
        @Override
        protected Void doInBackground(@NonNull ChatItem... chatItems) {
            ChatItem chatItem = chatItems[0];
            if (chatItem.getMessages() != null && !chatItem.getMessages().isEmpty()) {
                int last = chatItem.getMessages().size() - 1;
                if(chatItem.isGroup()){
                    String text = chatItem.getMessages().get(last).getText();
                    sendNotification(chatItem.getId(), chatItem.getName(), text, chatItem);
                }else {
                    String email = chatItem.getMessages().get(last).getSender();
                    String name = emailToName(email);
                    String text = chatItem.getMessages().get(last).getText();
                    sendNotification(email, name, text, chatItem);
                }
            }
            return null;
        }
    }

    public static class DeleteNotificationReceiver extends BroadcastReceiver {
        public static final String DELETE_NOTIFICATION_ACTION = "hu.koncsik.DELETE_NOTIFICATION";

        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            if (id != null) {
                if(notReadMessages.containsKey(id)) notReadMessages.remove(id);
                if(notificationIds.containsKey(id)) notificationIds.remove(id);
            }
        }
    }
    public static class NotificationClickReceiver extends BroadcastReceiver {
        public static final String NOTIFICATION_CLICK_ACTION = "hu.koncsik.NOTIFICATION_CLICK";

        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            if (id != null) {
                if(notReadMessages.containsKey(id)) notReadMessages.remove(id);
                if(notificationIds.containsKey(id)) notificationIds.remove(id);
            }

            ChatItem chatItem = (ChatItem) intent.getSerializableExtra("chatItem");
            if(chatItem != null) {
                Intent intentChat = new Intent(context, Chat.class);
                intentChat.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentChat.putExtra("chatItem", chatItem);
                intentChat.putExtra("id", chatItem.getId());
                context.startActivity(intentChat);
            }else {
                Intent homeIntent = new Intent(context, HomeActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Add this line
                context.startActivity(homeIntent);
            }


        }
    }
}

