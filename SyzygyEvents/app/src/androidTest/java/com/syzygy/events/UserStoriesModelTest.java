package com.syzygy.events;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Timestamp;
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

/**
 * Tests the user stories
 *
 * @author Gareth
 */

public class UserStoriesModelTest {

    private static final TestDatabase db = new TestDatabase();
    private static Context context;
    private static Resources constants;
    private static String random;

    private static int instances = 0;

    @BeforeClass
    public static void setUp() throws InterruptedException {

        SyzygyApplication.NO_DATABASE = true;
        context = ApplicationProvider.getApplicationContext();
        constants = context.getResources();
        db.createDb(context);
    }
    @Before
    public void set(){
        random = Integer.toHexString(Instant.now().hashCode());
    }


    private void getTestUser(BiConsumer<User, Runnable> listener){
        instances++;
        Set<Integer> invalidIDs = User.NewInstance(
                db.testDB, random+"u"+instances, "Name"+instances, "Des"+instances,
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
        Set<Integer> invalidIDs = Facility.NewInstance(db.testDB, "Name"+instances, new GeoPoint(0,0),
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

    private Timestamp after(){
        return new Timestamp(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    private Timestamp before(){
        return new Timestamp(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private static final int EVENT_BEFORE_REG = 0, EVENT_REG = 1, EVENT_AFTER_REG = 2, EVENT_BEFORE_START = 2, EVENT_START = 3, EVENT_END = 4;
    private void getTestEvent(Facility f, int dates, BiConsumer<Event, Runnable> listener, boolean geo){
        instances++;
        Timestamp open, close, start, end;
        Timestamp
                before = before(),
                after = after();
        switch(dates){
            case EVENT_REG:
                open = before; close = after; start = after; end = after;
                break;
            case EVENT_AFTER_REG:
                open = before; close = before; start = after; end = after;
                break;
            case EVENT_START:
                open = before; close = before; start = before; end = after;
                break;
            case EVENT_END:
                open = before; close = before; start = before; end = before;
                break;
            default:
                open = after; close = after; start = after; end = after;
                break;
        }

        Set<Integer> invalidIDs = Event.NewInstance(db.testDB, "Name"+instances, null, f.getDocumentID(),
                geo, "Des"+instances, 2L, 3L, 0.00,
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

    private void getTestEventFresh(int dates, BiConsumer<Event, Runnable> listener, boolean geo){
        getTestFacilityFresh((f, r1) -> {
            getTestEvent(f, dates, (e, r2) -> {
                listener.accept(e, () -> {
                    r2.run();
                    r1.run();
                });
            }, geo);
        });
    }

    private void getTestEventAssociation(User u, Event e, String status, BiConsumer<EventAssociation, Runnable> listener, GeoPoint geo){
        instances++;
        Set<Integer> invalidIDs = EventAssociation.NewInstance(db.testDB, e.getDocumentID(), geo,
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

    private void getTestEventAssociationFreshUser(User u, int dates, String status, BiConsumer<EventAssociation, Runnable> listener, boolean geo, GeoPoint loc){
        getTestEventFresh(dates, (e, r1) -> {
            getTestEventAssociation(u, e, status, (ea, r2) -> {
                listener.accept(ea, then(r2,r1));
            }, loc);
        }, geo);
    }

    private void getTestEventAssociationFresh(int dates, String status, BiConsumer<EventAssociation, Runnable> listener, boolean geo, GeoPoint loc){
        getTestUser((u,r)->{
            getTestEventAssociationFreshUser(u, dates, status, (e,r2)->{
                listener.accept(e,then(r2,r));
            }, geo, loc);
        });
    }

    private void getTestNotification(User send, User rec, Event e, BiConsumer<Notification, Runnable> listener){
        instances++;
        Set<Integer> invalidIDs = Notification.NewInstance(db.testDB, "Subject"+instances, "Body"+instances,
                e.getDocumentID(), rec.getDocumentID(), send.getDocumentID(), false, (instance, success) -> {
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
                    getTestEventAssociation(us.get(i), e, constants.getString(i<waitListCount?R.string.event_assoc_status_waitlist: R.string.event_assoc_status_enrolled), b, null);
                }, (eas, r3) -> {
                    listener.accept(eas, then(r3, r2, r1));
                });
            });
        }, false);
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
    public void US010101() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        getTestUser((u, r1) -> {
            getTestEventFresh(EVENT_REG, (e, r2) -> {
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
            }, false);
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Join waitlist timed out");
        }
    }

    @Test
    public void US010102() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTestUser((u,r1)->{
            getTestEventAssociationFreshUser(u, EVENT_REG, constants.getString(R.string.event_assoc_status_waitlist), (ea, r2) -> {
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
            }, false, null);
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Leave waitlist timed out");
        }
    }

    @Test
    public void US010201() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        Set<Integer> invalidIDs = User.NewInstance(
                db.testDB, random+"uDevice1", "Name", "Des",
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
    public void US010202() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        getTestUser((instance, onComplete) -> {
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

        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
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
    public void US010501() throws InterruptedException {
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

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US010502() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTestUser((u,r1)->{
            getTestEventAssociationFreshUser(u, EVENT_AFTER_REG, constants.getString(R.string.event_assoc_status_invited), (ea,r2)->{
                ea.getEvent().acceptInvite(u, (q,d,s)->{
                    try{
                        assertTrue(s);
                        assertEquals(constants.getString(R.string.event_assoc_status_enrolled), ea.getStatus());
                    }finally {
                        finish(r2, r1);
                        latch.countDown();
                    }
                });
            }, false, null);
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US010503() throws InterruptedException {
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
            }, false, null);
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US010601() throws InterruptedException {
        //Mainly visual
        CountDownLatch latch = new CountDownLatch(1);
        getTestEventFresh(EVENT_REG, (e, r) -> {
            db.testDB.getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
                latch.countDown();
                try{
                    assertTrue(s);
                    assertEquals(e,i);
                }finally {
                    r.run();
                }
            });
        }, false);
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US010602() throws InterruptedException {
        //Mainly visual
        CountDownLatch latch = new CountDownLatch(1);
        getTestEventFresh(EVENT_REG, (e, r) -> {
            db.testDB.<Event>getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
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
        }, false);
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US010701(){
        //TODO this is done by application which is not set up for testing
    }

    @Test
    public void US010801() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTestUser((u,r1)->{
            getTestEventFresh(EVENT_REG, (e,r2)->{
                e.addUserToWaitlist(u, null, (q,ea,s)->{
                    try{
                        assertFalse(s);
                        latch.countDown();
                    }finally {
                        finish(r2,r1);
                    }
                });
            }, true);
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US020101() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTestFacilityFresh((f,r)->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, 3L, 0.0, Timestamp.now(), Timestamp.now(), Timestamp.now(), Timestamp.now(), 0L,
                    (i,s)->{
                try{
                    assertTrue(s);
                    assertNotNull(i);
                    assertTrue(i.isLegalState());
                    assertEquals(i.getDocumentID(), i.getQrHash());
                    assertFalse(i.getQrHash().isBlank());
                    latch.countDown();
                }finally {
                    try{
                        i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s2->{});
                    }finally {
                        r.run();
                    }
                }
                    });
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US020102() throws InterruptedException {
        //Visual
    }


    @Test
    public void US020103() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        getTestUser((u,r)->{
            Facility.NewInstance(db.testDB, "Name"+random, new GeoPoint(0,0),
                    "Address", "Description", null, u.getDocumentID(), (i,s)->{
                try{
                    assertTrue(s);
                    assertNotNull(i);
                    assertEquals("Name"+random, i.getName());
                    assertEquals("Description", i.getDescription());
                    assertEquals(u, i.getOrganizer());
                    assertEquals("Address", i.getAddress());
                    assertNull(i.getImage());
                    i.update("Name2", i.getLocation(), "Address2", "des2", s2->{
                       try{
                           assertTrue(s2);
                           assertEquals("Name2", i.getName());
                           assertEquals("Address2", i.getAddress());
                           assertEquals(u, i.getOrganizer());
                           assertEquals("des2", i.getDescription());
                           latch.countDown();
                       }finally {
                           i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s3->{});
                           r.run();
                       }
                    });
                }catch (Exception ex) {
                    try{
                        i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s2->{});
                    }finally {
                        r.run();
                    }
                }
            });
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }


    @Test
    public void US020201() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getEventWithUsersInWaitlistAndEnrolled(3, 0, (eas,r)->{
            Event e = eas.get(0).getEvent();
            e.getWaitlistUsers((query, data, success) -> {
                try{
                    assertTrue(success);
                    assertEquals(3, data.size());
                    for(EventAssociation ea : eas){
                        assertTrue(data.result.contains(ea));
                        assertEquals(constants.getString(R.string.event_assoc_status_waitlist), ea.getStatus());
                    }
                    latch.countDown();
                }finally {
                    r.run();
                }
            });
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }


    @Test
    public void US020202() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.countDown();
        //TODO
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }


    @Test
    public void US020203() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTestFacilityFresh((f,r)->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), true,
                    "Des", 2L, 3L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        try{
                            assertTrue(s);
                            assertTrue(i.getRequiresLocation());
                            i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                                try{
                                    assertFalse(s2);
                                    latch.countDown();
                                }finally {
                                    i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s3->{});
                                    r.run();
                                }
                            });
                        }catch (Exception ex) {
                            try{
                                i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s2->{});
                            }finally {
                                r.run();
                            }
                        }
                    });
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US020301_Limit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTestFacilityFresh((f,r)->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, 1L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        try{
                            assertTrue(s);
                            i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                                try{
                                    assertTrue(s2);
                                    getTestUser((u,r2)->{
                                        i.addUserToWaitlist(u, null, (q2,d2,s3)->{
                                            try{
                                                assertFalse(s3);
                                            }finally {
                                                i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s4->{});
                                                finish(r2,r);
                                            }
                                            latch.countDown();
                                        });
                                    });
                                }catch (Exception ex){
                                    i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s3->{});
                                    r.run();
                                }
                            });
                        }catch (Exception ex) {
                            try{
                                i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s2->{});
                            }finally {
                                r.run();
                            }
                        }
                    });
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
            fail("User creation timed out");
        }
    }

    @Test
    public void US020301_Optional() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTestFacilityFresh((f,r)->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, -1L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        try{
                            assertTrue(s);
                            i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                                try{
                                    assertTrue(s2);
                                    latch.countDown();
                                }finally {
                                    i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s3->{});
                                    r.run();
                                }
                            });
                        }catch (Exception ex) {
                            try{
                                i.deleteInstance(DatabaseInstance.DeletionType.SILENT, s2->{});
                            }finally {
                                r.run();
                            }
                        }
                    });
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US020401() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.countDown();
        //TODO need image file
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US020402() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.countDown();
        //TODO need image file
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US020501() throws InterruptedException {
        //TODO not applicable
    }

    @Test
    public void US020502() throws InterruptedException {
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

                            latch.countDown();
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

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }

    @Test
    public void US020503() throws InterruptedException {
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
                        try{
                            assertTrue(s2);
                            int enrolled = 0;
                            int waitlist = 0;
                            EventAssociation eae = null;
                            for(EventAssociation ea : eas){
                                if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_waitlist))){
                                    waitlist++;
                                }else if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_invited))){
                                    enrolled++;
                                    eae = ea;
                                }
                            }
                            assertEquals(2, enrolled);
                            assertEquals(1, waitlist);

                            eae.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s4->{});

                            e.getLottery(-1, (q2,l2,s22)->{
                                assertTrue(s22);
                                try{
                                    assertEquals(1,l2.result.size());
                                    assertTrue(l2.filledAllSpots());
                                    assertEquals(0,l2.notChosen.size());
                                    l2.execute((q3,d3,s4) -> {
                                        try{
                                            assertTrue(s4);
                                            int enrolled2 = 0;
                                            int waitlist2 = 0;
                                            for(EventAssociation ea : eas){
                                                if(!ea.isLegalState()) continue;
                                                if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_waitlist))){
                                                    waitlist2++;
                                                }else if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_invited))){
                                                    enrolled2++;
                                                }
                                            }
                                            assertEquals(2, enrolled2);
                                            assertEquals(0, waitlist2);

                                            latch.countDown();
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
                        }catch (Exception e1){
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

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("User creation timed out");
        }
    }










}
