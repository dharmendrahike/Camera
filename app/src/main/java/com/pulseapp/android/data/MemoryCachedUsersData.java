package com.pulseapp.android.data;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.SearchResultModel;
import com.pulseapp.android.models.UserModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by deepankur on 7/18/16.
 */
public class MemoryCachedUsersData implements FireBaseKEYIDS {

    public enum DataType {USERS}

    FireBaseHelper fireBaseHelper;
    private static MemoryCachedUsersData memoryCachedUsersData;
    HashMap<String, CachedData> usersDataMap;

    public static MemoryCachedUsersData getInstance(Context context) {
        if (memoryCachedUsersData == null)
            memoryCachedUsersData = new MemoryCachedUsersData(context);
        return memoryCachedUsersData;
    }

    private MemoryCachedUsersData(Context context) {
        fireBaseHelper = FireBaseHelper.getInstance(context);
        usersDataMap = new HashMap<>();
    }

    private static class CachedData {
        String userId;
        String imageUrl;
        String name;
        String handle;

        public CachedData(String userId, String imageUrl, String name, String handle) {
            this.userId = userId;
            this.imageUrl = imageUrl;
            this.name = name;
            this.handle = handle;
        }
    }

    private void requestUserData(final String userId) {
        fireBaseHelper.getNewFireBase(ANCHOR_USERS, new String[]{userId}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserModel userModel = dataSnapshot.getValue(UserModel.class);
                CachedData cachedData = new CachedData(dataSnapshot.getKey(), userModel.imageUrl, userModel.name, userModel.handle);
                usersDataMap.put(dataSnapshot.getKey(), cachedData);
                userModel = null;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void initSentRequestData(Map<String, Integer> sentRequestMap) {
        if (sentRequestMap != null)
            for (Map.Entry<String, Integer> entry : sentRequestMap.entrySet()) {
                getUserData(entry.getKey());
            }
    }

    public void putUserData(SearchResultModel searchResultModel) {
        CachedData data = new CachedData(searchResultModel.searchId, searchResultModel.imageUrl, searchResultModel.name, searchResultModel.handle);
        this.usersDataMap.put(searchResultModel.searchId, data);
    }

    public SearchResultModel getUserData(String userId) {
        final CachedData cachedData = usersDataMap.get(userId);
        if (cachedData == null) {
            requestUserData(userId);
            return null;
        }
        return new SearchResultModel(cachedData.userId, cachedData.imageUrl, cachedData.handle, cachedData.name, FRIEND_ROOM);
    }
}
