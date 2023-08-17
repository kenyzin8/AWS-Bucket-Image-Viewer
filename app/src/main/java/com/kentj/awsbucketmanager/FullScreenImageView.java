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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

public class FullScreenImageView extends AppCompatActivity {

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
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageUrls);
        viewPager2.setAdapter(adapter);
        viewPager2.setCurrentItem(position, false);

        btnBackFullScreen = findViewById(R.id.btnBackFullScreen);
        fileName = findViewById(R.id.tvFileName);
        fileName.setText(imageFileName);

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