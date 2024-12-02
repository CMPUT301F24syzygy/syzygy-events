package com.syzygy.events.ModelTests;

import static org.junit.Assert.*;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.RequestCreator;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.DatabaseQuery;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tests the user stories
 *
 * @author Gareth
 */
public class UserStoriesModelTest {

    private static final TestDatabase db = new TestDatabase();
    private static Resources constants;
    private static String random;
    private static Uri img;
    private static final Set<UserStoriesModelTest> tests = new HashSet<>();

    private static String runningTest = null;

    @Rule
    public TestName name = new TestName();

    private static int instances = 0;

    @BeforeClass
    public static void setUp() throws Throwable{

        SyzygyApplication.NO_DATABASE = true;
        Context context = ApplicationProvider.getApplicationContext();
        constants = context.getResources();
        db.createDb(context);
        db.testDB.addErrorListener(UserStoriesModelTest::accept);
        Log.d("Testing", "BeforeClass");
        int resId = R.drawable.penguin_blue;
        img = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + constants.getResourcePackageName(resId) + '/' + constants.getResourceTypeName(resId) + '/' + constants.getResourceEntryName(resId) );
    }

    public static void accept(RuntimeException ex){
        tests.iterator().forEachRemaining(t -> t.acceptError(ex));
    }

    private static final Set<DatabaseInstance<?>> allObjectsUsed = new HashSet<>();

    private Throwable error = null;
    private CountDownLatch latch = null;
    private boolean ignoreDelete = false;

    @After
    public void cleanUp() throws Throwable {
        Log.d("Testing", "After");
        if(!ignoreDelete) {
            allObjectsUsed.addAll(db.testDB.getTrackedInstances());
        }
        db.testDB.getTrackedInstances().clear();
        runningTest = null;

    }

    @Before
    public void beforeTest(){
        Log.i("Testing", "Before");
        random = Integer.toHexString(Instant.now().hashCode());
        error = null;
        latch = new CountDownLatch(1);
        ignoreDelete = false;
        if(runningTest != null) throw new IllegalStateException("Already running test :" + runningTest);
        runningTest = name.getMethodName();
        tests.add(this);

    }
    @AfterClass
    public static void clean() throws Throwable {
        Log.d("Testing", "AfterClass");
        for(DatabaseInstance<?> i : allObjectsUsed){
            if(i==null || Objects.equals(i.getDocumentID(), SyzygyApplication.SYSTEM_ACCOUNT_ID)) continue;
            CountDownLatch l = new CountDownLatch(1);
            i.getDocumentReference().delete().addOnCompleteListener(t -> {
                if(i.getCollection() == Database.Collections.IMAGES){
                    db.testDB.deleteFile(((Image)i).getImageID(), (success) -> l.countDown());
                }else{
                    l.countDown();
                }
            });
            l.await();
        }
        allObjectsUsed.clear();
    }

    private void acceptError(Throwable ex){
        error = ex;
        completeTest(true);
    }

    private void completeTest(boolean fullComplete){
        if(fullComplete){
            for(int i = 0; i< latch.getCount(); i++){
                completeTest();
            }
        }else{
            completeTest();
        }
    }

    private void completeTest(){
        latch.countDown();
    }

    private boolean asserts(Runnable statements){
        try{
            statements.run();
            return true;
        }catch (Throwable ex){
            acceptError(ex);
            return false;
        }
    }

    private void awaitAndContinue(long timeout) throws Throwable {
        latch.await(timeout, TimeUnit.SECONDS);
        if(error != null){
            throw error;
        }
    }

    private void await() throws Throwable {
        if(!latch.await(5, TimeUnit.MINUTES)){
            fail("Timeout");
        };
        if(error != null){
            throw error;
        }
    }

    private void getTestUser(boolean freshImage, Consumer<User> listener){
        instances++;
        Set<Integer> invalidIDs = User.NewInstance(
                db.testDB, random+"u"+instances, name.getMethodName()+'|'+instances, "Des"+instances,
                freshImage ? img : null, "", "email"+instances+"@email.com", "12345678901",
                true, true, false, (instance, success) -> {
                    if(!success){
                        asserts(() -> fail("failed to create user"));
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestFacility(User u, boolean freshImage, Consumer<Facility> listener){
        instances++;
        Set<Integer> invalidIDs = Facility.NewInstance(db.testDB, name.getMethodName()+'|'+instances, new GeoPoint(0,0),
                "Address"+instances, "Des"+instances, freshImage ? img : null, u.getDocumentID(), (instance, success) -> {
                    if(!success){
                        asserts(() -> fail("failed to create facility"));
                    }else{
                        u.setFacility(instance);
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestFacilityFresh(boolean freshFacImage, boolean freshUsrImage, Consumer<Facility> listener){
        getTestUser(freshUsrImage, u -> getTestFacility(u, freshFacImage, listener));
    }

    private Timestamp after(){
        return new Timestamp(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    private Timestamp before(){
        return new Timestamp(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private static final int EVENT_BEFORE_REG = 0, EVENT_REG = 1, EVENT_AFTER_REG = 2, EVENT_BEFORE_START = 2, EVENT_START = 3, EVENT_END = 4;
    private void getTestEvent(Facility f, int dates, boolean geo, boolean freshImage, Consumer<Event> listener){
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

        Set<Integer> invalidIDs = Event.NewInstance(db.testDB, name.getMethodName()+'|'+instances, freshImage ? img : null, f.getDocumentID(),
                geo, "Des"+instances, 2L, 3L, 0.00,
                open, close, start, end, Event.Dates.EVERY_DAY, (instance, success) -> {
                    if(!success){
                        asserts(() -> fail("failed to create event"));
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestEventFresh(int dates, boolean geo, boolean freshImage, boolean freshFacImage, boolean freshUsrImage, Consumer<Event> listener){
        getTestFacilityFresh(freshFacImage, freshUsrImage, f -> getTestEvent(f, dates, geo, freshImage, listener));
    }

    private void getTestEventAssociation(User u, Event e, String status, Consumer<EventAssociation> listener, GeoPoint geo){
        instances++;
        Set<Integer> invalidIDs = EventAssociation.NewInstance(db.testDB, e.getDocumentID(), geo,
                status, u.getDocumentID(), (instance, success) -> {
                    if(!success){
                        asserts(() -> fail("failed to create event association"));
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestEventAssociationFreshUser(User u, int dates, String status, boolean geo, GeoPoint loc, boolean freshImage, boolean freshFacImage, boolean freshFUsrImage, Consumer<EventAssociation> listener){
        getTestEventFresh(dates, geo, freshImage, freshFacImage, freshFUsrImage, e -> getTestEventAssociation(u, e, status, listener, loc));
    }

    private void getTestEventAssociationFresh(int dates, String status, boolean geo, GeoPoint loc, boolean freshImage, boolean freshAUsrImage, boolean freshFUsrImage, boolean freshFacImage, Consumer<EventAssociation> listener){
        getTestUser(freshAUsrImage, u-> getTestEventAssociationFreshUser(u, dates, status, geo, loc, freshImage, freshFacImage, freshFUsrImage, listener));
    }

    private void getTestNotification(User send, User rec, Event e, Consumer<Notification> listener){
        instances++;
        Set<Integer> invalidIDs = Notification.NewInstance(db.testDB, "Subject"+instances, "Body"+instances,
                e == null ? "" : e.getDocumentID(), rec.getDocumentID(), send == null ? "" : send.getDocumentID(), false, (instance, success) -> {
                    if(!success){
                        asserts(() -> fail("failed to create notification"));
                    }else{
                        listener.accept(instance);
                    }
                });
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private void getTestImage(DatabaseInstance<?> forInst, Consumer<Image> listener) {
        instances++;
        Set<Integer> invalidIDs = Image.NewInstance(db.testDB,
                forInst.getAssociatedImageLocName(), forInst.getCollection(), forInst.getDocumentID(),
                img, (i, s) -> {
                    if (!s) {
                        asserts(() -> fail("failed to create image"));
                    } else {
                        listener.accept(i);
                    }
                }
        );
        asserts(() -> assertTrue(invalidIDs.isEmpty()));
    }

    private static final int N_FRESH_REC = 0b001, N_FRESH_SEND = 0b010, N_FRESH_EVENT = 0b100, N_FRESH_ALL = 0b111;
    private void getTestNotificationFresh(int which, User send, User rec, Event ev, Consumer<Notification> listener) {

        Function<Event, Function<User, Function<User, Runnable>>> create = e -> r -> s -> () -> {
            getTestNotification(s, r, e, listener);
        };

        Function<Event, Function<User, Runnable>> getSender = e -> r -> () -> {
            if((which & N_FRESH_SEND) != 0){
                getTestUser(false, s -> create.apply(e).apply(r).apply(s).run());
            }else{
                create.apply(e).apply(r).apply(send).run();
            }
        };

        Function<Event, Runnable> getReceiver = e -> () -> {
            if((which & N_FRESH_REC) != 0){
                getTestUser(false, r -> getSender.apply(e).apply(r).run());
            }else{
                getSender.apply(e).apply(rec).run();
            }
        };

        if((which & N_FRESH_EVENT) != 0){
            getTestEventFresh(EVENT_REG, false, false, false, false, e -> getReceiver.apply(e).run());
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

    private void getTestEventWithEventAssociations(int waitListCount, int enrolledCount, int invitedCount, int canceledCount, Consumer<List<EventAssociation>> listener){
        int all = waitListCount+enrolledCount+invitedCount+canceledCount;
        getTestEventFresh(EVENT_REG, false, false, false, false, e->{
            this.<User>getTestInstances(all, (i,b)->getTestUser(false, b), us -> {
                getTestInstances(all, (i, b)->{
                    getTestEventAssociation(us.get(i), e, constants.getString(
                            i < waitListCount?
                                    R.string.event_assoc_status_waitlist :
                                    i < waitListCount+enrolledCount?
                                            R.string.event_assoc_status_enrolled:
                                            i < waitListCount+enrolledCount+invitedCount?
                                                R.string.event_assoc_status_invited:
                                                R.string.event_assoc_status_cancelled
                        ), b, null
                    );
                }, listener);
            });
        });
    }

    @Test
    public void US010101() throws Throwable {
        

        getTestUser(false, u -> getTestEventFresh(EVENT_REG, false, false, false, false, e -> e.addUserToWaitlist(u, new GeoPoint(0,0), (q, a, s) -> {
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
        })));
        await();
    }

    @Test
    public void US010102() throws Throwable {
        
        getTestUser(false, u-> getTestEventAssociationFreshUser(u, EVENT_REG, constants.getString(R.string.event_assoc_status_waitlist), false, null, false, false, false, ea -> ea.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s->{
            if(!asserts(()->assertTrue(s))) return;
            testDeletedInstance(ea, this::completeTest);
        })));
        await();
    }

    @Test
    public void US010201() throws Throwable {

        

        Set<Integer> invalidIDs = User.NewInstance(
                db.testDB, random+"uDevice1", "Name", "Des",
                null, "", "email@email.com", "1234567890",
                true, true, false, (instance, success) -> {
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

        await();
    }

    @Test
    public void US010202() throws Throwable {
        

        getTestUser(false, instance -> {
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
        await();
    }

    @Test
    public void US010301() throws Throwable {
        getTestUser(false, u -> {
            u.setProfileImage(img, success -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertNotNull(u.getProfileImageID());
                    // cant really test if images are equal
                })) {
                    return;
                };
                completeTest();
            });
        });
        await();
    }

    @Test
    public void US010302() throws Throwable {
        getTestUser(true, u -> {
            Image i = u.getProfileImage();
            u.setProfileImage(null, success -> {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (Throwable ignore) {}

                if(!asserts(() -> {
                    assertTrue(success);
                    assertNull(u.getProfileImage());
                    assertEquals(u.getProfileImageID(), "");
                })) return;
                testDeletedImageCascade(i, this::completeTest);
            });
        });
        await();
    }

    @Test
    public void US010303() throws Throwable {
        //Mainly visual
        getTestUser(false, u -> {
            RequestCreator r2 = Image.getFormatedAssociatedImage(u, Database.Collections.USERS, Image.Options.Circle(10));
            if(!asserts(() -> assertNotNull(r2))) return;
            completeTest();
        });
        await();
    }

    @Test
    public void US010401_own_pos() throws Throwable {
        latch = new CountDownLatch(2);
        getTestEventFresh(EVENT_AFTER_REG, false, false, false, false, e -> {
            User u = e.getFacility().getOrganizer();
            u.new NotificationListener(n -> {
                if(asserts(() -> {
                    assertTrue(n.isLegalState());
                })) completeTest();
            });
            getTestEventAssociation(u, e, constants.getString(R.string.event_assoc_status_waitlist), ea -> {
                e.getLottery(1, (query, data, success) -> {
                    if(!asserts(() -> {
                        assertTrue(success);
                        assertTrue(data.filledAllSpots());
                        assertEquals(0, data.notChosen.size());
                        assertEquals(data.result.result.get(0), ea);
                    })) return;
                    data.execute((query1, data1, success1) -> {
                        if(asserts(() -> {
                            assertTrue(success1);
                            assertTrue(data1.failedNotifications.isEmpty());
                            assertEquals(1, data1.result.size());
                            Notification n = data1.result.get(0);
                            assertEquals(n.getEvent(), e);
                            assertEquals(n.getSender(), u);
                            assertEquals(n.getReceiver(), u);
                        })) completeTest();
                    }, false);
                });
            }, null);
        });
        await();
    }

    @Test
    public void US010401_US010402_US020501() throws Throwable {
        latch = new CountDownLatch(4);
        AtomicInteger chosen = new AtomicInteger(0);
        AtomicInteger lost = new AtomicInteger(0);
        getTestEventWithEventAssociations(3,0, 0, 0, eas -> {
            Event e = eas.get(0).getEvent();
            eas.forEach(ea -> {
                ea.getUser().new NotificationListener(n -> {
                    Log.d("Testing", n.getDocumentID()+" : "+ea.getUserID());
                    if(!asserts(() -> {
                        assertTrue(n.isLegalState());
                        assertEquals(n.getReceiver(), ea.getUser());
                        assertEquals(n.getEvent(), e);
                    })) return;
                    if(n.getSubject().equals(constants.getString(R.string.notification_lottery_chosen_subject))){
                        chosen.incrementAndGet();
                    }else if(n.getSubject().equals(constants.getString(R.string.notification_lottery_notChosen_subject))){
                        lost.incrementAndGet();
                    }
                    completeTest();
                });
            });
            e.getLottery(0, (query, data, success) -> {
                Log.d("Testing", "got lottery");
                if(!asserts(() -> {
                    assertTrue(success);
                    assertTrue(data.filledAllSpots());
                    assertEquals(2, data.result.size());
                    assertEquals(1, data.notChosen.size());
                })) return;
                Log.d("Testing", "executing");
                data.execute((query1, data1, success1) -> {
                    Log.d("Testing", "executed");
                    if(!asserts(() -> {
                        assertTrue(data1.failedNotifications.isEmpty());
                        assertEquals(3, data1.result.size());
                        assertTrue(e.hasRunLottery());
                    })) return;
                    completeTest();
                }, true);
            });
        });
        await();
        assertEquals(2, chosen.get());
        assertEquals(1, lost.get());
    }


    @Test
    public void US010403() throws Throwable {
        AtomicBoolean notified = new AtomicBoolean(false);
        getTestUser(false, u -> {
            u.setOrganizerNotifications(false);
            u.new NotificationListener(n -> {
                notified.set(true);
                completeTest();
            });
            getTestNotification(null, u, null, n -> {});
        });
        awaitAndContinue(10);
        assertFalse(notified.get());
    }

    @Test
    public void US010501() throws Throwable {
        
        getTestEventWithEventAssociations(3,0, 0, 0, eas -> {
            Event e = eas.get(0).getEvent();
            e.getLottery(-1, (q,l,s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(2,l.result.size());
                    assertTrue(l.filledAllSpots());
                    assertEquals(1,l.notChosen.size());
                })) return;
                l.execute((q1,d,s2) -> {
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

        await();
    }

    @Test
    public void US010502() throws Throwable {
        
        getTestUser(false, u-> getTestEventAssociationFreshUser(u, EVENT_AFTER_REG, constants.getString(R.string.event_assoc_status_invited), false, null, false, false, false, ea -> ea.getEvent().acceptInvite(u, (q, d, s)->{
            if(!asserts(() -> {
                assertTrue(s);
                assertEquals(constants.getString(R.string.event_assoc_status_enrolled), ea.getStatus());
            })) return;
            completeTest();
        })));
        await();
    }

    @Test
    public void US010503() throws Throwable {
        
        getTestUser(false, u-> getTestEventAssociationFreshUser(u, EVENT_AFTER_REG, constants.getString(R.string.event_assoc_status_invited), false, null, false, false, false, ea-> ea.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s->{
            if(!asserts(() -> assertTrue(s)))return;
            testDeletedInstance(ea, this::completeTest);
        })));
        await();
    }

    @Test
    public void US010601() throws Throwable {
        //Mainly visual
        
        getTestEventFresh(EVENT_REG, false, false, false, false, e -> db.testDB.getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
            if(!asserts(() -> {
                assertTrue(s);
                assertEquals(e,i);
            })) return;
            completeTest();
        }));
        await();
    }

    @Test
    public void US010602() throws Throwable {
        //Mainly visual
        
        getTestEventFresh(EVENT_REG, false, false, false, false, e -> {
            db.testDB.<Event>getInstance(Database.Collections.EVENTS, e.getQrHash(), (i, s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(e,i);
                })) return;
                getTestUser(false, u->{
                    i.addUserToWaitlist(u, new GeoPoint(0,0), (q,d,s2) -> {
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
        });
        await();
    }

    @Test
    public void US010701(){
        //This is done by application which is not set up for testing
    }

    @Test
    public void US010801() throws Throwable {
        
        getTestUser(false, u->{
            getTestEventFresh(EVENT_REG, true, false, false, false, e->{
                e.addUserToWaitlist(u, null, (q,ea,s)->{
                    if(!asserts(() ->  assertFalse(s))) return;
                    completeTest();
                });
            });
        });
        await();
    }

    @Test
    public void US020101() throws Throwable {
        
        getTestFacilityFresh(false, false, f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, 3L, 0.0, Timestamp.now(), Timestamp.now(), Timestamp.now(), Timestamp.now(), 0L,
                    (i,s)->{
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
        await();
    }

    @Test
    public void US020102() throws Throwable {
        //Implemented by application not database
        getTestEventFresh(EVENT_REG, false, false, false, false, e -> {
            if(asserts(() -> assertEquals(e.getDocumentID(), e.getQrHash()))){
                completeTest();
            };
        });
        await();
    }


    @Test
    public void US020103() throws Throwable {
        getTestUser(false, u->{
            Facility.NewInstance(db.testDB, "Name"+random, new GeoPoint(0,0),
                    "Address", "Description", null, u.getDocumentID(), (i,s)->{
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
        await();
    }


    @Test
    public void US020201() throws Throwable {
        
        getTestEventWithEventAssociations(3, 0, 0, 0, eas->{
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
        await();
    }


    @Test
    public void US020202() throws Throwable {
        GeoPoint p = new GeoPoint(1,1);
        getTestEventAssociationFresh(EVENT_REG, constants.getString(R.string.event_assoc_status_waitlist), true, p, false, false,false, false, ea->{
            User u = ea.getUser().fetch();
            Event e = ea.getEvent().fetch();
            e.getUserAssociation(u,(query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertNotNull(data);
                    assertEquals(data.result.size(), 1);
                    EventAssociation ea1 = data.result.get(0);
                    assertEquals(ea1.getUser(), u);
                    assertEquals(ea1.getEvent(), e);
                    assertEquals(ea1.getLocation(), p);
                })) return;
                completeTest();
            });
        });
        await();
    }


    @Test
    public void US020203() throws Throwable {
        getTestFacilityFresh(false, false, f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), true,
                    "Des", 2L, 3L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        if(!asserts(() -> {
                            assertTrue(s);
                            assertTrue(i.getRequiresLocation());
                        })) return;

                        i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                            if(!asserts(() -> assertFalse(s2))) return;
                            completeTest();
                        });
                    });
        });
        await();
    }

    @Test
    public void US020301_limit() throws Throwable {
        
        getTestFacilityFresh(false, false, f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, 1L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        if(!asserts(() -> assertTrue(s))) return;
                        i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                            if(!asserts(() -> assertTrue(s2))) return;
                            getTestUser(false, u->{
                                i.addUserToWaitlist(u, null, (q2,d2,s3)->{
                                    if(!asserts(() -> assertFalse(s3))) return;
                                    completeTest();
                                });
                            });
                        });
                    });
        });
        await();
    }

    @Test
    public void US020301_optional() throws Throwable {
        
        getTestFacilityFresh(false, false, f->{
            Event.NewInstance(db.testDB, random+"Title", null, f.getDocumentID(), false,
                    "Des", 2L, -1L, 0.0, before(), after(), after(), after(), 0L,
                    (i,s)->{
                        if(!asserts(() -> assertTrue(s))) return;
                        i.addUserToWaitlist(f.getOrganizer(), null, (q,d, s2)->{
                            if(!asserts(() -> assertTrue(s2))) return;
                            completeTest();
                        });
                    });
        });
        await();
    }

    @Test
    public void US020401() throws Throwable {
        getTestEventFresh(EVENT_REG, false, false, false, false, e -> {
            e.setPoster(img, success -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertNotNull(e.getPoster());
                    // cant really test if images are equal
                })) {
                    return;
                };
                completeTest();
            });
        });
        await();
    }

    @Test
    public void US020402() throws Throwable {
        getTestEventFresh(EVENT_REG, false, true, false, false, e -> {
            Image i = e.getPoster();
            if(!asserts(() -> {
                assertNotNull(i);
            })) return;
            e.setPoster(img, success -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertNotNull(e.getPoster());
                    assertNotEquals(i, e.getPoster());
                    // cant really test if images are equal
                })) return;
                completeTest();
            });
        });
        await();
    }

    @Test
    public void US020502() throws Throwable {
        getTestEventWithEventAssociations(3,0, 0, 0, eas -> {
            Event e = eas.get(0).getEvent();
            e.getLottery(-1, (q,l,s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(2,l.result.size());
                    assertTrue(l.filledAllSpots());
                    assertEquals(1,l.notChosen.size());
                })) return;

                l.execute((q1,d,s2) -> {
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

        await();
    }

    @Test
    public void US020503() throws Throwable {
        getTestEventWithEventAssociations(3,0, 0, 0, eas -> {
            Event e = eas.get(0).getEvent();
            e.getLottery(-1, (q,l,s) -> {
                if(!asserts(() -> {
                    assertTrue(s);
                    assertEquals(2,l.result.size());
                    assertTrue(l.filledAllSpots());
                    assertEquals(1,l.notChosen.size());
                })) return;

                l.execute((q1,d,s2) -> {
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

        await();
    }

    @Test
    public void US020601() throws Throwable {
        getTestEventWithEventAssociations(0,0,2, 0, eas -> {
            Event e = eas.get(0).getEvent();
            e.getUsersByStatus(R.string.event_assoc_status_invited, (query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertEquals(2, data.size());
                    EventAssociation a = data.result.get(0);
                    EventAssociation b = data.result.get(1);
                    assertTrue(a == eas.get(0) || b == eas.get(0));
                    assertTrue(a == eas.get(1) || b == eas.get(1));
                })) return;
                completeTest();
            });
        });
        await();
    }

    @Test
    public void US020602() throws Throwable {
        getTestEventWithEventAssociations(0,0,0, 2, eas -> {
            Event e = eas.get(0).getEvent();
            e.getUsersByStatus(R.string.event_assoc_status_cancelled, (query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertEquals(2, data.size());
                    EventAssociation a = data.result.get(0);
                    EventAssociation b = data.result.get(1);
                    assertTrue(a == eas.get(0) || b == eas.get(0));
                    assertTrue(a == eas.get(1) || b == eas.get(1));
                })) return;
                completeTest();
            });
        });
        await();
    }

    @Test
    public void US020603() throws Throwable {
        getTestEventWithEventAssociations(0,2,0, 0, eas -> {
            Event e = eas.get(0).getEvent();
            e.getUsersByStatus(R.string.event_assoc_status_enrolled, (query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                    assertEquals(2, data.size());
                    EventAssociation a = data.result.get(0);
                    EventAssociation b = data.result.get(1);
                    assertTrue(a == eas.get(0) || b == eas.get(0));
                    assertTrue(a == eas.get(1) || b == eas.get(1));
                })) return;
                completeTest();
            });
        });
        await();
    }

    @Test
    public void US020604() throws Throwable {
        getTestEventAssociationFresh(EVENT_REG, constants.getString(R.string.event_assoc_status_invited), false, null, false, false, false, false, ea -> {
            Event e = ea.getEvent();
            User u = ea.getUser();
            ea.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                if(!asserts(() -> {
                    assertTrue(s);
                })) return;
                testDeletedEventAssociation(ea, () -> {
                    e.getUserAssociation(u, (query, data, success) -> {
                        if(!asserts(()->{
                            assertTrue(success);
                            assertTrue(data.result.isEmpty());
                        })) return;
                        completeTest();
                    });
                });
            });
        });
        await();
    }

    @Test
    public void US020701() throws Throwable {
        getTestEventWithEventAssociations(2,0,0, 0, eas -> {
            Event e = eas.get(0).getEvent();
            User u = e.getFacility().getOrganizer();
            e.getUsersByStatus(R.string.event_assoc_status_waitlist, (query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                })) return;
                data.notify("CustomSubject", "CustomBody", true, true, false, (query1, data1, success1) -> {
                    if(!asserts(() -> {
                        assertTrue(success1);
                        assertEquals(2, data1.result.size());
                        assertTrue(data1.failedNotifications.isEmpty());
                        for(Notification n : data1.result){
                            assertEquals(e, n.getEvent());
                            assertEquals(u, n.getSender());
                            assertEquals("CustomSubject", n.getSubject());
                            assertEquals("CustomBody", n.getBody());
                            assertTrue(n.getReceiver() == eas.get(0).getUser() || n.getReceiver() == eas.get(1).getUser());
                        }
                        assertNotEquals(data1.result.get(0).getReceiver(), data1.result.get(1).getReceiver());
                    })) return;
                    completeTest();
                });
            });
        });
        await();
    }

    @Test
    public void US020702() throws Throwable {
        getTestEventWithEventAssociations(0,2,0, 0, eas -> {
            Event e = eas.get(0).getEvent();
            User u = e.getFacility().getOrganizer();
            e.getUsersByStatus(R.string.event_assoc_status_enrolled, (query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                })) return;
                data.notify("CustomSubject", "CustomBody", true, true, false, (query1, data1, success1) -> {
                    if(!asserts(() -> {
                        assertTrue(success1);
                        assertEquals(2, data1.result.size());
                        assertTrue(data1.failedNotifications.isEmpty());
                        for(Notification n : data1.result){
                            assertEquals(e, n.getEvent());
                            assertEquals(u, n.getSender());
                            assertEquals("CustomSubject", n.getSubject());
                            assertEquals("CustomBody", n.getBody());
                            assertTrue(n.getReceiver() == eas.get(0).getUser() || n.getReceiver() == eas.get(1).getUser());
                        }
                        assertNotEquals(data1.result.get(0).getReceiver(), data1.result.get(1).getReceiver());
                    })) return;
                    completeTest();
                });
            });
        });
        await();
    }

    @Test
    public void US020703() throws Throwable {
        getTestEventWithEventAssociations(0,0,0, 2, eas -> {
            Event e = eas.get(0).getEvent();
            User u = e.getFacility().getOrganizer();
            e.getUsersByStatus(R.string.event_assoc_status_cancelled, (query, data, success) -> {
                if(!asserts(() -> {
                    assertTrue(success);
                })) return;
                data.notify("CustomSubject", "CustomBody", true, true, false, (query1, data1, success1) -> {
                    if(!asserts(() -> {
                        assertTrue(success1);
                        assertEquals(2, data1.result.size());
                        assertTrue(data1.failedNotifications.isEmpty());
                        for(Notification n : data1.result){
                            assertEquals(e, n.getEvent());
                            assertEquals(u, n.getSender());
                            assertEquals("CustomSubject", n.getSubject());
                            assertEquals("CustomBody", n.getBody());
                            assertTrue(n.getReceiver() == eas.get(0).getUser() || n.getReceiver() == eas.get(1).getUser());
                        }
                        assertNotEquals(data1.result.get(0).getReceiver(), data1.result.get(1).getReceiver());
                    })) return;
                    completeTest();
                });
            });
        });
        await();
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
        db.testDB.getInstance(i.getLocType(), i.getLocType().instanceIDFromDatabaseID(i.getLocID()), (inst,s) -> {
            if(!asserts(()->{
                assertTrue(s);
                assertNull(inst.getAssociatedImage());
            })) return;
            testDeletedImageCascade(i, onComplete);
        });
    }

    public void testDeletedImageCascade(Image i, Runnable onComplete){
        if(i == null){
            onComplete.run();
            return;
        }
        TestDatabase.storage.child(i.getImageID()).getDownloadUrl().addOnCompleteListener(t -> {
            if(!asserts(() -> {
                assertFalse(t.isSuccessful());
            })) return;
            testDeletedInstance(i, onComplete);
        });
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
            assertNull(f.getOrganizer().getFacility());
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
    public void US030101() throws Throwable {
        getTestEventAssociationFresh(EVENT_REG, "Waitlist", false, null, true, false, true, true, ea -> {
            Event e = ea.getEvent();
            Facility f = e.getFacility();
            getTestNotificationFresh(N_FRESH_REC | N_FRESH_SEND, null, null, e, n_event -> {
                e.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                    try {
                        TimeUnit.SECONDS.sleep(2); //wait for updates
                    } catch (Throwable ignored) {}
                    if(asserts(() -> {
                        assertTrue(s);
                        assertTrue(f.isLegalState());
                    })){
                        testDeletedEvent(e, ea, n_event, this::completeTest);
                    };
                });
            });
        });
        await();
    }

    @Test
    public void US030201_shallow() throws Throwable {
        getTestUser(false, u -> {
            u.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                assertTrue(s);
                testDeletedUserShallow(u, this::completeTest);
            });
        });
        await();
    }

    @Test
    public void US030201_deep_noNotificationsImages
            () throws Throwable {
        getTestEventAssociationFresh(EVENT_REG, "Waitlist", false, null, false, false, false, false, ea -> {
            Event e = ea.getEvent();
            User u = e.getFacility().getOrganizer();
            getTestEventAssociationFreshUser(u, EVENT_REG, "Waitlist", false, null, false, false, false, ea_assoc -> {
                u.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                    if(!asserts(() -> assertTrue(s))) return;
                    testDeletedUserDeep(u, e, ea_assoc, ea, null, null, null, this::completeTest);
                });
            });
        });
        await();
    }

    @Test
    public void US030201_deep_noImages() throws Throwable{
        getTestEventAssociationFresh(EVENT_REG, "Waitlist", false, null, false, false, false, false, ea -> {
            Event e = ea.getEvent();
            User u = e.getFacility().getOrganizer();
            getTestEventAssociationFreshUser(u, EVENT_REG, "Waitlist", false, null, false, false, false, ea_assoc -> {
                getTestNotificationFresh(N_FRESH_REC | N_FRESH_SEND, null, null, e, n_event -> {
                    getTestNotificationFresh(N_FRESH_REC | N_FRESH_EVENT, u, null, null, n_send -> {
                        getTestNotificationFresh(N_FRESH_SEND | N_FRESH_EVENT, null, u, null, n_rec -> {
                            u.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                                if(asserts(() -> assertTrue(s))){
                                    try {
                                        TimeUnit.SECONDS.sleep(2); //wait for updates
                                    } catch (Throwable ignored) {}
                                    testDeletedUserDeep(u, e, ea_assoc, ea, n_rec, n_send, n_event, this::completeTest);
                                };
                            });
                        });
                    });
                });
            });
        });
        await();
    }

    @Test
    public void US030201_deep() throws Throwable{
        getTestEventAssociationFresh(EVENT_REG, "Waitlist", false, null, true, false, true, true, ea -> {
            Event e = ea.getEvent();
            User u = e.getFacility().getOrganizer();
            getTestEventAssociationFreshUser(u, EVENT_REG, "Waitlist", false, null, false, false, false, ea_assoc -> {
                getTestNotificationFresh(N_FRESH_REC | N_FRESH_SEND, null, null, e, n_event -> {
                    getTestNotificationFresh(N_FRESH_REC | N_FRESH_EVENT, u, null, null, n_send -> {
                        getTestNotificationFresh(N_FRESH_SEND | N_FRESH_EVENT, null, u, null, n_rec -> {
                            u.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                                if(asserts(() -> assertTrue(s))){
                                    try {
                                        TimeUnit.SECONDS.sleep(2); //wait for updates
                                    } catch (Throwable ignored) {}
                                    testDeletedUserDeep(u, e, ea_assoc, ea, n_rec, n_send, n_event, this::completeTest);
                                };
                            });
                        });
                    });
                });
            });
        });
        await();
    }

    @Test
    public void US030301_user() throws Throwable{
        getTestUser(true, u -> {
            Image i = u.getProfileImage();
            i.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, success -> {
                if(!asserts(() -> assertTrue(success))) return;
                try {
                    TimeUnit.SECONDS.sleep(2); //wait for updates
                } catch (Throwable ignored) {}
                if(!asserts(() -> assertEquals("", u.getProfileImageID()))) return;
                testDeletedImage(i, this::completeTest);
            });
        });
        await();
    }

    @Test
    public void US030301_event() throws Throwable {
        getTestEventFresh(EVENT_REG, false, true, false, false, e -> {
            Image i = e.getPoster();
            i.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, success -> {
                if(!asserts(() -> assertTrue(success))) return;
                try {
                    TimeUnit.SECONDS.sleep(2); //wait for updates
                } catch (Throwable ignored) {}
                if(!asserts(() -> assertEquals("", e.getPosterID()))) return;
                testDeletedImage(i, this::completeTest);
            });
        });
        await();
    }

    @Test
    public void US030301_facility() throws Throwable {
        getTestFacilityFresh(true, false, f -> {
            Image i = f.getImage();
            i.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, success -> {
                if(!asserts(() -> assertTrue(success))) return;
                try {
                    TimeUnit.SECONDS.sleep(2); //wait for updates
                } catch (Throwable ignored) {}
                if(!asserts(() -> assertEquals("", f.getImageID()))) return;
                testDeletedImage(i, this::completeTest);
            });
        });
        await();
    }

    @Test
    public void US030302() throws Throwable {
        getTestEventFresh(EVENT_REG, false, false, false, false, e -> {
            e.setQrHash("");
            if(asserts(() -> {
                assertNotEquals(e.getQrHash(), e.getDocumentID());
                assertEquals("", e.getQrHash());
            })) completeTest();
        });
        await();
    }

    @Test
    public void US030401() throws Throwable {
        getTestEventFresh(EVENT_REG, false, false, false, false, e1 -> {
            getTestEventFresh(EVENT_REG, false, false, false, false, e2 -> {
                DatabaseInfLoadQuery<Event> q = new DatabaseInfLoadQuery<>(DatabaseQuery.getEvents(db.testDB));
                q.incrementData((query, success) -> {
                    if(!asserts(() -> {
                        assertTrue(success);
                        List<Event> es = q.getInstances();
                        assertTrue(es.size() >= 2);
                        assertTrue(es.contains(e1));
                        assertTrue(es.contains(e2));
                    })) return;
                    completeTest();
                });
            });
        });
        await();
    }

    @Test
    public void US030501() throws Throwable {
        getTestUser(false, e1 -> {
            getTestUser(false, e2 -> {
                DatabaseInfLoadQuery<User> q = new DatabaseInfLoadQuery<>(DatabaseQuery.getUsers(db.testDB));
                q.incrementData((query, success) -> {
                    if(!asserts(() -> {
                        assertTrue(success);
                        List<User> es = q.getInstances();
                        assertTrue(es.size() >= 2);
                        assertTrue(es.contains(e1));
                        assertTrue(es.contains(e2));
                    })) return;
                    completeTest();
                });
            });
        });
        await();
    }

    @Test
    public void US030601() throws Throwable {
        getTestUser(true, e1 -> {
            getTestUser(true, e2 -> {
                DatabaseInfLoadQuery<Image> q = new DatabaseInfLoadQuery<>(DatabaseQuery.getImages(db.testDB));
                q.incrementData((query, success) -> {
                    if(!asserts(() -> {
                        assertTrue(success);
                        List<Image> es = q.getInstances();
                        assertTrue(es.size() >= 2);
                        assertTrue(es.contains(e1.getProfileImage()));
                        assertTrue(es.contains(e2.getProfileImage()));
                    })) return;
                    completeTest();
                });
            });
        });
        await();
    }

    @Test
    public void US030701() throws Throwable {
        getTestEventAssociationFresh(EVENT_REG, "Waitlist", false, null, true, false, true, true, ea -> {
            Event e = ea.getEvent();
            Facility f = e.getFacility();
            User u = e.getFacility().getOrganizer();
            getTestNotificationFresh(N_FRESH_REC | N_FRESH_SEND, null, null, e, n_event -> {
                f.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                    try {
                        TimeUnit.SECONDS.sleep(2); //wait for updates
                    } catch (Throwable ignored) {}
                    if(asserts(() -> {
                        assertTrue(s);
                        assertEquals("", u.getFacilityID());
                        assertNull(u.getFacility());
                        assertTrue(u.isLegalState());
                    })){
                        testDeletedFacilityDeep(f, e, ea, n_event, this::completeTest);
                    };
                });
            });
        });
        await();
    }


}
