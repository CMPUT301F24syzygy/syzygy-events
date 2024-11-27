package com.syzygy.events.ModelTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.User;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit Test class for the facility model
 * @author Caly Zheng
 * @version 1.0
 * @since 26nov2024
 */
public class FacilityTest {
    private static boolean setUpComplete = false;
    private static final TestDatabase db = new TestDatabase();
    static User testUser;
    static Facility testFacility;
    static Facility testFacility2;

    @Before
    public void createDb() throws InterruptedException {
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

            final CountDownLatch latch1 = new CountDownLatch(1);
            Facility.NewInstance(db.testDB, "Test Facility", new GeoPoint(50, 50), "1239 West Georgia, Vancouver, Canada", "Home away from home", null, testUser.getDocumentID(), (instance, success) -> {
                if (success) {
                    testFacility = instance;
                    latch1.countDown();
                }
                else {
                    fail("facility was not created");
                    latch1.countDown();
                }
            });

            if (!latch1.await(60, TimeUnit.SECONDS)) {
                fail("facility creation timed out");
            }

            final CountDownLatch latch2 = new CountDownLatch(1);
            Facility.NewInstance(db.testDB, "Test Facility 2", new GeoPoint(60, 60), "410 West Georgia, Vancouver, Canada", "Fruit Office", null, testUser.getDocumentID(), (instance, success) -> {
                if (success) {
                    testFacility2 = instance;
                    latch2.countDown();
                }
                else {
                    fail("facility was not created");
                    latch2.countDown();
                }
            });

            if (!latch2.await(60, TimeUnit.SECONDS)) {
                fail("facility creation timed out");
            }
        }
        setUpComplete = true;
    }

    @AfterClass
    public static void closeDb() {
        TestDatabase.firestore.collection("facilities").document(testFacility.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));

        TestDatabase.firestore.collection("facilities").document(testFacility2.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));

        TestDatabase.firestore.collection("users").document(testUser.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));
    }

    @Test
    public void testFacilityAttributes() {
        assertEquals("Test Facility", testFacility.getName());
        assertTrue(testFacility.setName("Test Name Change"));
        assertEquals("Test Name Change", testFacility.getName());

        assertEquals(new GeoPoint(50,50), testFacility.getLocation());
        assertEquals(new LatLng(50, 50), testFacility.getLatLngLocation());
        assertTrue(testFacility.setLocation(new GeoPoint(60, 60)));
        assertEquals(new GeoPoint(60, 60), testFacility.getLocation());
        assertEquals(new LatLng(60, 60), testFacility.getLatLngLocation());

        assertEquals("1239 West Georgia, Vancouver, Canada", testFacility.getAddress());
        assertTrue(testFacility.setAddress("410 West Georgia, Vancouver, Canada"));
        assertEquals("410 West Georgia, Vancouver, Canada", testFacility.getAddress());

        assertEquals("Home away from home", testFacility.getDescription());
        assertTrue(testFacility.setDescription("Fruit Office"));
        assertEquals("Fruit Office", testFacility.getDescription());

        assertEquals(testUser.getDocumentID(), testFacility.getOrganizerID());
    }

    @Test
    public void testUpdateFacility() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        testFacility2.update("Test Facility Change", new GeoPoint(50, 50), "1239 West Georgia, Vancouver, Canada", "Home away from home", (success) -> {
            latch.countDown();
        });
        if (!latch.await(60, TimeUnit.SECONDS)) {
            fail("facility update timed out");
        }

        assertEquals("Test Facility Change", testFacility2.getName());

        assertEquals(new GeoPoint(50,50), testFacility2.getLocation());
        assertEquals(new LatLng(50, 50), testFacility2.getLatLngLocation());

        assertEquals("1239 West Georgia, Vancouver, Canada", testFacility2.getAddress());
        assertEquals("Home away from home", testFacility2.getDescription());
    }
}
