package com.syzygy.events;

import com.google.firebase.firestore.FirebaseFirestore;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.User;

import org.junit.jupiter.api.parallel.Resources;
import org.mockito.Mock;

public class UserTest {

    @Mock
    Database mockDatabase;
    @Mock
    Resources mockResources;
    @Mock
    FirebaseFirestore mockFirestore;

    private User user;

}
