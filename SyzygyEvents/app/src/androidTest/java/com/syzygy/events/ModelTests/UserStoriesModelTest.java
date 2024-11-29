package com.syzygy.events.ModelTests;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.RequestCreator;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.Notification;
import com.syzygy.events.database.User;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

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
import java.util.function.Function;

/**
 * Tests the user stories
 *
 * @author Gareth
 */

//https://stackoverflow.com/questions/9903341/cleanup-after-all-junit-tests/49448319
//public class UserStoriesSuite

public class UserStoriesModelTest {

    private static final TestDatabase db = new TestDatabase();
    private static Context context;
    private static Resources constants;
    private static String random;

    @Rule
    public TestName name = new TestName();

    private static int instances = 0;

    @BeforeClass
    public static void setUp() throws InterruptedException {

        SyzygyApplication.NO_DATABASE = true;
        context = ApplicationProvider.getApplicationContext();
        constants = context.getResources();
        db.createDb(context);
        Log.d("Testing", "BeforeClass");
    }

    private final List<DatabaseInstance<?>> objects = new ArrayList<>();
    private static final List<DatabaseInstance<?>> allObjectsUsed = new ArrayList<>();

    private AssertionError error = null;
    private CountDownLatch latch = null;
    private boolean ignoreDelete = false;

    @After
    public void cleanUp(){
        Log.i("Testing", "After");
        if(!ignoreDelete){
            for(DatabaseInstance<?> i : objects){
                if(i==null) continue;
                i.getDocumentReference().delete();
            }
            allObjectsUsed.addAll(objects);
        }
        objects.clear();
    }

    @Before
    public void beforeTest(){
        Log.i("Testing", "Before");
        random = Integer.toHexString(Instant.now().hashCode());
        error = null;
        latch = new CountDownLatch(1);
        ignoreDelete = false;
    }
    @AfterClass
    public static void clean() throws InterruptedException {
        Log.d("Testing", "AfterClass");
        for(DatabaseInstance<?> i : allObjectsUsed){
            if(i==null) continue;
            CountDownLatch l = new CountDownLatch(1);
            i.getDocumentReference().delete().addOnCompleteListener(t -> l.countDown());
            l.await();
        }
        allObjectsUsed.clear();
    }

    private void completeTest(){
        latch.countDown();
    }

    private boolean asserts(Runnable statements){
        try{
            statements.run();
            return true;
        }catch (AssertionError ex){
            error = ex;
            completeTest();
            return false;
        }
    }

    private void await(long timeout) throws InterruptedException{
        if(!latch.await(timeout, TimeUnit.SECONDS)){
            fail("Timeout");
        };
        if(error != null){
            throw error;
        }
    }

