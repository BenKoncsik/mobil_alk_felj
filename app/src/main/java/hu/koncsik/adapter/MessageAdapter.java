package hu.koncsik.adapter;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import hu.koncsik.Chat;
import hu.koncsik.R;
import hu.koncsik.assync.DownloadImageTask;
import hu.koncsik.assync.GetEmailToName;
import hu.koncsik.assync.LoadImageAsyncTask;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String LOG_TAG = MessageAdapter.class.toString();
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SAVE_IMAGE = 3;


    private List<Message_1> messages;
    private String loggedInUserEmail;
    private static Context context;

    public MessageAdapter(Context context, List<Message_1> messages, String loggedInUserEmail) {
        this.messages = messages;
        this.loggedInUserEmail = loggedInUserEmail;
        this.context = context;
    }

    public void updateMessages(List<Message_1> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message_1 message = messages.get(position);
        if (message.getSender().equals(loggedInUserEmail)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sent_message_item, parent, false);
            return new SentMessageHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.received_message_item, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message_1 message = messages.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageHolder) holder).bind(message);


        } else {
            ((ReceivedMessageHolder) holder).bind(message);

        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private void loadImageFromFirebase(String imageUrl, ImageView imageView) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.loading_placeholder);
            if(isWifi()) {
                try {
                    StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    new LoadImageAsyncTask(imageView, context).execute(storageReference);
                }catch (Exception e){
                    Log.d(LOG_TAG, "I'm losing weight on the firebase");
                    imageView.setImageResource(R.drawable.ic_money);
                }

            }else imageView.setImageResource(R.drawable.ic_mobil_data_off);
        } else {
            imageView.setImageDrawable(null);
        }
    }
    private boolean isWifi(){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(
                (Activity) context,
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                WRITE_EXTERNAL_STORAGE_PERMISSION
        );
    }
    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView senderName;
        TextView senderTime;
        ImageView messageImage;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            senderName = itemView.findViewById(R.id.sender_name);
            senderTime = itemView.findViewById(R.id.message_time);
            messageImage = itemView.findViewById(R.id.message_image);


        }

        void bind(Message_1 message) {
            Log.d(LOG_TAG, "Message sender-->"+ message.getText());
            messageText.setText(message.getText());
            new GetEmailToName(message.getSender(), senderName, context).execute();
            senderTime.setText(new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(message.getSend()));
//            if((messageText.getText() == null || messageText.getText().equals("")) && (message.getImageUrl() != null || message.getImageUrl().equals(""))) messageText.setVisibility(View.GONE);
            if (message.getImageUrl() != null ) {
                messageImage.setVisibility(View.VISIBLE);
                loadImageFromFirebase(message.getImageUrl(), messageImage);
                messageImage.setOnClickListener(view -> {
                    if (hasStoragePermission()) {
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        intent.putExtra(Intent.EXTRA_TITLE, "downloaded_image.jpg");
                        ((Chat) context).startChatFragment(intent, message.getImageUrl());
                    } else {
                        requestStoragePermission();
                    }
                });
            } else {
                messageImage.setVisibility(View.GONE);
            }
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView messageImage;
        TextView receiverName;
        TextView receiverTime;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            receiverName = itemView.findViewById(R.id.receive_name);
            receiverTime = itemView.findViewById(R.id.message_time);
            messageImage = itemView.findViewById(R.id.message_image);
        }

        void bind(Message_1 message) {
            Log.d(LOG_TAG, "Message receiver-->"+ message.getText());
             messageText.setText(message.getText());
            new GetEmailToName(message.getSender(), receiverName, context).execute();
            receiverTime.setText(new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(message.getSend()));
//            if((messageText.getText() == null || messageText.getText().equals("")) && (message.getImageUrl() != null || message.getImageUrl().equals(""))) messageText.setVisibility(View.GONE);
            if (message.getImageUrl() != null ) {
                messageImage.setVisibility(View.VISIBLE);
                loadImageFromFirebase(message.getImageUrl(), messageImage);

                messageImage.setOnClickListener(view -> {
                    if (hasStoragePermission()) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_TITLE, "downloaded_image.jpg");
                    ((Chat) context).startChatFragment(intent, message.getImageUrl());
                    } else {
                        requestStoragePermission();
                    }
                });
            } else {
                messageImage.setVisibility(View.GONE);
            }
        }
    }

}