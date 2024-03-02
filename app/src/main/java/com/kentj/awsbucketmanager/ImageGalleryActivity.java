package com.kentj.awsbucketmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageGalleryActivity extends AppCompatActivity {

    private String BUCKETEER_AWS_ACCESS_KEY_ID = "";
    private String BUCKETEER_AWS_REGION = "";
    private String BUCKETEER_AWS_SECRET_ACCESS_KEY = "";
    private String BUCKETEER_BUCKET_NAME = "";
    private AmazonS3 s3Client;
    private RecyclerView imageRecyclerView;
    private ImageAdapter imageAdapter;
    private ArrayList<String> imageUrls = new ArrayList<>();
    ImageView btnBack;
    public static ImageView btnRefresh;
    private AdView loggedAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_gallery);

        Intent intent = getIntent();
        BUCKETEER_AWS_ACCESS_KEY_ID = intent.getStringExtra("ACCESS_KEY_ID");
        BUCKETEER_AWS_REGION = intent.getStringExtra("REGION");
        BUCKETEER_AWS_SECRET_ACCESS_KEY = intent.getStringExtra("SECRET_ACCESS_KEY");
        BUCKETEER_BUCKET_NAME = intent.getStringExtra("BUCKET_NAME");

//        String folderName = getIntent().getStringExtra("folderName"); unused

        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        imageAdapter = new ImageAdapter(this, imageUrls, BUCKETEER_AWS_ACCESS_KEY_ID, BUCKETEER_AWS_REGION, BUCKETEER_AWS_SECRET_ACCESS_KEY, BUCKETEER_BUCKET_NAME);
        imageRecyclerView.setAdapter(imageAdapter);

        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        loggedAdView = findViewById(R.id.loggedAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        loggedAdView.loadAd(adRequest);
        loggedAdView.setAdListener(new AdListener() {
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


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View customDialogView = LayoutInflater.from(ImageGalleryActivity.this).inflate(R.layout.logout_dialog_custom, null);
                AlertDialog dialog = new AlertDialog.Builder(ImageGalleryActivity.this)
                        .setView(customDialogView)
                        .create();
                Button yesButton = customDialogView.findViewById(R.id.dialogYesButtonExit);
                Button noButton = customDialogView.findViewById(R.id.dialogNoButtonExit);
                yesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ImageGalleryActivity.super.onBackPressed();
                        dialog.dismiss();
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

        initializeS3Client();

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRefresh.setEnabled(false);

                Drawable drawable = btnRefresh.getDrawable();
                drawable.setColorFilter(Color.parseColor("#232323"), PorterDuff.Mode.SRC_IN);
                btnRefresh.setImageDrawable(drawable);

                loadImageUrls();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btnRefresh.setEnabled(true);
                        drawable.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN);
                        btnRefresh.setImageDrawable(drawable);
                    }
                }, 5000);
            }
        });

        loadImageUrls();
    }

    @Override
    public void onBackPressed() {
        View customDialogView = LayoutInflater.from(ImageGalleryActivity.this).inflate(R.layout.logout_dialog_custom, null);
        AlertDialog dialog = new AlertDialog.Builder(ImageGalleryActivity.this)
                .setView(customDialogView)
                .create();
        Button yesButton = customDialogView.findViewById(R.id.dialogYesButtonExit);
        Button noButton = customDialogView.findViewById(R.id.dialogNoButtonExit);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageGalleryActivity.super.onBackPressed();
                dialog.dismiss();
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

    private void initializeS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(BUCKETEER_AWS_ACCESS_KEY_ID, BUCKETEER_AWS_SECRET_ACCESS_KEY);
        s3Client = new AmazonS3Client(credentials, Region.getRegion(Regions.fromName(BUCKETEER_AWS_REGION)));
    }

    private void loadImageUrls() {
        new ListS3ImagesTask().execute();
    }

    private class ListS3ImagesTask extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            ObjectListing objectListing = s3Client.listObjects(new ListObjectsRequest()
                    .withBucketName(BUCKETEER_BUCKET_NAME));

            ArrayList<String> imageUrlList = new ArrayList<>();
            for (S3ObjectSummary os : objectListing.getObjectSummaries()) {
                String key = os.getKey();
                if (key.toLowerCase().endsWith(".jpg") || key.toLowerCase().endsWith(".jpeg") || key.toLowerCase().endsWith(".png")
                        || key.toLowerCase().endsWith(".webp") || key.toLowerCase().endsWith(".gif")) {
                    imageUrlList.add(generatePresignedUrl(BUCKETEER_BUCKET_NAME, key));
                }
            }
            return imageUrlList;
        }

        @Override
        protected void onPostExecute(ArrayList<String> imageUrlList) {
            imageUrls.clear();
            imageUrls.addAll(imageUrlList);
            imageAdapter.notifyDataSetChanged();
        }
    }

    public String generatePresignedUrl(String bucketName, String objectKey) {
        Date expiration = new Date();
        long msec = expiration.getTime();
        msec += 1000 * 60 * 60;
        expiration.setTime(msec);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
}