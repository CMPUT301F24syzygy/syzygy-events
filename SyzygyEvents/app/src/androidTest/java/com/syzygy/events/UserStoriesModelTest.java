package com.syzygy.events;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.RequestCreator;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.Notification;
import com.syzygy.events.database.User;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UserStoriesModelTest {

    private static FirebaseFirestore firestore;
    private static Database testDB;
    private static Resources constants;
    private static Context context;

    private static String random;

    private static int instances = 0;

    @BeforeClass
    public static void setUp(){

        SyzygyApplication.NO_DATABASE = true;
        context = ApplicationProvider.getApplicationContext();
        constants = context.getResources();

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_TEST_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_TEST_APPLICATION_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build();

        //create database
        FirebaseApp.initializeApp(context, options, "test");
        FirebaseApp TEST_INSTANCE = FirebaseApp.getInstance("test");
        firestore = FirebaseFirestore.getInstance(TEST_INSTANCE);
        testDB = new Database(constants, firestore, null);
        System.out.println("Created");
    }
    @Before
    public void set(){
        random = Integer.toHexString(Instant.now().hashCode());
    }


    private void getTestUser(BiConsumer<User, Runnable> listener){
        instances++;
        Set<Integer> invalidIDs = User.NewInstance(
                testDB, random+"u"+instances, "Name"+instances, "Des"+instances,
                null, "", "email"+instances+"@email.com", "12345678901",
                true, true, false, (instance, success) -> {
                    if(!success){
                        fail("failed to create user");
                    }else{
                        listener.accept(instance, () -> {
                            instance.deleteInstance(DatabaseInstance.DeletionType.SILENT, s->{});
                        });
                    }
                });
        assertTrue(invalidIDs.isEmpty());
    }

    private void getTestFacility(User u, BiConsumer<Facility, Runnable> listener){
        instances++;
        Set<Integer> invalidIDs = Facility.NewInstance(testDB, "Name"+instances, new GeoPoint(0,0),
                "Address"+instances, "Des"+instances, null, u.getDocumentID(), (instance, success) -> {
                    if(!success){
                        fail("failed to create facility");
                    }else{
                        u.setFacility(instance);
                        listener.accept(instance, () -> {
                            instance.deleteInstance(DatabaseInstance.DeletionType.SILENT, s->{});
                            u.setFacility(null);
                        });
                    }
                });
        assertTrue(invalidIDs.isEmpty());
    }

    private void getTestFacilityFresh(BiConsumer<Facility, Runnable> listener){
        getTestUser((u, r1) -> {
            getTestFacility(u, (f, r2) -> {
                listener.accept(f, () -> {
                    r2.run();
                    r1.run();
                });
            });
        });
    }

    private static final int EVENT_BEFORE_REG = 0, EVENT_REG = 1, EVENT_AFTER_REG = 2, EVENT_BEFORE_START = 2, EVENT_START = 3, EVENT_END = 4;
    private void getTestEvent(Facility f, int dates, BiConsumer<Event, Runnable> listener){
        instances++;
        Timestamp open, close, start, end;
        Timestamp
                before = new Timestamp(Instant.now().minus(1, ChronoUnit.DAYS)),
                after = new Timestamp(Instant.now().plus(1, ChronoUnit.DAYS));
        switch(dates){
            case EVENT_BEFORE_REG:
                open = after; close = after; start = after; end = after;
            case EVENT_REG:
                open = before; close = after; start = after; end = after;
            case EVENT_AFTER_REG:
                open = before; close = before; start = after; end = after;
            case EVENT_START:
                open = before; close = before; start = before; end = after;
            case EVENT_END:
                open = before; close = before; start = before; end = before;
            default:
                open = after; close = after; start = after; end = after;
        }

        Set<Integer> invalidIDs = Event.NewInstance(testDB, "Name"+instances, null, f.getDocumentID(),
                false, "Des"+instances, 2L, 3L, 0.00,
                open, close, start, end, Event.Dates.EVERY_DAY, (instance, success) -> {
                    if(!success){
                        fail("failed to create event");
                    }else{
                        listener.accept(instance, () -> {
                            instance.deleteInstance(DatabaseInstance.DeletionType.SILENT, s->{});
                        });
                    }
                });
        assertTrue(invalidIDs.isEmpty());
    }

    private void getTestEventFresh(int dates, BiConsumer<Event, Runnable> listener){
        getTestFacilityFresh((f, r1) -> {
            getTestEvent(f, dates, (e, r2) -> {
                listener.accept(e, () -> {
                    r2.run();
                    r1.run();
                });
            });
        });
    }

    private void getTestEventAssociation(User u, Event e, String status, BiConsumer<EventAssociation, Runnable> listener){
        instances++;
        Set<Integer> invalidIDs = EventAssociation.NewInstance(testDB, e.getDocumentID(), new GeoPoint(0,0),
                status, u.getDocumentID(), (instance, success) -> {
                    if(!success){
                        fail("failed to create event association");
                    }else{
                        listener.accept(instance, () -> {
                            instance.deleteInstance(DatabaseInstance.DeletionType.SILENT, s->{});
                        });
                    }
                });
        assertTrue(invalidIDs.isEmpty());
    }

    private void getTestEventAssociationFreshUser(User u, int dates, String status, BiConsumer<EventAssociation, Runnable> listener){
        getTestEventFresh(dates, (e, r1) -> {
            getTestEventAssociation(u, e, status, (ea, r2) -> {
                listener.accept(ea, () -> {
                    r2.run();
                    r1.run();
                });
            });
        });
    }

    private void getTestNotification(User send, User rec, Event e, BiConsumer<Notification, Runnable> listener){
        instances++;
        Set<Integer> invalidIDs = Notification.NewInstance(testDB, "Subject"+instances, "Body"+instances,
                e.getDocumentID(), rec.getDocumentID(), send.getDocumentID(), (instance, success) -> {
                    if(!success){
                        fail("failed to create notification");
                    }else{
                        listener.accept(instance, () -> {
                            instance.deleteInstance(DatabaseInstance.DeletionType.SILENT, s->{});
                        });
                    }
                });
        assertTrue(invalidIDs.isEmpty());
    }

    private <T extends DatabaseInstance<T>> void getTestInstances(int count, BiConsumer<Integer, BiConsumer<T, Runnable>> getNext, BiConsumer<List<T>, Runnable> listener){
        Runnable l1 = new Runnable() {
            private final List<T> instances = new ArrayList<T>();
            private Runnable runable = () -> {};
            private int i = -1;
            @Override
            public void run() {
                i++;
                if(i>=count){
                    listener.accept(instances, runable);
                    return;
                }
                getNext.accept(i, (u,r)->{
                    runable = then(r, runable);
                    instances.add(u);
                    run();
                });
            }
        };
        l1.run();
    }

    private void getEventWithUsersInWaitlistAndEnrolled(int waitListCount, int enrolledCount, BiConsumer<List<EventAssociation>, Runnable> listener){
        getTestEventFresh(EVENT_REG, (e, r1)->{
            this.<User>getTestInstances(waitListCount+enrolledCount, (i,b)->getTestUser(b), (us, r2) -> {
                this.<EventAssociation>getTestInstances(waitListCount+enrolledCount, (i,b)->{
                    getTestEventAssociation(us.get(i), e, constants.getString(i<waitListCount?R.string.event_assoc_status_waitlist: R.string.event_assoc_status_enrolled), b);
                }, (eas, r3) -> {
                    listener.accept(eas, then(r3, r2, r1));
                });
            });
        });
    }

    private void finish(Runnable ...rs){
        for(Runnable r : rs){
            r.run();
        }
    }

    private Runnable then(Runnable ...rs){
        return () -> finish(rs);
    }

    @Test
    public void US010101(){
        getTestUser((u, r1) -> {
            getTestEventFresh(EVENT_REG, (e, r2) -> {
                CountDownLatch latch = new CountDownLatch(1);
                e.addUserToWaitlist(u, new GeoPoint(0,0), (q,a,s) -> {
                    latch.countDown();
                    assertTrue(s);
                    assertNotNull(a);
                    assertNotNull(a.result);;
                    try{
                        assertEquals(a.result.getUser(), u);
                        assertEquals(a.result.getUserID(), u.getDocumentID());
                        assertEquals(a.result.getEvent(), e);
                        assertEquals(a.result.getEventID(), e.getDocumentID());
                        assertEquals(a.result.getStatus(), constants.getString(R.string.event_assoc_status_waitlist));
                    }finally {
                        try{
                            a.result.deleteInstance(DatabaseInstance.DeletionType.SILENT, sc->{});
                        }finally{
                            r2.run();
                            r1.run();
                        }
                    }
                });
                try {
                    if (!latch.await(10, TimeUnit.SECONDS)) {
                        fail("Join waitlist timed out");
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    r2.run();
                    r1.run();
                }
            });
        });
    }

    @Test
    public void US010102(){
        getTestUser((u,r1)->{
            getTestEventAssociationFreshUser(u, EVENT_REG, constants.getString(R.string.event_assoc_status_waitlist), (ea, r2) -> {
                CountDownLatch latch = new CountDownLatch(1);
                ea.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s->{
                    latch.countDown();
                    try{
                        assertTrue(s);
                        assertFalse(ea.isLegalState());
                    }finally {
                        r2.run();
                        r1.run();
                    }
                });
                try {
                    if (!latch.await(10, TimeUnit.SECONDS)) {
                        fail("Leave waitlist timed out");
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    r2.run();
                    r1.run();
                }
            });
        });
    }

    @Test
    public void US010201() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        Set<Integer> invalidIDs = User.NewInstance(
                testDB, random+"uDevice1", "Name", "Des",
                null, "", "email@email.com", "1234567890",
                true, true, false, (instance, success) -> {
                    assertTrue(success);
                    assertNotNull(instance);
                    try{
                        assertTrue(instance.isLegalState());
                        assertEquals(instance.getName(), "Name");
                        assertEquals(instance.getDocumentID(), random+"uDevice1");
                        assertEquals(instance.getDescription(), "Des");
                        assertEquals(instance.getEmail(), "email@email.com");
                        assertEquals(instance.getPhoneNumber(), "1234567890");
                        assertFalse(instance.isAdmin());
                        assertTrue(instance.getAdminNotifications());
                        assertTrue(instance.getOrganizerNotifications());
                        assertNull(instance.getProfileImage());
                        assertNull(instance.getFacility());
                        assertEquals(instance.getFacilityID(), "");
                        assertEquals(instance.getProfileImageID(), "");
                        latch.countDown();
                    }finally {
                        instance.deleteInstance(DatabaseInstance.DeletionType.SILENT, s->{});
                    }

                });

        assertTrue(invalidIDs.isEmpty());

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US010202() {

        getTestUser((instance, onComplete) -> {

            CountDownLatch latch = new CountDownLatch(1);
            Set<Integer> invalidIDs = instance.update("1", "2", "email4@email.com", "",
                    false, false, true, (success) -> {
                        assertTrue(success);
                        assertNotNull(instance);
                        try {
                            assertTrue(instance.isLegalState());
                            assertEquals(instance.getName(), "1");
                            assertEquals(instance.getDescription(), "2");
                            assertEquals(instance.getEmail(), "email4@email.com");
                            assertEquals(instance.getPhoneNumber(), "");
                            assertTrue(instance.isAdmin());
                            assertFalse(instance.getAdminNotifications());
                            assertFalse(instance.getOrganizerNotifications());
                            assertNull(instance.getProfileImage());
                            assertNull(instance.getFacility());
                            assertEquals(instance.getFacilityID(), "");
                            assertEquals(instance.getProfileImageID(), "");
                            latch.countDown();
                        }finally{
                            onComplete.run();
                        }
                    });

            if(!invalidIDs.isEmpty()){
                onComplete.run();
            }
            assertTrue(invalidIDs.isEmpty());

            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    fail("User creation timed out");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void US010301(){
        //TODO
        //Need image file
    }

    @Test
    public void US010302(){
        //TODO
        //Need image file to remove
    }

    @Test
    public void US010303(){
        //Mainly visual
        getTestUser((u,r)->{
            RequestCreator r2 = Image.getFormatedAssociatedImage(u, Database.Collections.USERS, Image.Options.Circle(10));
            assertNotNull(r2);
            r.run();
        });
    }

    @Test
    public void US010401(){
        //TODO NA applicable to this project part
    }

    @Test
    public void US010402(){
        //TODO NA applicable to this project part
    }
    @Test
    public void US010403(){
        //TODO NA applicable to this project part
    }

    @Test
    public void US010501(){
        CountDownLatch latch = new CountDownLatch(1);
        getEventWithUsersInWaitlistAndEnrolled(3,0,(eas,r1) -> {
            Event e = eas.get(0).getEvent();
            e.getLottery(-1, (q,l,s) -> {
                assertTrue(s);
                try{
                    assertEquals(2,l.result.size());
                    assertTrue(l.filledAllSpots());
                    assertEquals(1,l.notChosen.size());
                    l.execute((q1,d,s2) -> {
                        latch.countDown();
                        try{
                            assertTrue(s2);
                            int enrolled = 0;
                            int waitlist = 0;
                            for(EventAssociation ea : eas){
                                if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_waitlist))){
                                    waitlist++;
                                }else if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_invited))){
                                    enrolled++;
                                }
                            }
                            assertEquals(2, enrolled);
                            assertEquals(1, waitlist);
                        }finally {
                            d.result.forEach(n -> n.deleteInstance(DatabaseInstance.DeletionType.SILENT, s3->{}));
                            d.failedNotifications.forEach(n -> n.deleteInstance(DatabaseInstance.DeletionType.SILENT, s3->{}));
                            r1.run();
                        }

                    }, false);
                }catch (Exception ex){
                    r1.run();
                }
            });
        });

        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void US010502(){
        CountDownLatch latch = new CountDownLatch(1);
        getTestUser((u,r1)->{
            getTestEventAssociationFreshUser(u, EVENT_AFTER_REG, constants.getString(R.string.event_assoc_status_invited), (ea,r2)->{
                ea.getEvent().acceptInvite(u, (q,d,s)->{
                    latch.countDown();
                    try{
                        assertTrue(s);
                        assertEquals(constants.getString(R.string.event_assoc_status_enrolled), ea.getStatus());
                    }finally {
                        finish(r2, r1);
                    }
                });
            });
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void US010503(){
        CountDownLatch latch = new CountDownLatch(1);
        getTestUser((u,r1)->{
            getTestEventAssociationFreshUser(u, EVENT_AFTER_REG, constants.getString(R.string.event_assoc_status_invited), (ea,r2)->{
                ea.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s->{
                    latch.countDown();
                    try{
                        assertTrue(s);
                        assertFalse(ea.isLegalState());
                    }finally {
                        finish(r1);
                    }
                });
            });
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void US010601(){
        //Mainly visual
        CountDownLatch latch = new CountDownLatch(1);
        getTestEventFresh(EVENT_REG, (e, r) -> {
            testDB.getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
                latch.countDown();
                try{
                    assertTrue(s);
                    assertEquals(e,i);
                }finally {
                    r.run();
                }
            });
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void US010602(){
        //Mainly visual
        CountDownLatch latch = new CountDownLatch(1);
        getTestEventFresh(EVENT_REG, (e, r) -> {
            testDB.<Event>getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
                try{
                    assertTrue(s);
                    assertEquals(e,i);
                }catch (Exception ex) {
                    r.run();
                }
                getTestUser((u,r2)->{
                    i.addUserToWaitlist(u, new GeoPoint(0,0), (q,d,s2) -> {
                        latch.countDown();
                        try{
                            assertTrue(s);
                            assertNotNull(d);
                            assertNotNull(d.result);
                            assertTrue(d.result.isLegalState());
                            assertEquals(u, d.result.getUser());
                            assertEquals(constants.getString(R.string.event_assoc_status_waitlist), d.result.getStatus());
                            assertEquals(e, d.result.getEvent());
                        }finally {
                            try{
                                d.result.deleteInstance(DatabaseInstance.DeletionType.SILENT, s3->{});
                            }finally{
                                r2.run();
                                r.run();
                            }
                        }
                    });
                });
            });
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("User creation timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void US010701(){
        //TODO this is done by application which is not set up for testing
    }

    @Test
    public void US010801(){
        //TODO visual

    }









}