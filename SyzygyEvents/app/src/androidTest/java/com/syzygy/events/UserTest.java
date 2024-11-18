package com.syzygy.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static org.junit.Assert.assertEquals;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
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
 * Unit Test class for the user model
 * @author Caly Zheng
 * @version 1.0
 * @since 05nov2024
 */

public class UserTest {

    private static FirebaseApp TEST_INSTANCE;
    private static boolean setUpComplete = false;
    static FirebaseFirestore firestore;
    Database testDB;
    Resources constants;
    static User testuser;

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
            //create user

            final CountDownLatch latch = new CountDownLatch(1);
            User.NewInstance(testDB, "testDeviceId18", "testName", "TEST", null, "", "abc@xyz.com", "1234567890", false, false, false, (instance, success) -> {
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
        System.out.println("Deleting User");
        System.out.println(testuser.getDocumentID());
        firestore.collection("users").document(testuser.getDocumentID())
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
     * Tests the Name attribute of the user model
     */
    @Test
    public void testUserName() {
        assertEquals(testuser.getName(), "testName");
        if (testuser.setName("NewTestName")){
            assertEquals(testuser.getName(), "NewTestName");
        }else{
            fail();
        }
    }


    /**
     * Tests the Description attribute of the User model
     */
    @Test
    public void testUserDescription() {
        assertEquals(testuser.getDescription(), "TEST");
        testuser.setDescription("description description");
        assertEquals(testuser.getDescription(), "description description");
    }


    /**
     * Tests the Email attribute of the User model
     */
    @Test
    public void testUserEmail() {
        assertEquals(testuser.getEmail(), "abc@xyz.com");

        if (testuser.setEmail("janedoe@gmail.com")){
            assertEquals(testuser.getEmail(), "janedoe@gmail.com");
        }
        else{
            fail();
        }
    }


    /**
     * Tests the Phone attribute of the User model
     */
    @Test
    public void testUserPhone() {
        assertEquals(testuser.getPhoneNumber(), "1234567890");

        if (testuser.setPhoneNumber("0987654321")){
            assertEquals(testuser.getPhoneNumber(), "0987654321");
        }
        else{
            fail();
        }
    }

    /**
     * Tests the admin notification attribute of the User model
     */
    @Test
    public void testUserAdminNotifs() {
        assertEquals(testuser.getAdminNotifications(), false);

        if (testuser.setAdminNotifications(true)){
            assertEquals(testuser.getAdminNotifications(), true);
        }
        else{
            fail();
        }
    }


    /**
     * Tests the organization notification attribute of the User model
     */
    @Test
    public void testUserOrgNotifs() {
        assertEquals(testuser.getOrganizerNotifications(), false);

        if (testuser.setOrganizerNotifications(true)){
            assertEquals(testuser.getOrganizerNotifications(), true);
        }
        else{
            fail();
        }
    }

    /**
     * Tests the admin attribute of the User model
     */
    @Test
    public void testUserAdminStatus() {
        assertEquals(testuser.isAdmin(), false);

        if (testuser.makeAdmin(true)){
            assertEquals(testuser.isAdmin(), true);
        }
        else{
            fail();
        }
    }


    /**
     * Tests the update method of the User model
     */
    /*
    public void onComplete(boolean success){;}
    @Test
    public void testUserUpdate() {
        Set<Integer> usersetinteger;
        usersetinteger = testuser.update("John Doe", "-", "john@gmail.com", "1112223333", true, true, false, this::onComplete);

        assertEquals(usersetinteger.size(), 0);

        assertEquals(testuser.getName(), "John Doe");
        assertEquals(testuser.getDescription(), "-");
        assertEquals(testuser.getEmail(), "john@gmail.com");
        assertEquals(testuser.getPhoneNumber(), "1112223333");
        assertEquals(testuser.getOrganizerNotifications(), true);
        assertEquals(testuser.getAdminNotifications(), true);
        assertEquals(testuser.isAdmin(), false);
    }
    */



}

