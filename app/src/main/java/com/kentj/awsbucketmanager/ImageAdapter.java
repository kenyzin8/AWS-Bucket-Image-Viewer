package com.kentj.awsbucketmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.squareup.picasso.Picasso;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private ArrayList<String> imageUrls;
    private Context context;
    private String BUCKETEER_AWS_ACCESS_KEY_ID = "";
    private String BUCKETEER_AWS_REGION = "";
    private String BUCKETEER_AWS_SECRET_ACCESS_KEY = "";
    private String BUCKETEER_BUCKET_NAME = "";
    public ImageAdapter(Context context, ArrayList<String> imageUrls, String accessKeyID, String region, String secretAccessKey, String bucketName) {
        this.context = context;
        this.imageUrls = imageUrls;
        BUCKETEER_AWS_ACCESS_KEY_ID = accessKeyID;
        BUCKETEER_AWS_REGION = region;
        BUCKETEER_AWS_SECRET_ACCESS_KEY = secretAccessKey;
        BUCKETEER_BUCKET_NAME = bucketName;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String imageUrl = imageUrls.get(position);
        holder.progressBar.setVisibility(View.VISIBLE);

        Glide.with(context)
                .load(imageUrl)
                .transform(new CenterInside(),new RoundedCorners(15))
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.imageView);

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, FullScreenImageView.class);
                intent.putExtra("IMAGE_URL", imageUrls.get(position));

                String fileName = extractFileNameFromPresignedUrl(imageUrl);
                intent.putExtra("FILE_NAME", fileName);

                intent.putStringArrayListExtra("IMAGE_URLS", imageUrls);
                intent.putExtra("POSITION", position);

                intent.putExtra("ACCESS_KEY_ID", BUCKETEER_AWS_ACCESS_KEY_ID);
                intent.putExtra("REGION", BUCKETEER_AWS_REGION);
                intent.putExtra("SECRET_ACCESS_KEY", BUCKETEER_AWS_SECRET_ACCESS_KEY);
                intent.putExtra("BUCKET_NAME", BUCKETEER_BUCKET_NAME);

                context.startActivity(intent);
            }
        });
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

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ProgressBar progressBar;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageItemView);
            progressBar = itemView.findViewById(R.id.imageLoadingProgress);
        }
    }
}

