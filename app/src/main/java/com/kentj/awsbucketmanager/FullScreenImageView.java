package com.kentj.awsbucketmanager;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

interface PermissionRequestListener {
    void onRequestPermission(String imageUrl);
}

public class FullScreenImageView extends AppCompatActivity implements PermissionRequestListener {
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 1;
    private ImageView btnBackFullScreen, btnDelete;
    private TextView fileName;
    private String imageUrl;
    private String imageFileName;
    private ArrayList<String> imageUrls;

    private String BUCKETEER_AWS_ACCESS_KEY_ID = "";
    private String BUCKETEER_AWS_REGION = "";
    private String BUCKETEER_AWS_SECRET_ACCESS_KEY = "";
    private String BUCKETEER_BUCKET_NAME = "";
    private String currentFile = "";
    private int currentFileIndex = -1;
    private AdView viewImageAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image_view);

        imageUrl = getIntent().getStringExtra("IMAGE_URL");
        imageFileName = getIntent().getStringExtra("FILE_NAME");
        int position = getIntent().getIntExtra("POSITION", 0);

        BUCKETEER_AWS_ACCESS_KEY_ID = getIntent().getStringExtra("ACCESS_KEY_ID");
        BUCKETEER_AWS_REGION = getIntent().getStringExtra("REGION");
        BUCKETEER_AWS_SECRET_ACCESS_KEY = getIntent().getStringExtra("SECRET_ACCESS_KEY");
        BUCKETEER_BUCKET_NAME = getIntent().getStringExtra("BUCKET_NAME");

        imageUrls = getIntent().getStringArrayListExtra("IMAGE_URLS");

        currentFile = imageUrls.get(position);
        currentFileIndex = position;

        ViewPager2 viewPager2 = findViewById(R.id.imageViewPager);
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageUrls, this);
        viewPager2.setAdapter(adapter);
        viewPager2.setCurrentItem(position, false);

        btnBackFullScreen = findViewById(R.id.btnBackFullScreen);
        fileName = findViewById(R.id.tvFileName);
        fileName.setText(imageFileName);

        viewImageAdView = findViewById(R.id.viewImageAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        viewImageAdView.loadAd(adRequest);
        viewImageAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }
        });

        btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View customDialogView = LayoutInflater.from(FullScreenImageView.this).inflate(R.layout.delete_dialog_custom, null);
                AlertDialog dialog = new AlertDialog.Builder(FullScreenImageView.this)
                        .setView(customDialogView)
                        .create();
                Button yesButton = customDialogView.findViewById(R.id.dialogYesButtonDelete);
                Button noButton = customDialogView.findViewById(R.id.dialogNoButtonDelete);
                yesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DeleteObjectTask deleteTask = new DeleteObjectTask(FullScreenImageView.this);
                        deleteTask.execute(currentFile);
                        yesButton.setEnabled(false);
                    }
                });
                noButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                String currentImageUrl = imageUrls.get(position);
                String imageFileName = extractFileNameFromUrl(currentImageUrl);

                currentFile = imageUrls.get(position);
                currentFileIndex = position;

                System.out.println(currentFile);

                if (imageFileName != null) {
                    fileName.setText(imageFileName);
                }
            }
        });

        btnBackFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FullScreenImageView.super.onBackPressed();
            }
        });
    }

    @Override
    public void onRequestPermission(String imageUrl) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE_PERMISSION);
            } else {
                saveImage(imageUrl, extractFileNameFromPresignedUrl(imageUrl));
            }
        } else {
            saveImage(imageUrl, extractFileNameFromPresignedUrl(imageUrl));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage(imageUrl, extractFileNameFromPresignedUrl(imageUrl));
            } else {
                Toasty.error(this, "This app does not have permission to write to storage. Please go to Settings > Apps > AWS Bucket Image Viewer > Permissions, and enable Storage.", Toast.LENGTH_LONG, true).show();
            }
        }
    }

    private void saveImage(String imageUrl, String fileName) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        saveBitmapToGallery(resource, fileName);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void saveBitmapToGallery(Bitmap bitmap, String fileName) {
        String savedImagePath = null;
        if (fileName.lastIndexOf(".") > 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        String imageFileName = fileName + ".jpg";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/AWSBucketManager/");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            savedImagePath = imageFile.getAbsolutePath();
            try {
                OutputStream fOut = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            galleryAddPic(savedImagePath);
            Toasty.success(this, "Image Saved!", Toast.LENGTH_SHORT, true).show();
        }
        else{
            Toasty.error(this, "Failed to save image.", Toast.LENGTH_SHORT, true).show();
        }
    }

    private void galleryAddPic(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private String extractFileNameFromPresignedUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "";
        }
    }

    private class DeleteObjectTask extends AsyncTask<String, Void, Void> {
        private Activity mActivity;
        public DeleteObjectTask(Activity activity) {
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(String... strings) {
            String currentFile = strings[0];

            AWSCredentials credentials = new BasicAWSCredentials(BUCKETEER_AWS_ACCESS_KEY_ID, BUCKETEER_AWS_SECRET_ACCESS_KEY);
            AmazonS3 s3Client = new AmazonS3Client(credentials, Region.getRegion(Regions.fromName(BUCKETEER_AWS_REGION)));
            String objectKey = extractObjectKeyFromS3Url(currentFile);
            s3Client.deleteObject(new DeleteObjectRequest(BUCKETEER_BUCKET_NAME, objectKey));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mActivity != null) {
                mActivity.finish();
                ImageGalleryActivity.btnRefresh.setEnabled(true);
                ImageGalleryActivity.btnRefresh.performClick();
            }
        }
    }

    private String extractFileNameFromUrl(String url) {
        String[] parts = url.split("/images/");
        if (parts.length > 1) {
            String filename = parts[1];
            int questionMarkIndex = filename.indexOf("?");
            if (questionMarkIndex > -1) {
                return filename.substring(0, questionMarkIndex);
            } else {
                return filename;
            }
        }
        return null;
    }

    public static String extractObjectKeyFromS3Url(String s3Url) {
        try {
            URL url = new URL(s3Url);
            String path = url.getPath();

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            return path;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}