package com.syzygy.events;

import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Notification;
import com.syzygy.events.database.User;
import com.syzygy.events.ui.SignupActivity;

import org.hamcrest.Matcher;
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
 * Unit Test class for the notification model
 * @author Caly Zheng
 * @version 1.0
 * @since 08nov2024
 */

public class NotificationTest {
    private static FirebaseApp TEST_INSTANCE;
    private static boolean setUpComplete = false;
    static FirebaseFirestore firestore;
    Database testDB;
    Resources constants;
    static Notification testnotif;

    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    @Before
    public void createDb() throws InterruptedException {
        if (!setUpComplete){

            final CountDownLatch firebaselatch = new CountDownLatch(1);
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            final Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage("com.syzygy.events");
            // Clear out any previous instances
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);



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
            System.out.println("Created");
            // Decrement the latch count to signal task completion
            firebaselatch.countDown();
            // Wait for the background task to finish (with a timeout)
            if (!firebaselatch.await(60, TimeUnit.SECONDS)) {
                fail("Firebase creation timed out");
            }

            final CountDownLatch latch = new CountDownLatch(1);
            Notification.NewInstance(testDB, "New Event", "Join this new event!","9nTWIt5zBWqwv0ZfB0hx", "1f3887f9b773179d", "6215a24cb3292e0e", (instance, success) -> {
                if (success) {
                    testnotif = instance;
                    // Indicate that the operation is complete
                    System.out.println("Notification was created");
                    latch.countDown();
                } else {
                    fail("Notification was not created in db");
                    latch.countDown(); // Ensure latch is decremented
                }
            });


            if (!latch.await(60, TimeUnit.SECONDS)) {
                fail("Notification creation timed out");
            }
        }
        setUpComplete = true;

    }

    @AfterClass
    public static void closeDb() {
        System.out.println("Deleting User");
        System.out.println(testnotif.getDocumentID());
        firestore.collection("users").document(testnotif.getDocumentID())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Delete", "DocumentSnapshot successfully deleted!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Delete", "Error deleting document", e);
                    }
                });
    }

    /**
     * Tests the attributes of the Notification model
     */
    @Test
    public void testNotifAttributes(){
        assertEquals(testnotif.getSubject(), "New Event");
        assertEquals(testnotif.getBody(), "Join this new event!");
        assertEquals(testnotif.getEventID(), "9nTWIt5zBWqwv0ZfB0hx");
        assertEquals(testnotif.getReceiverID(), "1f3887f9b773179d");
        assertEquals(testnotif.getSenderID(), "6215a24cb3292e0e");
    }

    /**
     * Tests the read attribute of the Notification model
     */
    @Test
    public void testNotifReadAttribute(){
        assertFalse(testnotif.getIsRead());
        assertTrue(testnotif.setIsRead(true));
        assertTrue(testnotif.getIsRead());

    }

}
