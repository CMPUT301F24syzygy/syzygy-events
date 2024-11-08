package com.syzygy.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.User;
import com.syzygy.events.ui.SignupActivity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
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

public class EventTest {
    private static FirebaseApp TEST_INSTANCE;
    private static boolean setUpComplete = false;
    FirebaseFirestore firestore;
    Database testDB;
    Resources constants;
    static Event testevent;


    @Before
    public void createDb() throws InterruptedException, ParseException {
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

            //create event
            final CountDownLatch latch = new CountDownLatch(1);
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Event.NewInstance(testDB, "Yoga Class", (Uri) null, "pVSCFZI5Zk0LRsls499G", false, "Come join yoga class!", 2L, 2L, 20.00, new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/01 12:00"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/02 16:30"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/11 12:00"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/12 16:30"))), Event.Dates.NO_REPEAT, (instance, success) -> {
                if (success) {
                    testevent = instance;
                    // Indicate that the operation is complete
                    System.out.println("event was created");
                    latch.countDown();
                } else {
                    fail("event was not created in db");
                    latch.countDown(); // Ensure latch is decremented
                }
            });

            if (!latch.await(60, TimeUnit.SECONDS)) {
                fail("event creation timed out");
            }
        }
        setUpComplete = true;

    }

    @AfterClass
    public static void closeDb() {
        testevent.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, success -> {});
    }

    @Test
    public void testEventAttributes() throws ParseException {
        assertEquals(testevent.getTitle(), "Yoga Class");
        assertTrue(testevent.setTitle("Pilates Class"));
        assertEquals(testevent.getTitle(), "Pilates Class");

        assertEquals(testevent.getFacilityID(), "pVSCFZI5Zk0LRsls499G");

        assertFalse(testevent.getRequiresLocation());

        assertEquals(testevent.getDescription(), "Come join yoga class!");
        assertTrue(testevent.setDescription("Come join pilates class!"));
        assertEquals(testevent.getDescription(), "Come join pilates class!");

        assertEquals(2L, (long) testevent.getCapacity());
        assertEquals(2L, (long) testevent.getWaitlistCapacity());

        assertEquals(20.00, (double) testevent.getPrice(), 0.001);
        assertTrue(testevent.setPrice(30.0));
        assertEquals(30.00, (double) testevent.getPrice(), 0.001);

        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        assertEquals(testevent.getOpenRegistrationDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/01 12:00"))));
        assertEquals(testevent.getCloseRegistrationDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/02 16:30"))));
        assertEquals(testevent.getStartDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/11 12:00"))));
        assertEquals(testevent.getEndDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/12 16:30"))));
    }
}
