package hu.koncsik.assync;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ActivityCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


import hu.koncsik.HomeActivity;
import hu.koncsik.R;

public class DownloadImageTask extends AsyncTask<Void, Integer, Void> {
    private NotificationManagerCompat notificationManager;
    private static final String LOG_TAG = DownloadImageTask.class.toString();

    private String imageSaveUri;
    private Uri uri;
    private Context context;
    private int notyId = -1;
    private static AtomicInteger notificationIdCounter = new AtomicInteger(0);


    public DownloadImageTask(Context context, String imageSaveUri, Uri uri) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);
        this.imageSaveUri = imageSaveUri;
        this.uri = uri;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();


    }

    @Override
    protected Void doInBackground(Void... imageUrls) {

        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageSaveUri);

        try {
            final File localFile = File.createTempFile("temp_image", "jpg");
            final CountDownLatch latch = new CountDownLatch(1);
            imageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                saveFile(localFile);
                latch.countDown();
            }).addOnFailureListener(e -> {
                latch.countDown();
            }).addOnProgressListener(snapshot -> {
                int progress = (int) (((double) snapshot.getBytesTransferred() / snapshot.getTotalByteCount()) * 100);
                publishProgress(progress);
            });
            latch.await();
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error download file", e);
            Toast.makeText(context, "Some problem downloading the file :(", Toast.LENGTH_LONG).show();
        }
        return null;
    }



        @Override
    protected void onProgressUpdate(Integer... values) {

        super.onProgressUpdate(values);
        int progress = values[0];
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)return;
        if(notyId == -1) notyId = notificationIdCounter.getAndIncrement();

        Intent intent = new Intent(context, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notyId, intent, PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "DownlandChanel")
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(pendingIntent)
                .setContentTitle("Image Download")
                .setContentText("Downloading image: " + progress + "%")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, progress, false);

        notificationManager.notify(notyId, builder.build());

    }

    private void saveFile(File localFile){
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(Uri.fromFile(localFile));
            if (inputStream != null) {
                if(!localFile.exists()) {
                    localFile.createNewFile();
                }
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    localFile.delete();
                }
            }
       } catch (Exception e) {
        Log.d(LOG_TAG, "Error: ", e);
    }
    }



}
