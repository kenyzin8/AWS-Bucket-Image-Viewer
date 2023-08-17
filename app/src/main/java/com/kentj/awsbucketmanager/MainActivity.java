package com.kentj.awsbucketmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final boolean DEBUG = false;
    private static final String BUCKETEER_AWS_ACCESS_KEY_ID = "";
    private static final String BUCKETEER_AWS_REGION = "";
    private static final String BUCKETEER_AWS_SECRET_ACCESS_KEY = "";
    private static final String BUCKETEER_BUCKET_NAME = "";

    private EditText accessKeyID, region, secretAccessKey, bucketName;
    private Button connectBucket, clearFields, loadSavedIAM;
    private CheckBox saveData;
    private TextView tvConnecting;

    private Handler connectingHandler = new Handler();
    private int dotCount = 0;

    private Runnable connectingRunnable = new Runnable() {
        @Override
        public void run() {
            tvConnecting.setVisibility(View.VISIBLE);
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < dotCount; i++) {
                dots.append(".");
            }

            tvConnecting.setText("Connecting, please wait" + dots.toString());

            dotCount = (dotCount + 1) % 4;
            connectingHandler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accessKeyID = findViewById(R.id.editKeyID);
        region = findViewById(R.id.editRegion);
        secretAccessKey = findViewById(R.id.editSecretAccessKey);
        bucketName = findViewById(R.id.editBucketName);

        connectBucket = findViewById(R.id.connectButton);
        clearFields = findViewById(R.id.clearIAM);
        loadSavedIAM = findViewById(R.id.loadSavedIAM);
        saveData = findViewById(R.id.saveData);

        tvConnecting = findViewById(R.id.tvConnecting);

        tvConnecting.setVisibility(View.INVISIBLE);

        if(DEBUG)
        {
            accessKeyID.setText(BUCKETEER_AWS_ACCESS_KEY_ID);
            region.setText(BUCKETEER_AWS_REGION);
            secretAccessKey.setText(BUCKETEER_AWS_SECRET_ACCESS_KEY);
            bucketName.setText(BUCKETEER_BUCKET_NAME);
        }

        if(checkSharedPreferenceStatus())
        {
            List<String> data = loadSharedPreference();

            accessKeyID.setText(data.get(0));
            region.setText(data.get(1));
            secretAccessKey.setText(data.get(2));
            bucketName.setText(data.get(3));
            saveData.setChecked(true);
        }

        connectBucket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String keyId = accessKeyID.getText().toString().trim();
                String reg = region.getText().toString().trim();
                String secretKey = secretAccessKey.getText().toString().trim();
                String bucket = bucketName.getText().toString().trim();

                if (keyId.isEmpty() && reg.isEmpty() && secretKey.isEmpty() && bucket.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill out all fields!", Toast.LENGTH_SHORT).show();
                    return;
                }

                new ConnectToS3Task().execute(keyId, secretKey, reg, bucket);
                connectBucket.setEnabled(false);

                if(saveData.isChecked())
                {
                    SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences("IAM_CREDENTIALS", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("AUTO_APPLY_CREDENTIALS", true);
                    editor.putString("BUCKETEER_AWS_ACCESS_KEY_ID", keyId);
                    editor.putString("BUCKETEER_AWS_REGION", reg);
                    editor.putString("BUCKETEER_AWS_SECRET_ACCESS_KEY", secretKey);
                    editor.putString("BUCKETEER_BUCKET_NAME", bucket);
                    editor.apply();
                }
                else
                {
                    SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences("IAM_CREDENTIALS", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("AUTO_APPLY_CREDENTIALS", false);
                    editor.putString("BUCKETEER_AWS_ACCESS_KEY_ID", "");
                    editor.putString("BUCKETEER_AWS_REGION", "");
                    editor.putString("BUCKETEER_AWS_SECRET_ACCESS_KEY", "");
                    editor.putString("BUCKETEER_BUCKET_NAME", "");
                    editor.apply();
                }
            }
        });

        clearFields.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFields();
                if(checkSharedPreferenceStatus()) {
                    loadSavedIAM.setVisibility(View.VISIBLE);
                }
            }
        });

        loadSavedIAM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSharedPreferenceStatus())
                {
                    List<String> data = loadSharedPreference();

                    accessKeyID.setText(data.get(0));
                    region.setText(data.get(1));
                    secretAccessKey.setText(data.get(2));
                    bucketName.setText(data.get(3));
                    saveData.setChecked(true);
                    loadSavedIAM.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectingHandler.removeCallbacks(connectingRunnable);
    }

    private List<String> loadSharedPreference()
    {
        List<String> data = new ArrayList<>();
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("IAM_CREDENTIALS", Context.MODE_PRIVATE);

        data.add(sharedPref.getString("BUCKETEER_AWS_ACCESS_KEY_ID", ""));
        data.add(sharedPref.getString("BUCKETEER_AWS_REGION", ""));
        data.add(sharedPref.getString("BUCKETEER_AWS_SECRET_ACCESS_KEY", ""));
        data.add(sharedPref.getString("BUCKETEER_BUCKET_NAME", ""));

        return data;
    }

    private boolean checkSharedPreferenceStatus()
    {
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("IAM_CREDENTIALS", Context.MODE_PRIVATE);

        return sharedPref.getBoolean("AUTO_APPLY_CREDENTIALS", false);
    }

    private void resetFields()
    {
        accessKeyID.setText("");
        region.setText("");
        secretAccessKey.setText("");
        bucketName.setText("");
        saveData.setChecked(false);
    }

    private class ConnectToS3Task extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            String keyId = strings[0];
            String secretKey = strings[1];
            String reg = strings[2];
            String bucket = strings[3];

            try {
                connectingHandler.post(connectingRunnable);

                AWSCredentials credentials = new BasicAWSCredentials(keyId, secretKey);
                AmazonS3 s3 = new AmazonS3Client(credentials);
                s3.setRegion(Region.getRegion(Regions.fromName(reg)));

                return s3.doesBucketExist(bucket);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isConnected) {
            if (isConnected) {
                Toast.makeText(MainActivity.this, "Connected to the bucket successfully!", Toast.LENGTH_SHORT).show();
                tvConnecting.setVisibility(View.INVISIBLE);
                connectingHandler.removeCallbacks(connectingRunnable);

                String keyId = accessKeyID.getText().toString().trim();
                String reg = region.getText().toString().trim();
                String secretKey = secretAccessKey.getText().toString().trim();
                String bucket = bucketName.getText().toString().trim();

                Intent intent = new Intent(MainActivity.this, ImageGalleryActivity.class);
                intent.putExtra("ACCESS_KEY_ID", keyId);
                intent.putExtra("REGION", reg);
                intent.putExtra("SECRET_ACCESS_KEY", secretKey);
                intent.putExtra("BUCKET_NAME", bucket);
                startActivity(intent);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectBucket.setEnabled(true);
                    }
                },5000);
            } else {
                Toast.makeText(MainActivity.this, "Failed to connect to the bucket!", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectBucket.setEnabled(true);
                    }
                },5000);
                tvConnecting.setVisibility(View.INVISIBLE);
                connectingHandler.removeCallbacks(connectingRunnable);
            }
        }
    }

}
