package com.example.pokedraw;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public final class FirebaseUserBootstrap {
    private FirebaseUserBootstrap() {}

    public static void ensureUserNode(@NonNull String uid, @NonNull String email) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.child("meta").child("email").setValue(email);
        userRef.child("meta").child("lastLoginAt").setValue(System.currentTimeMillis());

        // Create collection node if missing so the DB path is visible and stable.
        Map<String, Object> empty = new HashMap<>();
        userRef.child("collection").updateChildren(empty);
    }
}
