package hu.koncsik;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import hu.koncsik.adapter.ChatItem;
import hu.koncsik.adapter.Message_1;
import hu.koncsik.adapter.MessageAdapter;
import hu.koncsik.assync.DownloadImageTask;

public class Chat extends AppCompatActivity {

    private static final String LOG_TAG = Chat.class.toString();
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 5;
    private ChatItem chat;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageButton sendMessageButton;
    private MessageAdapter messageAdapter;
    private List<Message_1> messages;
    private FirebaseFirestore mFirestorm;
    private DocumentReference mItems;
    private FirebaseAuth mAuth;
    private FirebaseUser loggedUser;
    private String messageFirebaseId;
    private ListenerRegistration listenerRegistration;
    private StorageReference mStorageRef;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri filePath = null;
    private ImageView selectedImage;
    private ImageButton deleteImageButton;
    private boolean isImageSelected = false;
    private ViewGroup chatLayout;
    private ImageButton selectImageButton;
    private ImageButton openImageButton;
    private ImageButton startCamera;
    private LinearLayout linearLayout;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_CODE_SAVE_IMAGE = 3;
    private File tempImageFile;
    private String imageSaveUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        messagesRecyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.message_input);
        sendMessageButton = findViewById(R.id.send_button);
        selectedImage = findViewById(R.id.selected_image);
        deleteImageButton = findViewById(R.id.cancel_image_button);
        selectImageButton = findViewById(R.id.select_image_button);
        openImageButton = findViewById(R.id.open_image_button);
        chatLayout = findViewById(R.id.message_input_layout);
        linearLayout = findViewById(R.id.image_option);
        startCamera = findViewById(R.id.start_camera);


        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mFirestorm = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        loggedUser = FirebaseAuth.getInstance().getCurrentUser();

        chat = (ChatItem) getIntent().getSerializableExtra("chatItem");
        messageFirebaseId = (String) getIntent().getStringExtra("id");

        Log.d(LOG_TAG, "Cheat is on: " + messageFirebaseId);
        if(chat != null && chat.getMessages() != null) messages = chat.getMessages();
        mFirestorm.collection("messages").whereEqualTo("id", chat.getId()).limit(1).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            mItems = document.getReference();
                            chat = document.toObject(ChatItem.class);
                            messages = chat.getMessages();
                            messageFirebaseId = document.getId();
                        }
                        }
                });

        Resources res = getResources();
        Drawable plus = res.getDrawable(R.drawable.ic_add);
        Drawable minus = res.getDrawable(R.drawable.ic_minus);
        messageAdapter = new MessageAdapter(this, messages, loggedUser.getEmail());
        messagesRecyclerView.setAdapter(messageAdapter);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        selectImageButton.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });
        openImageButton.setOnClickListener(view -> {
            Log.d(LOG_TAG, "Click selected image button");
            if (isImageSelected) {
                TransitionManager.beginDelayedTransition(chatLayout);
                linearLayout.setVisibility(View.GONE);
                openImageButton.setImageDrawable(plus);
                isImageSelected = false;
            } else {
                TransitionManager.beginDelayedTransition(chatLayout);
                linearLayout.setVisibility(View.VISIBLE);
                openImageButton.setImageDrawable(minus);
                isImageSelected = true;
            }
        });

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    TransitionManager.beginDelayedTransition(chatLayout);
                    linearLayout.setVisibility(View.GONE);
                    openImageButton.setImageDrawable(plus);
                    isImageSelected = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        sendMessageButton.setOnClickListener(view -> {
            String messageText = messageInput.getText().toString().trim();

            if (!messageText.isEmpty() || filePath != null) {
                Message_1 newMessage = new Message_1(messageText, loggedUser.getEmail());
                messagesRecyclerView.scrollToPosition(messages.size() - 1);
                messageInput.setText("");
                if (filePath != null) {
                    uploadImageToFirebaseStorage(filePath, newMessage);
                    filePath = null;
                    selectedImage.setImageBitmap(null);
                    selectedImage.setVisibility(View.GONE);
                    deleteImageButton.setVisibility(View.GONE);
                    deleteImage();
                } else {
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

        deleteImageButton.setOnClickListener(task -> {
            if (filePath != null) {
                filePath = null;
                selectedImage.setImageBitmap(null);
                selectedImage.setVisibility(View.GONE);
                deleteImageButton.setVisibility(View.GONE);
                if (tempImageFile != null) deleteImage();
            }
        });

        startCamera.setOnClickListener(task ->{
            dispatchTakePictureIntent();
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                tempImageFile = createTempImageFile();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error creating temp file: " + e);
            }
            if (tempImageFile != null) {
                Uri tempImageUri = FileProvider.getUriForFile(this, "hu.koncsik.fileprovider", tempImageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    private void deleteImage() {
        if (filePath != null) {
            File imageFile = new File(filePath.getPath());
            if (imageFile.exists()) {
                if (imageFile.delete()) {
                    Log.i(LOG_TAG, "Image deleted successfully");
                    selectedImage.setImageBitmap(null);
                    selectedImage.setVisibility(View.GONE);
                    deleteImageButton.setVisibility(View.GONE);
                    filePath = null;
                } else {
                    Log.e(LOG_TAG, "Error deleting image");
                }
            }
        }
    }
    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                selectedImage.setImageBitmap(bitmap);
                selectedImage.setVisibility(View.VISIBLE);
                deleteImageButton.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error load image: " + e);
            }
        }else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                filePath = Uri.fromFile(tempImageFile);
                Bitmap bitmap = BitmapFactory.decodeFile(tempImageFile.getAbsolutePath());
                selectedImage.setImageBitmap(bitmap);
                selectedImage.setVisibility(View.VISIBLE);
                deleteImageButton.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error load image: " + e);
            }
        } else if (requestCode == REQUEST_CODE_SAVE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri saveUri = data.getData();
            Log.d(LOG_TAG, "Save uri-->" + saveUri.getPath());
            Context context = this;
            new DownloadImageTask(context, imageSaveUri, saveUri).execute();
        }
    }


    public void startChatFragment(Intent intent, String imageUri) {
        imageSaveUri = imageUri;
        ((Activity) this).startActivityForResult(intent, REQUEST_CODE_SAVE_IMAGE);
    }

    private void uploadImageToFirebaseStorage(Uri filePath, Message_1 message) {
        if (filePath != null) {
            StorageReference imageRef = mStorageRef.child("images/" + UUID.randomUUID().toString());
            ProgressBar progressBar = findViewById(R.id.upload_progress_bar);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);

            UploadTask uploadTask = imageRef.putFile(filePath);
            uploadTask.addOnProgressListener(taskSnapshot -> {
                int progress = (int) (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                progressBar.setProgress(progress);
            });
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            message.setImageUrl(imageUrl);
                            mItems.update("messages", FieldValue.arrayUnion(message))
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(LOG_TAG, "Message added successfully");
                                        progressBar.setVisibility(View.GONE);
                                    })
                                    .addOnFailureListener(e -> {
                                        messages.remove(message);
                                        Toast.makeText(Chat.this, "Error send message :(", Toast.LENGTH_LONG).show();
                                        Log.w(LOG_TAG, "Error adding message", e);
                                        progressBar.setVisibility(View.GONE);
                                    });
                        });
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(LOG_TAG, "Image upload failed: ", exception);
                        progressBar.setVisibility(View.GONE);
                    });
        }

           /* imageRef.putFile(filePath)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            message.setImageUrl(imageUrl);
                            mItems.update("messages", FieldValue.arrayUnion(message))
                                    .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Message added successfully"))
                                    .addOnFailureListener(e -> {
                                        messages.remove(message);
                                        Toast.makeText(Chat.this, "Error send message :(", Toast.LENGTH_LONG).show();
                                        Log.w(LOG_TAG, "Error adding message", e);
                                    });
                        });
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(LOG_TAG, "Image upload failed: ", exception);
                    });
        }*/
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_delete:
                deleteChat();
                return true;
            case R.id.menu_members:
                members();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteChat(){
        mItems = mFirestorm.collection("messages").document(messageFirebaseId);
        mItems.delete().addOnSuccessListener(aVoid -> {
            Log.d(LOG_TAG, "Chat deleted");
            onBackPressed();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error deletd chat :(", Toast.LENGTH_LONG).show();
                Log.w(LOG_TAG, "Error deleted chat", e);
            });
    }

    private void members(){
        Intent intent = new Intent(this, GroupsChat.class);
        intent.putExtra("chatItem", chat);
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_members);
        menuItem.setVisible(chat.isGroup());
        return true;
    }


    private void attachCheatListener() {
        new Thread(() -> {
            while (mItems == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "Error sleep: " + e);
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> listenerRegistration = mItems.addSnapshotListener((value, error) -> {
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
            }));
        }).start();
    }

    private void updateCheat(DocumentSnapshot documentSnapshot){
        chat = documentSnapshot.toObject(ChatItem.class);
        if(chat.getMembers() != null && !chat.getMembers().contains(loggedUser.getEmail())){
            Toast.makeText(this, "I was kicked out of the group! :( ", Toast.LENGTH_LONG).show();
            onBackPressed();
        }
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
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        attachCheatListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachCheatListener();
    }



}

