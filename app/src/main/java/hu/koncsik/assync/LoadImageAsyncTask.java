package hu.koncsik.assync;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import hu.koncsik.R;

public class LoadImageAsyncTask extends AsyncTask<StorageReference, Void, Bitmap> {
    private static final String LOG_TAG = LoadImageAsyncTask.class.toString();
    private final WeakReference<ImageView> imageViewWeakReference;
    private Context context;
    private boolean losing = false;
    public LoadImageAsyncTask(ImageView imageView, Context context) {
        imageViewWeakReference = new WeakReference<>(imageView);
        this.context = context;
    }

    @Override
    protected Bitmap doInBackground(StorageReference... storageReferences) {

            StorageReference storageReference = (StorageReference) storageReferences[0];
            String fileName = storageReference.getName();
            File cacheDir = context.getCacheDir();
            File imageFile = new File(cacheDir, fileName);
            Bitmap bitmap = null;
        if (imageFile.exists()) {
            try {
                Log.d(LOG_TAG, "Image load in cache storage");
                FileInputStream fis = new FileInputStream(imageFile);
                bitmap = BitmapFactory.decodeStream(fis);
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error get image in cache storage : " + e.getMessage());
            }
        } else {
            try {
                Log.d(LOG_TAG, "Image load in firebase storage");
                InputStream inputStream = Tasks.await(storageReference.getStream()).getStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
                saveBitmapToCache(bitmap, imageFile);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof StorageException) {
                    Log.d(LOG_TAG, "I'm losing weight on the firebase");
                    losing = true;
                } else {
                    Log.d(LOG_TAG, "Error get image: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, "Error get image: " + e.getMessage());
            }
        }
        return bitmap;
    }
    private void saveBitmapToCache(Bitmap bitmap, File imageFile) {
        try {
            OutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.d(LOG_TAG, "Not exists image in firebase :(");
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        ImageView imageView = imageViewWeakReference.get();

        if (imageView != null) {
            if(bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }else if(losing) {
                Log.d(LOG_TAG, "I'm losing weight on the firebase");
                imageView.setImageResource(R.drawable.ic_money);
            }else{
                Drawable noPhotoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_no_photo);
                imageView.setImageDrawable(noPhotoDrawable);
            }


        }


    }
}