    private void getTestUser(Consumer<User> listener){
        instances++;
        Set<Integer> invalidIDs = User.NewInstance(
                db.testDB, random+"u"+instances, name.getMethodName()+'|'+instances, "Des"+instances,
                null, "", "email"+instances+"@email.com", "12345678901",
                true, true, false, (instance, success) -> {
                    objects.add(instance);
                    if(!success){
                        fail("failed to create user");
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestFacility(User u, Consumer<Facility> listener){
        instances++;
        Set<Integer> invalidIDs = Facility.NewInstance(db.testDB, name.getMethodName()+'|'+instances, new GeoPoint(0,0),
                "Address"+instances, "Des"+instances, null, u.getDocumentID(), (instance, success) -> {
                    objects.add(instance);
                    if(!success){
                        fail("failed to create facility");
                    }else{
                        u.setFacility(instance);
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestFacilityFresh(Consumer<Facility> listener){
        getTestUser(u -> getTestFacility(u, listener));
    }

    private Timestamp after(){
        return new Timestamp(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    private Timestamp before(){
        return new Timestamp(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private static final int EVENT_BEFORE_REG = 0, EVENT_REG = 1, EVENT_AFTER_REG = 2, EVENT_BEFORE_START = 2, EVENT_START = 3, EVENT_END = 4;
    private void getTestEvent(Facility f, int dates, Consumer<Event> listener, boolean geo){
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

        Set<Integer> invalidIDs = Event.NewInstance(db.testDB, name.getMethodName()+'|'+instances, null, f.getDocumentID(),
                geo, "Des"+instances, 2L, 3L, 0.00,
                open, close, start, end, Event.Dates.EVERY_DAY, (instance, success) -> {
                    objects.add(instance);
                    if(!success){
                        fail("failed to create event");
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestEventFresh(int dates, Consumer<Event> listener, boolean geo){
        getTestFacilityFresh(f -> getTestEvent(f, dates, listener, geo));
    }

    private void getTestEventAssociation(User u, Event e, String status, Consumer<EventAssociation> listener, GeoPoint geo){
        instances++;
        Set<Integer> invalidIDs = EventAssociation.NewInstance(db.testDB, e.getDocumentID(), geo,
                status, u.getDocumentID(), (instance, success) -> {
                    objects.add(instance);
                    if(!success){
                        fail("failed to create event association");
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestEventAssociationFreshUser(User u, int dates, String status, Consumer<EventAssociation> listener, boolean geo, GeoPoint loc){
        getTestEventFresh(dates, e -> getTestEventAssociation(u, e, status, listener, loc), geo);
    }

    private void getTestEventAssociationFresh(int dates, String status, Consumer<EventAssociation> listener, boolean geo, GeoPoint loc){
        getTestUser(u-> getTestEventAssociationFreshUser(u, dates, status, listener, geo, loc));
    }

    private void getTestNotification(User send, User rec, Event e, Consumer<Notification> listener){
        instances++;
        Set<Integer> invalidIDs = Notification.NewInstance(db.testDB, "Subject"+instances, "Body"+instances,
                e == null ? "" : e.getDocumentID(), rec.getDocumentID(), send == null ? "" : send.getDocumentID(), false, (instance, success) -> {
                    objects.add(instance);
                    if(!success){
                        fail("failed to create notification");
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private static final int N_FRESH_REC = 0b001, N_FRESH_SEND = 0b010, N_FRESH_EVENT = 0b100, N_FRESH_ALL = 0b111;
    private void getTestNotificationFresh(int which, User send, User rec, Event ev, Consumer<Notification> listener) {

        Function<Event, Function<User, Function<User, Runnable>>> create = e -> r -> s -> () -> {
            getTestNotification(s, r, e, listener);
        };

        Function<Event, Function<User, Runnable>> getSender = e -> r -> () -> {
            if((which & N_FRESH_SEND) != 0){
                getTestUser(s -> create.apply(e).apply(r).apply(s).run());
            }else{
                create.apply(e).apply(r).apply(send).run();
            }
        };

        Function<Event, Runnable> getReceiver = e -> () -> {
            if((which & N_FRESH_REC) != 0){
                getTestUser(r -> getSender.apply(e).apply(r).run());
            }else{
                getSender.apply(e).apply(rec).run();
            }
        };

        if((which & N_FRESH_EVENT) != 0){
            getTestEventFresh(EVENT_REG, e -> getReceiver.apply(e).run(), false);
        }else{
            getReceiver.apply(ev).run();
        }
    }

    private <T extends DatabaseInstance<T>> void getTestInstances(int count, BiConsumer<Integer, Consumer<T>> getNext, Consumer<List<T>> listener){
        Runnable l1 = new Runnable() {
            private final List<T> instances = new ArrayList<T>();
            private int i = -1;
            @Override
            public void run() {
                i++;
                if(i>=count){
                    listener.accept(instances);
                    return;
                }
                getNext.accept(i, u->{
                    instances.add(u);
                    run();
                });
            }
        };
        l1.run();
    }

    private void getEventWithUsersInWaitlistAndEnrolled(int waitListCount, int enrolledCount, Consumer<List<EventAssociation>> listener){
        getTestEventFresh(EVENT_REG, e->{
            this.<User>getTestInstances(waitListCount+enrolledCount, (i,b)->getTestUser(b), us -> {
                this.<EventAssociation>getTestInstances(waitListCount+enrolledCount, (i,b)->{
                    getTestEventAssociation(us.get(i), e, constants.getString(i<waitListCount? R.string.event_assoc_status_waitlist: R.string.event_assoc_status_enrolled), b, null);
                }, eas -> {
                    listener.accept(eas);
                });
            });
        }, false);
    }

    @Test
    public void US010101() throws InterruptedException {
        

        getTestUser(u -> getTestEventFresh(EVENT_REG, e -> e.addUserToWaitlist(u, new GeoPoint(0,0), (q, a, s) -> {
            if(a != null) objects.add(a.result);
            if(!asserts(() -> {
                assertTrue(s);
                assertNotNull(a);
                assertNotNull(a.result);;
                assertEquals(a.result.getUser(), u);
                assertEquals(a.result.getUserID(), u.getDocumentID());
                assertEquals(a.result.getEvent(), e);
                assertEquals(a.result.getEventID(), e.getDocumentID());
                assertEquals(a.result.getStatus(), constants.getString(R.string.event_assoc_status_waitlist));
            })) return;
            completeTest();
        }), false));
        await(10);
    }

    @Test
    public void US010102() throws InterruptedException {
        
        getTestUser(u-> getTestEventAssociationFreshUser(u, EVENT_REG, constants.getString(R.string.event_assoc_status_waitlist), ea -> ea.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s->{
            if(!asserts(()->assertTrue(s))) return;
            testDeletedInstance(ea, this::completeTest);
        }), false, null));
        await(10);
    }

    @Test
    public void US010201() throws InterruptedException {

        

        Set<Integer> invalidIDs = User.NewInstance(
                db.testDB, random+"uDevice1", "Name", "Des",
                null, "", "email@email.com", "1234567890",
                true, true, false, (instance, success) -> {
                    objects.add(instance);
                    if(!asserts(() -> {
                        assertTrue(success);
                        assertNotNull(instance);
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
                    })) return;
                    completeTest();
                });

        if(!asserts(()->assertTrue(invalidIDs.isEmpty())))return;

        await(10);
    }

    @Test
    public void US010202() throws InterruptedException {
        

        getTestUser(instance -> {
            Set<Integer> invalidIDs = instance.update("1", "2", "email4@email.com", "",
                    false, false, true, (success) -> {
                        if(!asserts(() -> {
                            assertTrue(success);
                            assertNotNull(instance);
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
                        })) return;
                        completeTest();
                    });
            asserts(() -> assertTrue(invalidIDs.isEmpty()));
        });
        await(10);
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
    public void US010303() throws InterruptedException {
        //Mainly visual
        getTestUser(u -> {
            RequestCreator r2 = Image.getFormatedAssociatedImage(u, Database.Collections.USERS, Image.Options.Circle(10));
            if(!asserts(() -> assertNotNull(r2))) return;
            completeTest();
        });
        await(10);
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
        
        getEventWithUsersInWaitlistAndEnrolled(3,0,eas -> {
            Event e = eas.get(0).getEvent();
            e.getLottery(-1, (q,l,s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(2,l.result.size());
                    assertTrue(l.filledAllSpots());
                    assertEquals(1,l.notChosen.size());
                })) return;
                l.execute((q1,d,s2) -> {
                    objects.addAll(d.result);
                    objects.addAll(d.failedNotifications);
                    if(!asserts(() -> assertTrue(s2))) return;
                    int enrolled = 0;
                    int waitlist = 0;
                    for(EventAssociation ea : eas){
                        if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_waitlist))){
                            waitlist++;
                        }else if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_invited))){
                            enrolled++;
                        }
                    }
                    int finalEnrolled = enrolled;
                    int finalWaitlist = waitlist;
                    if(!asserts(() -> {
                        assertEquals(2, finalEnrolled);
                        assertEquals(1, finalWaitlist);
                    })) return;
                    completeTest();

                }, false);
            });
        });

        await(10);
    }

    @Test
    public void US010502() throws InterruptedException {
        
        getTestUser(u-> getTestEventAssociationFreshUser(u, EVENT_AFTER_REG, constants.getString(R.string.event_assoc_status_invited), ea -> ea.getEvent().acceptInvite(u, (q, d, s)->{
            if(!asserts(() -> {
                assertTrue(s);
                assertEquals(constants.getString(R.string.event_assoc_status_enrolled), ea.getStatus());
            })) return;
            completeTest();
        }), false, null));
        await(10);
    }

    @Test
    public void US010503() throws InterruptedException {
        
        getTestUser(u-> getTestEventAssociationFreshUser(u, EVENT_AFTER_REG, constants.getString(R.string.event_assoc_status_invited), ea-> ea.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s->{
            if(!asserts(() -> assertTrue(s)))return;
            testDeletedInstance(ea, this::completeTest);
        }), false, null));
        await(10);
    }

    @Test
    public void US010601() throws InterruptedException {
        //Mainly visual
        
        getTestEventFresh(EVENT_REG, e -> db.testDB.getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
            if(!asserts(() -> {
                assertTrue(s);
                assertEquals(e,i);
            })) return;
            completeTest();
        }), false);
        await(10);
    }

    @Test
    public void US010602() throws InterruptedException {
        //Mainly visual
        
        getTestEventFresh(EVENT_REG, e -> {
            db.testDB.<Event>getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(e,i);
                })) return;
                getTestUser(u->{
                    i.addUserToWaitlist(u, new GeoPoint(0,0), (q,d,s2) -> {
                        if(d != null) objects.add(d.result);
                        if(!asserts(() -> {
                            assertTrue(s);
                            assertNotNull(d);
                            assertNotNull(d.result);
                            assertTrue(d.result.isLegalState());
                            assertEquals(u, d.result.getUser());
                            assertEquals(constants.getString(R.string.event_assoc_status_waitlist), d.result.getStatus());
                            assertEquals(e, d.result.getEvent());
                        })) return;
                        completeTest();
                    });
                });
            });
        }, false);
        await(10);
    }

    @Test
    public void US010701(){
        //TODO this is done by application which is not set up for testing
    }

    @Test
    public void US010801() throws InterruptedException {
        
        getTestUser(u->{
            getTestEventFresh(EVENT_REG, e->{
                e.addUserToWaitlist(u, null, (q,ea,s)->{
                    if(ea != null) objects.add(ea.result);
                    if(!asserts(() ->  assertFalse(s))) return;
                    completeTest();
                });
            }, true);
        });
        await(10);
    }

    @Test
    public void US020101() throws InterruptedException {
        
        getTestFacilityFresh(f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, 3L, 0.0, Timestamp.now(), Timestamp.now(), Timestamp.now(), Timestamp.now(), 0L,
                    (i,s)->{
                        objects.add(i);
                        if(!asserts(() -> {
                            assertTrue(s);
                            assertNotNull(i);
                            assertTrue(i.isLegalState());
                            assertEquals(i.getDocumentID(), i.getQrHash());
                            assertFalse(i.getQrHash().isBlank());
                        })) return;

                        completeTest();
                    });
        });
        await(10);
    }

    @Test
    public void US020102() throws InterruptedException {
        //Visual
    }


    @Test
    public void US020103() throws InterruptedException {

        
        getTestUser(u->{
            Facility.NewInstance(db.testDB, "Name"+random, new GeoPoint(0,0),
                    "Address", "Description", null, u.getDocumentID(), (i,s)->{
                        objects.add(i);
                        if(!asserts(() -> {
                            assertTrue(s);
                            assertNotNull(i);
                            assertEquals("Name"+random, i.getName());
                            assertEquals("Description", i.getDescription());
                            assertEquals(u, i.getOrganizer());
                            assertEquals("Address", i.getAddress());
                            assertNull(i.getImage());
                        })) return;

                        i.update("Name2", i.getLocation(), "Address2", "des2", s2->{
                            if(!asserts(() -> {
                                assertTrue(s2);
                                assertEquals("Name2", i.getName());
                                assertEquals("Address2", i.getAddress());
                                assertEquals(u, i.getOrganizer());
                                assertEquals("des2", i.getDescription());
                            })) return;

                            completeTest();
                        });
            });
        });
        await(10);
    }


    @Test
    public void US020201() throws InterruptedException {
        
        getEventWithUsersInWaitlistAndEnrolled(3, 0, eas->{
            Event e = eas.get(0).getEvent();
            e.getWaitlistUsers((query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertEquals(3, data.size());
                    for(EventAssociation ea : eas){
                        assertTrue(data.result.contains(ea));
                        assertEquals(constants.getString(R.string.event_assoc_status_waitlist), ea.getStatus());
                    }
                })) return;
                completeTest();
            });
        });
        await(10);
    }


    @Test
    public void US020202() throws InterruptedException {
        
        completeTest();
        //TODO
        await(10);
    }


    @Test
    public void US020203() throws InterruptedException {
        
        getTestFacilityFresh(f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), true,
                    "Des", 2L, 3L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        objects.add(i);
                        if(!asserts(() -> {
                            assertTrue(s);
                            assertTrue(i.getRequiresLocation());
                        })) return;

                        i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                            if(d != null) objects.add(d.result);
                            if(!asserts(() -> assertFalse(s2))) return;
                            completeTest();
                        });
                    });
        });
        await(10);
    }

    @Test
    public void US020301_limit() throws InterruptedException {
        
        getTestFacilityFresh(f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, 1L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        objects.add(i);
                        if(!asserts(() -> assertTrue(s))) return;
                        i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                            if(d != null) objects.add(d.result);
                            if(!asserts(() -> assertTrue(s2))) return;
                            getTestUser(u->{
                                i.addUserToWaitlist(u, null, (q2,d2,s3)->{
                                    if(!asserts(() -> assertFalse(s3))) return;
                                    completeTest();
                                });
                            });
                        });
                    });
        });
        await(10);
    }

    @Test
    public void US020301_optional() throws InterruptedException {
        
        getTestFacilityFresh(f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, -1L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        objects.add(i);
                        if(!asserts(() -> assertTrue(s))) return;
                        i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                            if(d != null) objects.add(d.result);
                            if(!asserts(() -> assertTrue(s2))) return;
                            completeTest();
                        });
                    });
        });
        await(10);
    }

    @Test
    public void US020401() throws InterruptedException {
        
        completeTest();
        //TODO need image file
        await(10);
    }

    @Test
    public void US020402() throws InterruptedException {
        completeTest();
        //TODO need image file
        await(10);
    }

    @Test
    public void US020501() throws InterruptedException {
        //TODO not applicable
    }

    @Test
    public void US020502() throws InterruptedException {
        getEventWithUsersInWaitlistAndEnrolled(3,0,eas -> {
            Event e = eas.get(0).getEvent();
            e.getLottery(-1, (q,l,s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(2,l.result.size());
                    assertTrue(l.filledAllSpots());
                    assertEquals(1,l.notChosen.size());
                })) return;

                l.execute((q1,d,s2) -> {
                    objects.addAll(d.result);
                    objects.addAll(d.failedNotifications);
                    if(!asserts(() -> assertTrue(s2))) return;
                    int enrolled = 0;
                    int waitlist = 0;
                    for(EventAssociation ea : eas){
                        if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_waitlist))){
                            waitlist++;
                        }else if(Objects.equals(ea.getStatus(), constants.getString(R.string.event_assoc_status_invited))){
                            enrolled++;
                        }
                    }
                    int finalEnrolled = enrolled;
                    int finalWaitlist = waitlist;
                    if(!asserts(() -> {
                        assertEquals(2, finalEnrolled);
                        assertEquals(1, finalWaitlist);
                    })) return;
                    completeTest();
                }, false);
            });
        });

        await(10);
    }

    @Test
    public void US020503() throws InterruptedException {
        getEventWithUsersInWaitlistAndEnrolled(3,0,eas -> {
            Event e = eas.get(0).getEvent();
            e.getLottery(-1, (q,l,s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(2,l.result.size());
                    assertTrue(l.filledAllSpots());
                    assertEquals(1,l.notChosen.size());
                })) return;

                l.execute((q1,d,s2) -> {
                    objects.addAll(d.result);
                    objects.addAll(d.failedNotifications);
                    if(!asserts(() -> assertTrue(s2))) return;
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
                    int finalEnrolled = enrolled;
                    int finalWaitlist = waitlist;
                    if(!asserts(() -> {
                        assertEquals(2, finalEnrolled);
                        assertEquals(1, finalWaitlist);
                    })) return;


                    eae.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s4->{});

                    e.getLottery(-1, (q2,l2,s22)->{
                        if(!asserts(() -> {
                            assertTrue(s22);
                            assertEquals(1,l2.result.size());
                            assertTrue(l2.filledAllSpots());
                            assertEquals(0,l2.notChosen.size());
                        })) return;

                        l2.execute((q3,d3,s4) -> {
                            objects.addAll(d3.result);
                            objects.addAll(d3.failedNotifications);
                            if(!asserts(() -> assertTrue(s4))) return;
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
                            int finalEnrolled1 = enrolled2;
                            int finalWaitlist1 = waitlist2;
                            if(!asserts(() -> {
                                assertEquals(2, finalEnrolled1);
                                assertEquals(0, finalWaitlist1);
                            }));
                            completeTest();

                        }, false);
                    });

                }, false);
            });
        });

        await(10);
    }

    public void testDeletedInstance(DatabaseInstance<?> instance, Runnable onComplete){
        if(instance == null){
            onComplete.run();
            return;
        }
        if(!asserts(() -> assertFalse(instance.isLegalState()))) return;
        instance.getDocumentReference().get().addOnCompleteListener(t -> {
            if(!asserts(()->{
                assertTrue(t.isSuccessful());
                assertFalse(t.getResult().exists());
            })) return;
            onComplete.run();
        });
    }

    public void testDeletedEventAssociation(EventAssociation ea, Runnable onComplete){
        if(ea == null){
            onComplete.run();
            return;
        }
        if(!asserts(() -> {
            assertTrue(ea.getUser().isLegalState());
            assertTrue(ea.getEvent().isLegalState());
        })) return;
        testDeletedInstance(ea, onComplete);
    }

    public void  testDeletedEventAssociationCascade(EventAssociation ea, Runnable onComplete){
        if(ea == null){
            onComplete.run();
            return;
        }
        testDeletedInstance(ea, onComplete);
    }

    public void testDeletedNotification(Notification n, Runnable onComplete){
        if(n == null){
            onComplete.run();
            return;
        }
        if(!asserts(() -> {
            assertTrue(n.getSender().isLegalState());
            assertTrue(n.getReceiver().isLegalState());
            assertTrue(n.getEvent() == null || n.getEvent().isLegalState());
        })) return;
        testDeletedInstance(n, onComplete);
    }

    public void testDeletedImage(Image i, Runnable onComplete){
        if(i == null){
            onComplete.run();
            return;
        }
        i.getLocType().getDocument(db.testDB, i.getLocType().instanceIDFromDatabaseID(i.getLocID())).get().addOnCompleteListener(t -> {
            if(!asserts(()->{
                assertTrue(t.isSuccessful());
                assertFalse(t.getResult().exists());
            })) return;
            testDeletedImageCascade(i, onComplete);
        });
    }

    public void testDeletedImageCascade(Image i, Runnable onComplete){
        if(i == null){
            onComplete.run();
            return;
        }
        //todo storage
        testDeletedInstance(i, onComplete);
    }

    public void testDeletedEvent(Event e, EventAssociation ea, Notification n, Runnable onComplete){
        if(e == null){
            onComplete.run();
            return;
        }
        if(!asserts(()->{
            assertTrue(e.getFacility().isLegalState());
            assertTrue(n == null || n.getEvent() == null);
        })) return;
        testDeletedImageCascade(e.getAssociatedImage(), () -> {
            testDeletedEventAssociationCascade(ea, () -> testDeletedInstance(e, onComplete));
        });
    }

    public void testDeletedEventCascade(Event e, EventAssociation ea, Notification n, Runnable onComplete){
        if(e == null){
            onComplete.run();
            return;
        }
        if(!asserts(()->{
            assertTrue(n == null || (n.getEvent() == null && n.isLegalState()));
        })) return;
        testDeletedImageCascade(e.getAssociatedImage(), () -> {
            testDeletedEventAssociationCascade(ea, () -> testDeletedInstance(e, onComplete));
        });
    }

    public void testDeletedFacilityShallow(Facility f, Event e, Runnable onComplete){
        testDeletedFacilityDeep(f, e, null, null, onComplete);
    }

    public void testDeletedFacilityDeep(Facility f, Event e, EventAssociation ea, Notification n, Runnable onComplete){
        if(f == null){
            onComplete.run();
            return;
        }
        if(!asserts(()->{
            assertTrue(e == null || e.getFacility() == f);
        }))return;
        testDeletedImageCascade(f.getAssociatedImage(), () -> {
            testDeletedEventCascade(e, ea, n, () -> testDeletedInstance(f, onComplete));
        });
    }

    public void testDeletedUserDeep(
            User u, Event e_ofFacility,
            EventAssociation ea_associated, EventAssociation ea_ofEvent,
            Notification n_receiver, Notification n_sender, Notification n_event,
            Runnable onComplete
    ){
        if(u == null){
            onComplete.run();
            return;
        }
        Facility f = u.getFacility();
        if(!asserts(() -> {
            assertTrue(ea_associated == null || ea_associated.getUser() == u);
            assertTrue(f == null || e_ofFacility == null || e_ofFacility.getFacility() == f);
            assertTrue(f == null || e_ofFacility == null || ea_ofEvent == null || ea_ofEvent.getEvent() == e_ofFacility);
            assertTrue(n_receiver == null || n_receiver.getReceiver() == u);
            assertTrue(n_sender == null || (n_sender.getSender() == null && n_sender.isLegalState())); //assumes correct, asserts set to null
        }))return;

        testDeletedImageCascade(u.getAssociatedImage(),
                () -> testDeletedFacilityDeep(f, e_ofFacility, ea_ofEvent, n_event,
                        () -> testDeletedEventAssociationCascade(ea_associated,
                                () -> testDeletedInstance(u, onComplete)
                        )
                )
        );
    }

    public void testDeletedUserShallow(User u, Runnable onComplete){
        testDeletedUserDeep(u, null, null, null, null, null, null, onComplete);
    }

    @Test
    public void US030201_shallow() throws InterruptedException {
        getTestUser(u -> {
            u.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                assertTrue(s);
                testDeletedUserShallow(u, this::completeTest);
            });
        });
        await(30);
    }

    @Test
    public void US030201_deep_noNotifications() throws InterruptedException {
        getTestEventAssociationFresh(EVENT_REG, "Waitlist", ea -> {
            Event e = ea.getEvent();
            User u = e.getFacility().getOrganizer();
            getTestEventAssociationFreshUser(u, EVENT_REG, "Waitlist", ea_assoc -> {
                u.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                    if(!asserts(() -> assertTrue(s))) return;
                    testDeletedUserDeep(u, e, ea_assoc, ea, null, null, null, this::completeTest);
                });
            }, false, null);
        }, false, null);
        await(30);
    }

    @Test
    public void US030201_deep_noImages() throws InterruptedException{
        getTestEventAssociationFresh(EVENT_REG, "Waitlist", ea -> {
            Event e = ea.getEvent();
            User u = e.getFacility().getOrganizer();
            getTestEventAssociationFreshUser(u, EVENT_REG, "Waitlist", ea_assoc -> {
                getTestNotificationFresh(N_FRESH_REC | N_FRESH_SEND, null, null, e, n_event -> {
                    getTestNotificationFresh(N_FRESH_REC | N_FRESH_EVENT, u, null, null, n_send -> {
                        getTestNotificationFresh(N_FRESH_SEND | N_FRESH_EVENT, null, u, null, n_rec -> {
                            u.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                                if(asserts(() -> assertTrue(s))){
                                    try {
                                        TimeUnit.SECONDS.sleep(2); //wait for updates
                                    } catch (InterruptedException ignored) {}
                                    testDeletedUserDeep(u, e, ea_assoc, ea, n_rec, n_send, n_event, this::completeTest);
                                };
                            });
                        });
                    });
                });
            }, false, null);
        }, false, null);
        await(30);
    }
}
