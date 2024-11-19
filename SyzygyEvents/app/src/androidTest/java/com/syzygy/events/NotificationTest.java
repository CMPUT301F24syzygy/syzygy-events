package com.syzygy.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Notification;
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
 * Unit Test class for the notification model
 * @author Caly Zheng
 * @version 1.0
 * @since 08nov2024
 */

public class NotificationTest {
    private static boolean setUpComplete = false;
    static Notification testNotification;
    static Event testEvent;
    static User testUser;
    static User testUser2;
    static Facility testFacility;
    private static final TestDatabase db = new TestDatabase();

    @Before
    public void createDb() throws InterruptedException, ParseException {
        if (!setUpComplete){

            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            db.createDb(context);

            final CountDownLatch latch = new CountDownLatch(1);
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

            final CountDownLatch ulatch = new CountDownLatch(1);
            User.NewInstance(db.testDB, UUID.randomUUID().toString(), "testName2", "TEST2", null, "", "abc2@xyz.com", "1234567890", false, false, false, (instance, success) -> {
                if (success) {
                    testUser2 = instance;
                    // Indicate that the operation is complete
                    System.out.println("User was created");
                    ulatch.countDown();
                } else {
                    fail("User was not created in db");
                    ulatch.countDown(); // Ensure latch is decremented
                }
            });

            if (!ulatch.await(60, TimeUnit.SECONDS)) {
                fail("user 2 creation timed out");
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
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Event.NewInstance(db.testDB, "Yoga Class", (Uri) null, testFacility.getDocumentID(), false, "Come join yoga class!", 2L, 2L, 20.00, new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/01 12:00"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/02 16:30"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/11 12:00"))), new Timestamp(Objects.requireNonNull(formatter.parse("2024/11/12 16:30"))), Event.Dates.NO_REPEAT, (instance, success) -> {
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

            final CountDownLatch nlatch = new CountDownLatch(1);
            Notification.NewInstance(db.testDB, "New Event", "Join this new event!", testEvent.getDocumentID(), testUser2.getDocumentID(), testUser.getDocumentID(), (instance, success) -> {
                if (success) {
                    testNotification = instance;
                    // Indicate that the operation is complete
                    System.out.println("Notification was created");
                    nlatch.countDown();
                } else {
                    fail("Notification was not created in db");
                    nlatch.countDown(); // Ensure latch is decremented
                }
            });


            if (!nlatch.await(60, TimeUnit.SECONDS)) {
                fail("Notification creation timed out");
            }
        }
        setUpComplete = true;

    }

    @AfterClass
    public static void closeDb() {

        TestDatabase.firestore.collection("notifications").document(testNotification.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));

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

        TestDatabase.firestore.collection("users").document(testUser2.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));
    }

    /**
     * Tests the attributes of the Notification model
     */
    @Test
    public void testNotifAttributes(){
        assertEquals(testNotification.getSubject(), "New Event");
        assertEquals(testNotification.getBody(), "Join this new event!");
        assertEquals(testNotification.getEventID(), testEvent.getDocumentID());
        assertEquals(testNotification.getReceiverID(), testUser2.getDocumentID());
        assertEquals(testNotification.getSenderID(), testUser.getDocumentID());
    }

    /**
     * Tests the read attribute of the Notification model
     */
    @Test
    public void testNotifReadAttribute(){
        assertFalse(testNotification.getIsRead());
        assertTrue(testNotification.setIsRead(true));
        assertTrue(testNotification.getIsRead());
    }

}
