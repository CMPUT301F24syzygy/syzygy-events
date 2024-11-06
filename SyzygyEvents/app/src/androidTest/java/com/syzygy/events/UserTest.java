package com.syzygy.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.User;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test class for the user model
 * @author Caly Zheng
 * @version 1.0
 * @since 05nov2024
 */

public class UserTest {

    private static FirebaseApp TEST_INSTANCE;
    private static boolean setUpComplete = false;
    FirebaseFirestore firestore;
    Database testDB;
    Resources constants;
    static User testuser;


    @Before
    public void createDb() throws InterruptedException {
        if (!setUpComplete){
            Context context = ApplicationProvider.getApplicationContext();
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

            CountDownLatch latch = new CountDownLatch(1);

            //create user
            User.NewInstance(testDB, "testDeviceId1", "testName", "TEST", "", "", "abc@xyz.com", "1234567890", false, false, false, (instance, success) -> {
                if (success) {
                    testuser = instance;
                    // Indicate that the operation is complete
                    latch.countDown();
                } else {
                    fail("User was not created in db");
                    latch.countDown(); // Ensure latch is decremented
                }
            });

            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }
        }
        setUpComplete = true;

    }

    @AfterClass
    public static void closeDb() throws IOException {
        testuser.deleteInstance(success -> {});
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
     * Tests the ProfileImageID attribute of the User model
     */
    @Test
    public void testUserProfileImageID() {
        //TODO: Finish this test once profileimageID has been implemented and used
        /*
        assertEquals(testuser.getProfileImageID(), "");
        testuser.setProfileImageID("");
        assertEquals(testuser.getProfileImageID(), "");
        */
    }


    /**
     * Tests the FacilityID attribute of the User model
     */
    @Test
    public void testUserFacilityID() {
        //TODO: get example of facilityID from DB
        /*
        assertEquals(testuser.getFacilityID(), "");
        testuser.setDescription("");
        assertEquals(testuser.getFacilityID(), "d");
        */
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


}

