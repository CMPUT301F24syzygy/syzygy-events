package com.syzygy.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.content.Context;

import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.syzygy.events.database.User;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit Test class for the user model
 * @author Caly Zheng
 * @version 1.0
 * @since 05nov2024
 */

public class UserTest {

    private static boolean setUpComplete = false;
    static User testUser;
    private static final TestDatabase db = new TestDatabase();

    @Before
    public void createDb() throws InterruptedException {
        if (!setUpComplete){

            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            db.createDb(context);

            //create user
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
                fail("User creation timed out");
            }
        }
        setUpComplete = true;

    }

    @AfterClass
    public static void closeDb() {
        System.out.println("Deleting User");
        System.out.println(testUser.getDocumentID());
        TestDatabase.firestore.collection("users").document(testUser.getDocumentID())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Delete", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Delete", "Error deleting document", e));
    }


    /**
     * Tests the Name attribute of the user model
     */
    @Test
    public void testUserName() {
        assertEquals(testUser.getName(), "testName");
        if (testUser.setName("NewTestName")){
            assertEquals(testUser.getName(), "NewTestName");
        }else{
            fail();
        }
    }


    /**
     * Tests the Description attribute of the User model
     */
    @Test
    public void testUserDescription() {
        assertEquals(testUser.getDescription(), "TEST");
        testUser.setDescription("description description");
        assertEquals(testUser.getDescription(), "description description");
    }


    /**
     * Tests the Email attribute of the User model
     */
    @Test
    public void testUserEmail() {
        assertEquals(testUser.getEmail(), "abc@xyz.com");

        if (testUser.setEmail("janedoe@gmail.com")){
            assertEquals(testUser.getEmail(), "janedoe@gmail.com");
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
        assertEquals(testUser.getPhoneNumber(), "1234567890");

        if (testUser.setPhoneNumber("0987654321")){
            assertEquals(testUser.getPhoneNumber(), "0987654321");
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
        assertEquals(testUser.getAdminNotifications(), false);

        if (testUser.setAdminNotifications(true)){
            assertEquals(testUser.getAdminNotifications(), true);
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
        assertEquals(testUser.getOrganizerNotifications(), false);

        if (testUser.setOrganizerNotifications(true)){
            assertEquals(testUser.getOrganizerNotifications(), true);
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
        assertEquals(testUser.isAdmin(), false);

        if (testUser.makeAdmin(true)){
            assertEquals(testUser.isAdmin(), true);
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

