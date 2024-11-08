package com.syzygy.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.User;
import com.syzygy.events.ui.SignupActivity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit Test class for the user model
 * @author Caly Zheng
 * @version 1.0
 * @since 05nov2024
 */

public class UnitTestTemplate {
    private static FirebaseApp TEST_INSTANCE;
    private static boolean setUpComplete = false;
    FirebaseFirestore firestore;
    Database testDB;
    Resources constants;
    static User testuser;


    @Before
    public void createDb() throws InterruptedException {
        if (!setUpComplete){

            final CountDownLatch firebaselatch = new CountDownLatch(1);


            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertNotNull("Application context is null", context.getPackageName());

            constants = context.getResources();

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_TEST_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_TEST_APPLICATION_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build();

            //create database
            FirebaseApp.initializeApp(context, options, "test");
            TEST_INSTANCE = FirebaseApp.getInstance("test");
            firestore = FirebaseFirestore.getInstance(TEST_INSTANCE);
            testDB = new Database(constants, firestore, null);
            // Decrement the latch count to signal task completion
            firebaselatch.countDown();
            // Wait for the background task to finish (with a timeout)
            if (!firebaselatch.await(60, TimeUnit.SECONDS)) {
                fail("Firebase creation timed out");
            }
            //create user

            final CountDownLatch latch = new CountDownLatch(1);
            User.NewInstance(testDB, "testDeviceId5", "testName", "TEST", null, "", "abc@xyz.com", "1234567890", false, false, false, (instance, success) -> {
                if (success) {
                    testuser = instance;
                    // Indicate that the operation is complete
                    System.out.println("User was created");
                    latch.countDown();
                } else {
                    fail("User was not created in db");
                    latch.countDown(); // Ensure latch is decremented
                }
            });

            if (!latch.await(60, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }
        }
        setUpComplete = true;

    }

    @AfterClass
    public static void closeDb() {
        testuser.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, success -> {});
    }
}