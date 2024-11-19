package com.syzygy.events;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.syzygy.events.database.Database;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestDatabase {
    public static FirebaseFirestore firestore;
    public Database testDB;
    Context context;
    Resources constants;
    public void createDb(Context userContext) throws  InterruptedException {
        context = userContext;
        final CountDownLatch firebaselatch = new CountDownLatch(1);
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage("com.syzygy.events");

        // Clear out any previous instances
        assert intent != null;
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        assertNotNull("Application context is null", context.getPackageName());
        constants = context.getResources();

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_TEST_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_TEST_APPLICATION_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build();

        FirebaseApp.initializeApp(context, options, "test");
        FirebaseApp TEST_INSTANCE = FirebaseApp.getInstance("test");
        firestore = FirebaseFirestore.getInstance(TEST_INSTANCE);
        testDB = new Database(constants, firestore, null);
        System.out.println("Created");

        // Decrement the latch count to signal task completion
        firebaselatch.countDown();
        // Wait for the background task to finish (with a timeout)
        if (!firebaselatch.await(60, TimeUnit.SECONDS)) {
            fail("Firebase creation timed out");
        }
    }
}
