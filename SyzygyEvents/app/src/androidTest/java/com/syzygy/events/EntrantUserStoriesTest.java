package com.syzygy.events;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.User;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test class for all current completed user stories relating to entrants
 * @author
 * @version 1.0
 * @since 07nov2024
 */
@RunWith(AndroidJUnit4.class)
public class EntrantUserStoriesTest {
    private static FirebaseApp TEST_INSTANCE;
    private static boolean setUpComplete = false;
    private static FirebaseFirestore firestore;
    private static Database testDB;
    Resources constants;
    private static Event testEvent;
    private static User testUser;

    @Before
    public void createDb() throws InterruptedException {
        if (!setUpComplete) {
            Context context = ApplicationProvider.getApplicationContext();
            constants = context.getResources();

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_TEST_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_TEST_APPLICATION_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build();

            // Create test database
            FirebaseApp.initializeApp(context, options, "test");
            TEST_INSTANCE = FirebaseApp.getInstance("test");
            firestore = FirebaseFirestore.getInstance(TEST_INSTANCE);
            testDB = new Database(context.getResources(), firestore, null);

            // Create a test event
            CountDownLatch eventLatch = new CountDownLatch(1);
            Event.NewInstance(
                    testDB,
                    "Yoga Class",
                    null,
                    null,
                    false,
                    "Advanced Yoga Class",
                    100L,
                    10L,
                    20.0,
                    null,
                    null,
                    null,
                    null,
                    Event.Dates.WEEKDAYS,
                    (event, success) -> {
                        if (success) {
                            testEvent = event;
                        } else {
                            fail("Event was not created in db");
                        }
                        eventLatch.countDown();
                    }
            );
            if (!eventLatch.await(10, TimeUnit.SECONDS)) {
                fail("Event creation timed out");
            }

            // Create mock user
            CountDownLatch userLatch = new CountDownLatch(1);
            User.NewInstance(
                    testDB,
                    "testDeviceId2",
                    "Bob Ross",
                    "Painter",
                    Uri.parse(""),
                    "",
                    "Bob@hotmail.com",
                    "1234567890",
                    false,
                    false,
                    false,
                    (instance, success) -> {
                if (success) {
                    testUser = instance;
                    // Indicate that the operation is complete
                    userLatch.countDown();
                } else {
                    fail("User was not created in db");
                    userLatch.countDown(); // Ensure latch is decremented
                }
            });
            if (!userLatch.await(10, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }

            setUpComplete = true;
        }
    }

    @AfterClass
    public static void closeDb() {
        if (testEvent != null) {
            testEvent.deleteInstance(1, success -> {});
        }
        if (testUser != null) {
            testUser.deleteInstance(1, success -> {});
        }
    }

    /**
     * Tests user story 01.01.01: "As an entrant I want to join the waiting list for a specific event"
     */
    @Test
    public void test01_01_01() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testEvent.addUserToWaitlist(testUser, null, (query, result, success) -> {
            if (success) {
                System.out.println("User added to waitlist successfully");
            } else {
                fail("Failed to add user to waitlist");
            }
            latch.countDown();
        });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Waitlist addition timed out");
        }
    }

    /**
     * Tests user story 01.01.02: "As an entrant I want to leave the waiting list for a specific event"
     */
    @Test
    public void test01_01_02() {
        // todo: implement
    }

    /**
     * Tests user story 01.02.01: "As an entrant I want to provide my personal information such as name, email, and optional phone number in the app"
     */
    @Test
    public void test01_02_01() {
        // todo: implement
    }

    /**
     * Tests user story 01.02.02: "As an entrant I want my profile picture to be deterministically generated from my profile name if I haven't uploaded a profile image yet"
     */
    @Test
    public void test01_02_02() {
        // todo: implement
    }

    /**
     * Tests user story 01.03.01: "As an entrant I want to upload a profile picture for a more personalized experience"
     */
    @Test
    public void test01_03_01() {
        // todo: implement
    }

    /**
     * Tests user story 01.03.02: "As an entrant I want to remove profile picture if need be"
     */
    @Test
    public void test01_03_02() {
        // todo: implement
    }

    /**
     * Tests user story 01.03.03: "As an entrant I want my profile picture to be deterministically generated from my profile name if I haven't uploaded a profile image yet"
     */
    @Test
    public void test01_03_03() {
        // todo: implement
    }

    /**
     * Tests user story 01.04.01: "As an entrant I want to receive notification when chosen from the waiting list"
     */
    @Test
    public void test01_04_01() {
        // todo: implement
    }

    /**
     * Tests user story 01.04.02: "As an entrant I want to receive notification of not chosen on the app"
     */
    @Test
    public void test01_04_02() {
        // todo: implement
    }

    /**
     * Tests user story 01.04.03: "As an entrant I want to opt out of receiving notifications from organizers and admin"
     */
    @Test
    public void test01_04_03() {
        // todo: implement
    }

    /**
     * Tests user story 01.05.01: "As an entrant I want another chance to be chosen from the waiting list if a selected user declines an invitation to register/sign up when chosen to participate in an event"
     */
    @Test
    public void test01_05_01() {
        // todo: implement
    }

    /**
     * Tests user story 01.05.02: "As an entrant I want to be able to accept the invitation to register/sign up when chosen to participate in an event"
     */
    @Test
    public void test01_05_02() {
        // todo: implement
    }

    /**
     * Tests user story 01.05.03: "As an entrant I want to be able to decline an invitation when chosen to participate in an event"
     */
    @Test
    public void test01_05_03() {
        // todo: implement
    }

    /**
     * Tests user story 01.06.01: "As an entrant I want to view event details within the app by scanning the promotional QR code"
     */
    @Test
    public void test01_06_01() {
        // todo: implement
    }

    /**
     * Tests user story 01.06.02: "As an entrant I want to be able to sign up for an event by scanning the QR code"
     */
    @Test
    public void test01_06_02() {
        // todo: implement
    }

    /**
     * Tests user story 01.07.01: "As an entrant I want to be identified by my device so that I don't have to use a username and password"
     */
    @Test
    public void test01_07_01() {
        // todo: implement
    }

    /**
     * Tests user story 01.08.01: "As an entrant I want to be warned before joining a waiting list that requires geolocation"
     */
    @Test
    public void test01_08_01() throws InterruptedException {
        // todo: implement
    }
}
