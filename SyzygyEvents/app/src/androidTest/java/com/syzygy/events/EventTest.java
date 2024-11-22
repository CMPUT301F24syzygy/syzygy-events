package com.syzygy.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.User;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit Test class for the user model
 * @author Caly Zheng
 * @version 1.0
 * @since 05nov2024
 */

public class EventTest {
    private static boolean setUpComplete = false;
    static Event testEvent;
    static User testUser;
    static Facility testFacility;
    private static final TestDatabase db = new TestDatabase();

    public void resetUser(){
        testUser.update("testName", "TEST", "abc@xyz.com", "1234567890",false, false, false, (success) -> {});
    }

    @Before
    public void createDb() throws InterruptedException, ParseException {
        if (!setUpComplete){
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            db.createDb(context);

            final CountDownLatch latch = new CountDownLatch(1);
            DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            User.NewInstance(db.testDB, UUID.randomUUID().toString(), "testName", "TEST", null, "", "abc@xyz.com", "1234567890", false, false, false, (instance, success) -> {
                if (success) {
                    testUser = instance;
                    // Indicate that the operation is complete
                    System.out.println("User was created");
                    latch.countDown();
                } else {
                    fail("User was not created in db");
                    latch.countDown(); // Ensure latch is decremented
                }
            });

            if (!latch.await(60, TimeUnit.SECONDS)) {
                fail("user creation timed out");
            }

            final CountDownLatch flatch = new CountDownLatch(1);
            Facility.NewInstance(db.testDB, "Test", new GeoPoint(51.5074, 0.1278), "test", "test", null, testUser.getDocumentID(), (instance, success) -> {
                if (success) {
                    testFacility = instance;
                    // Indicate that the operation is complete
                    System.out.println("facility was created");
                    flatch.countDown();
                } else {
                    fail("facility was not created in db");
                    flatch.countDown(); // Ensure latch is decremented
                }
            });

            if (!flatch.await(60, TimeUnit.SECONDS)) {
                fail("facility creation timed out");
            }

            final CountDownLatch elatch = new CountDownLatch(1);
            Event.NewInstance(db.testDB, "Yoga Class", (Uri) null, testFacility.getDocumentID(), false, "Come join yoga class!", 2L, 2L, 20.00, new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/01 12:00"))), new Timestamp(Objects.requireNonNull(formatter.parse("2025/11/02 16:30"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/11 12:00"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/12 16:30"))), Event.Dates.NO_REPEAT, (instance, success) -> {
                if (success) {
                    testEvent = instance;
                    // Indicate that the operation is complete
                    System.out.println("event was created");
                    elatch.countDown();
                } else {
                    fail("event was not created in db");
                    elatch.countDown(); // Ensure latch is decremented
                }
            });

            if (!elatch.await(60, TimeUnit.SECONDS)) {
                fail("event creation timed out");
            }
        }
        setUpComplete = true;

    }

    @AfterClass
    public static void closeDb() {
        TestDatabase.firestore.collection("events").document(testEvent.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));

        TestDatabase.firestore.collection("facilities").document(testFacility.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));

        TestDatabase.firestore.collection("users").document(testUser.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));
    }

    @Test
    public void testEventAttributes() throws ParseException {
        assertEquals(testEvent.getTitle(), "Yoga Class");
        assertTrue(testEvent.setTitle("Pilates Class"));
        assertEquals(testEvent.getTitle(), "Pilates Class");

        assertEquals(testEvent.getFacilityID(), testFacility.getDocumentID());

        assertFalse(testEvent.getRequiresLocation());

        assertEquals(testEvent.getDescription(), "Come join yoga class!");
        assertTrue(testEvent.setDescription("Come join pilates class!"));
        assertEquals(testEvent.getDescription(), "Come join pilates class!");

        assertEquals(2L, (long) testEvent.getCapacity());
        assertEquals(2L, (long) testEvent.getWaitlistCapacity());

        assertEquals(20.00, (double) testEvent.getPrice(), 0.001);
        assertTrue(testEvent.setPrice(30.0));
        assertEquals(30.00, (double) testEvent.getPrice(), 0.001);

        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        assertEquals(testEvent.getOpenRegistrationDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/01 12:00"))));
        assertEquals(testEvent.getCloseRegistrationDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2025/11/02 16:30"))));
        assertEquals(testEvent.getStartDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/11 12:00"))));
        assertEquals(testEvent.getEndDate(), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/12 16:30"))));
    }

    @Test
    public void testAddUserToWaitlist(){
        resetUser();

        testEvent.addUserToWaitlist(testUser, null, (q, a, s)->{
            assertTrue(s);
            assertNotNull(a);
            assertNotNull(a.result);
            assertEquals(a.result.getUser(), testUser);
            assertEquals(a.result.getUserID(), testUser.getDocumentID());
            assertEquals(a.result.getEvent(), testEvent);
            assertEquals(a.result.getEventID(), testEvent.getDocumentID());

            assertEquals(q.getCurrentWaitlist(), 1);
        });

        testEvent.getLottery(-1, (e, result, s)->{
            assertTrue((s));
        });
    }
}
