package com.bluetoothwifiofflinechattingfilesharing.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

public class ShowReview {
    ReviewInfo reviewInfo;
    ReviewManager reviewManager;
    Activity activity;
    Context context;

    public ShowReview(Activity activity, Context context) {
        this.activity=activity;
        this.context=context;
        reviewManager = ReviewManagerFactory.create(context);

        reviewManager.requestReviewFlow().addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
            @Override
            public void onComplete(@NonNull Task<ReviewInfo> task) {
                if (task.isSuccessful()) {
                    // We got the ReviewInfo object
                    reviewInfo = task.getResult();
                } else {
                    // There was some problem, log or handle the error code.\
                    Toast.makeText(context, task.getException().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

   public void initiateRatingFlow(){
        Task<Void> flow = reviewManager.launchReviewFlow(activity, reviewInfo);
        flow.addOnCompleteListener(task -> {

            Toast.makeText(context, "Rating Completed 💝", Toast.LENGTH_SHORT).show();
        });
    }
}
