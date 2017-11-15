package com.pulseapp.android.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.SearchAdapter;
import com.pulseapp.android.data.MemoryCachedUsersData;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.FaceBookFriendsModel;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.modelView.SearchResultModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.FontPicker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by deepankur on 7/11/16.
 */
public class SearchFragment extends BaseFragment {

    public enum SearchSource {DASHBOARD, ADD_FRIEND}

    public SearchSource searchSource;
    public static final int SEARCH_MY_FRIEND_STREAM = 11,
            SEARCH_AROUND_YOU = 22,
            SEARCH_ADD_BY_USERNAME = 33,
            SEARCH_FRIEND_REQUESTS = 44,
            SEARCH_ALL_FRIENDS = 55,
            SEARCH_SUGGESTED_FRIENDS = 66;

    private ChildSearchFragment[] childFragments;
    private int[] searchTags;
    private int[] containerId;

    private String TAG = getClass().getSimpleName();
    private TextView noResultTv;
    private SocialModel socialModel;
    private Handler queryHandler = new Handler();
    private NestedScrollView scrollView;
    private boolean isDestroyed;
    private static HashMap<String, SearchResultModel> userId_ApiSearch_cached;
    private static HashSet<String> ignoreUserSearchSet = new HashSet<>();
    static MemoryCachedUsersData cachedUsersData;
    private DashBoardFragment dashBoardFragmentReference;

    public void setDashBoardFragmentReference(DashBoardFragment dashBoardFragmentReference) {
        this.dashBoardFragmentReference = dashBoardFragmentReference;
        SearchAdapter.dashboardFragment = this.dashBoardFragmentReference;
    }

    public static void updateIgnoreUserSearchList(String id) {
        ignoreUserSearchSet.add(id);
    }

    public void clearSearchCache() {
        userId_ApiSearch_cached.clear();
        ignoreUserSearchSet.clear();
    }

    private FireBaseHelper.SearchFriendListener searchFriendListener = new FireBaseHelper.SearchFriendListener() {
        @Override
        public void onSearchResultChanged(String query, ArrayList<SearchResultModel> searchResultModels) {
            if (userId_ApiSearch_cached == null)
                userId_ApiSearch_cached = new HashMap<>();
            if (searchResultModels != null && searchResultModels.size() > 0) {
//                userId_ApiSearch_chchedMAp.put(sea, searchResultModels);
                for (SearchResultModel model : searchResultModels) {
                    if (ignoreUserSearchSet.contains(model.searchId))
                        continue;
                    userId_ApiSearch_cached.put(model.searchId, model);

                }
                Log.d(TAG, "cached the result ");
                loadApiResponse(null);
                toggleNoSearchVisibility();
            } else Log.d(TAG, " skipping empty/null data");
        }
    };

    private String previousQuery = null;
    private SearchView searchView;
    private HandlerThread searchThread;
    private Handler searchHandler;
    private View progressView;
    private boolean searchInProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            return;

        TAG += " " + searchSource;
        Log.d(TAG, " onCreate");
        //  initChildFragmentsCount();
    }

    private void initChildFragmentsCount(View rootView) {
        if (this.searchSource==null) this.searchSource = SearchSource.DASHBOARD;

        if (this.searchSource == SearchSource.DASHBOARD) {
            childFragments = new ChildSearchFragment[6];
            searchTags = new int[]{SEARCH_MY_FRIEND_STREAM, SEARCH_AROUND_YOU, SEARCH_ADD_BY_USERNAME, SEARCH_FRIEND_REQUESTS, SEARCH_ALL_FRIENDS, SEARCH_SUGGESTED_FRIENDS};
            addChildContainers(6, rootView);
//            containerId = new int[]{R.id.searchLL1, R.id.searchLL2, R.id.searchLL3, R.id.searchLL4, R.id.searchLL5, R.id.searchLL6};
        } else if (this.searchSource == SearchSource.ADD_FRIEND) {
            childFragments = new ChildSearchFragment[4];
            searchTags = new int[]{SEARCH_ADD_BY_USERNAME, SEARCH_FRIEND_REQUESTS, SEARCH_SUGGESTED_FRIENDS, SEARCH_ALL_FRIENDS};
            addChildContainers(4, rootView);
//            containerId = new int[]{R.id.searchLL1, R.id.searchLL2, R.id.searchLL3};
        }

        for (int i = 0; i < childFragments.length; i++) {
            childFragments[i] = new ChildSearchFragment();
        }
    }

    void addChildContainers(int count, View rootView) {
        containerId = new int[count];
        for (int i = 0; i < count; i++) {
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            int id = View.generateViewId();
            ll.setId(id);
            containerId[i] = id;
            ((LinearLayout) rootView.findViewById(R.id.searchResultsLayout)).addView(ll);
        }
    }

    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            if (searchView.getQuery().toString().trim().toLowerCase().equals(previousQuery)) {
                queryHandler.removeCallbacksAndMessages(null);
                Log.d(TAG, "hitting api for search on query--- " + previousQuery);
                if (mFireBaseHelper != null && searchHandler!=null) {
                    mFireBaseHelper.hitSearchApi(previousQuery);
                        searchHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                refreshData(previousQuery, false);
                            }
                        });

                }
            }
        }
    };

    private ValueEventListener socialModelListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            socialModel = dataSnapshot.getValue(SocialModel.class);
            refreshData(previousQuery == null ? "" : previousQuery, true);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

    };
    private DatabaseReference socialFireBase;
    private boolean isSearchViewOpen;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    public void registerSearchView(SearchView searchView) {
        this.searchView = searchView;
    }

    public void setQuery(String query) {
        query = query.toLowerCase().trim();
        previousQuery = query;
        int WAIT_TIME_FOR_QUERY = 800;
        queryHandler.postDelayed(queryRunnable, WAIT_TIME_FOR_QUERY);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, " onCreateView form " + searchSource);
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        if (mFireBaseHelper == null || savedInstanceState != null) return rootView;

        socialModel = mFireBaseHelper.getSocialModel();
        if (socialModel==null) return rootView;

        mFireBaseHelper.addSearchFriendListener(searchFriendListener);
        socialFireBase = mFireBaseHelper.getNewFireBase(ANCHOR_SOCIALS, new String[]{myUserId});
        socialFireBase.addValueEventListener(socialModelListener);

        if(this.searchThread==null) {
            this.searchThread = new HandlerThread("SearchThread");
            searchThread.start();
        }

        if (this.searchHandler==null)
            searchHandler = new Handler(searchThread.getLooper());


        progressView = rootView.findViewById(R.id.progressFrame);
        progressView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;// for blocking touch events
            }
        });

        LinearLayout searchLayout = (LinearLayout) rootView.findViewById(R.id.searchResultsLayout);
        scrollView = (NestedScrollView) rootView.findViewById(R.id.scrollView);
        noResultTv = (TextView) rootView.findViewById(R.id.noResultTV);
        searchLayout.setOrientation(LinearLayout.VERTICAL);
        initChildFragmentsCount(rootView);
        return rootView;
    }

    private void toggleNoSearchVisibility() {
        boolean resultsPresent = false;
        for (ChildSearchFragment fragment : childFragments) {
            if (fragment.getSearchResultModels() != null && fragment.getSearchResultModels().size() > 0) {
                resultsPresent = true;
                break;
            }
        }
        scrollView.setVisibility(resultsPresent ? View.VISIBLE : View.GONE);
        noResultTv.setVisibility(resultsPresent ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState!=null || mFireBaseHelper==null || mFireBaseHelper.getSocialModel()==null) return;

        loadFragments();
        refreshData("", false);
    }



    public void refreshDataOnToggle() {
        if (previousQuery != null)
            refreshData(previousQuery, false);
    }

    private void loadFragments() {
        for (int i = 0; i < childFragments.length; i++) {
            if (childFragments[i] == null) {
                childFragments[i] = new ChildSearchFragment();
            }
            childFragments[i].setSearchTag(searchTags[i]);
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(containerId[i], childFragments[i], SearchFragment.class.getSimpleName());
//            containerId[i]+=345;
            fragmentTransaction.commitAllowingStateLoss();
        }
    }


    /**
     * @param rootView ,Note: it belongs to dashboard fragment and not this
     */
    public void initSearchView(final View rootView) {
        searchView = (SearchView) rootView.findViewById(R.id.action_bar_search_view);
        EditText searchEditText = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);

        if (fontPicker == null)
            fontPicker = FontPicker.getInstance(MasterClass.getGlobalContext());

        searchEditText.setTypeface(fontPicker.getMontserratRegular());
//        searchEditText.setAlpha(0.87f);
        searchEditText.setTextColor(Color.WHITE);
        searchEditText.setBackgroundColor(Color.TRANSPARENT);
        searchEditText.setHintTextColor(Color.parseColor("#80FFFFFF"));
        if (searchSource == SearchSource.ADD_FRIEND)
            searchEditText.setHint("Search User");
        else if (searchSource == SearchSource.DASHBOARD)
            searchEditText.setHint("Search");

        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(searchEditText, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundColor(Color.TRANSPARENT);

        final ImageView searchClose = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchClose.setColorFilter(Color.WHITE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, " onQueryTextSubmit " + query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, " onQueryTextChange " + newText);
                setQuery(newText);
                return false;
            }
        });
    }


    public void searchViewOpened() {
        isSearchViewOpen = true;
    }

    public void searchViewClosed() {
        isSearchViewOpen = false;

    }


    /**
     * @param previousQuery the query in searchView supply empty string for all results
     * @param blockUI       supply true if there has been a change in any of the data structure concerned with
     *                      the search and the UI will be blocked by a progress bar to avoid any interference with
     *                      stale data
     */
    private void refreshData(String previousQuery, boolean blockUI) {
        if (blockUI)
            progressView.setVisibility(View.VISIBLE);
        refreshLocalData(previousQuery);

        progressView.setVisibility(View.GONE);
        if (getActivity()!=null)
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggleNoSearchVisibility();
            }
        });
    }

    private void refreshLocalData(String query) {
        HashSet<String> userWithActiveMomentSet = new HashSet<>();

        for (ChildSearchFragment childFragment : childFragments) {
            switch (childFragment.getSearchTag()) {
                case SEARCH_MY_FRIEND_STREAM:


                    ArrayList<HomeMomentViewModel> friendsWithActiveMomentList = null;//represents both friend and followers
                    if (dashBoardFragmentReference != null && dashBoardFragmentReference.getFriendsMomentsFragment() != null) {
                         friendsWithActiveMomentList = dashBoardFragmentReference.getFriendsMomentsFragment().getFriendsWithMomentsList();
                    }

                    ArrayList<SearchResultModel> searchResults_myFriendStream = new ArrayList<>();
                    if (friendsWithActiveMomentList != null)
                        for (HomeMomentViewModel momentRoomView : friendsWithActiveMomentList) {
                            String userId = momentRoomView.userId;
                            if (userId == null)
                                continue;
                            if (momentRoomView.name.toLowerCase().contains(query)) {
                                SearchResultModel searchResultModel = new SearchResultModel(userId, momentRoomView.imageUrl, null, momentRoomView.name, null);
                                searchResultModel.momentUpdatedAt = momentRoomView.updatedAtText;
                                searchResults_myFriendStream.add(searchResultModel);
                            }
                            userWithActiveMomentSet.add(userId);
                        }

                    notifyChildSearchResultChanged(childFragment, searchResults_myFriendStream);

                    break;
                case SEARCH_AROUND_YOU:
                    final ArrayList<HomeMomentViewModel> momentViewModels = AroundYouFragment.getMomentViewModels();
                    ArrayList<SearchResultModel> searchResults_nearby_moment = new ArrayList<>();
                    if (momentViewModels != null)
                        for (HomeMomentViewModel homeMomentViewModel : momentViewModels) {
                            if (homeMomentViewModel != null && homeMomentViewModel.name.toLowerCase().contains(query))
                                searchResults_nearby_moment.add(new SearchResultModel(homeMomentViewModel.momentId, homeMomentViewModel.imageUrl, null, homeMomentViewModel.name, null));
                        }
                    notifyChildSearchResultChanged(childFragment, searchResults_nearby_moment);
                    break;
                case SEARCH_ADD_BY_USERNAME:
                    loadApiResponse(query);
                    break;
                case SEARCH_FRIEND_REQUESTS:
                    // friend requests
                    ArrayList<SearchResultModel> searchResults_requests = new ArrayList<>();
                    final HashMap<String, SocialModel.RequestReceived> requestReceived = mFireBaseHelper.getPendingFriendRequests();
                    if (requestReceived != null) {
                        for (Map.Entry<String, SocialModel.RequestReceived> entry : requestReceived.entrySet()) {
                            final SocialModel.RequestReceived value = entry.getValue();
                            if (value.name.toLowerCase().contains(query))
                                searchResults_requests.add(new SearchResultModel(entry.getKey(), value.imageUrl, value.handle, value.name, REQUEST_RECEIVED));
                        }
                    }

                    if (socialModel==null) break; //Wait for social model to be queried

                    //group requests
                    final HashMap<String, SocialModel.PendingGroupRequest> pendingGroupRequest = socialModel.pendingGroupRequest;
                    if (pendingGroupRequest != null) {
                        for (Map.Entry<String, SocialModel.PendingGroupRequest> entry : pendingGroupRequest.entrySet()) {
                            final SocialModel.PendingGroupRequest value = entry.getValue();
                            if (value.name.toLowerCase().contains(query)) {
                                SearchResultModel resultModel = new SearchResultModel(entry.getKey(), null, null, value.name, PENDING_GROUP_REQUEST);

                                searchResults_requests.add(resultModel);
                            }
                        }
                    }

                    //sent friend request
                    final Map<String, Integer> requestSent = socialModel.requestSent;
                    if (cachedUsersData == null)
                        cachedUsersData = MemoryCachedUsersData.getInstance(context);
                    if (requestSent != null) {
                        for (Map.Entry<String, Integer> entry : socialModel.requestSent.entrySet()) {
                            if (cachedUsersData.getUserData(entry.getKey()) != null) {
                                SearchResultModel resultModel = cachedUsersData.getUserData(entry.getKey());//// FIXME: 7/23/16 ; fix from firebase
                                if (!isAlreadyAFriend(entry.getKey()) && isAlreadyFriendRequestReceived(entry.getKey()) && /*!isFriendRequestAlreadySent(entry.getKey()) &&*/ cachedUsersData.getUserData(entry.getKey()).name.toLowerCase().contains(query)) {
                                    resultModel.setRequestSentTrue();
                                    searchResults_requests.add(resultModel);
                                }
                            }
                        }
                    }

                    notifyChildSearchResultChanged(childFragment, searchResults_requests);
                    break;

                case SEARCH_ALL_FRIENDS:
//                    final ArrayList<ListRoomView> listRoomView1 = mFireBaseHelper.getListRoomView();
                    ArrayList<SearchResultModel> searchResults_allFriends = new ArrayList<>();
//                    if (listRoomView1!=null) {
//                        for (ListRoomView roomView : listRoomView1) {
//                            if (roomView.name.toLowerCase().contains(query))
//                                if (roomView.type == FRIEND_ROOM) {
//                                    if (!userWithActiveMomentSet.contains(roomView.userId))
//                                        searchResults_allFriends.add(new SearchResultModel(roomView.userId, roomView.imageUrl, roomView.handle, roomView.name, FRIEND_ROOM));
//                                } else if (roomView.type == GROUP_ROOM)
//                                    searchResults_allFriends.add(new SearchResultModel(roomView.roomId, null, null, roomView.name, GROUP_ROOM));
//                        }
//                    }

                    final HashMap<String, SocialModel.Friends> friends = mFireBaseHelper.getSocialModel().friends;
                    if (friends != null)
                        for (Map.Entry<String, SocialModel.Friends> friendEntry:friends.entrySet()){
                            if (!userWithActiveMomentSet.contains(friendEntry.getKey())){
                                if (friendEntry.getValue().name.toLowerCase().contains(query))
                                    searchResults_allFriends.add(new SearchResultModel(friendEntry.getKey(), friendEntry.getValue().imageUrl, friendEntry.getValue().handle, friendEntry.getValue().name, FRIEND_ROOM));
                            }
                        }

                    notifyChildSearchResultChanged(childFragment, searchResults_allFriends);
                    break;
                case SEARCH_SUGGESTED_FRIENDS:
                    ArrayList<SearchResultModel> searchResults_facebook_sugested_friend = new ArrayList<>();

                    if (socialModel==null) break; //Wait for social model to be queried

                    final HashMap<String, FaceBookFriendsModel> facebookFriends = socialModel.facebookFriends;
                    if (facebookFriends != null) {
                        for (Map.Entry<String, FaceBookFriendsModel> entry : facebookFriends.entrySet()) {
                            final FaceBookFriendsModel value = entry.getValue();
                            if (!(isFriendRequestAlreadySent(entry.getKey())) && !(isAlreadyFriendRequestReceived(entry.getKey())) && (value.name.toLowerCase().contains(query) || (value.handle != null && value.handle.toLowerCase().contains(query))))
                                searchResults_facebook_sugested_friend.add(new SearchResultModel(entry.getKey(), value.imageUrl, value.handle, value.name, FRIEND_ROOM));
                        }
                    }

                    notifyChildSearchResultChanged(childFragment, searchResults_facebook_sugested_friend);
                    break;
                default:
                    throw new RuntimeException("undefined search tag");
            }
        }
    }

    private boolean isAlreadyAFriend(String usrId) {
        final HashMap<String, SocialModel.Friends> friends = mFireBaseHelper.getSocialModel().friends;
        if (friends == null)
            return false;
        for (Map.Entry<String, SocialModel.Friends> entry : friends.entrySet()) {
            if (entry.getKey().equals(usrId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFriendRequestAlreadySent(String userId) {
        final Map<String, Integer> requestSent = socialModel.requestSent;
        if (requestSent == null) return false;
        final Set<String> userIds = requestSent.keySet();
        return userIds.contains(userId);
    }

    private boolean isAlreadyFriendRequestReceived(String userId) {
        final HashMap<String, SocialModel.RequestReceived> requestReceived = socialModel.requestReceived;
        if (requestReceived == null) return false;
        for (Map.Entry<String, SocialModel.RequestReceived> entry : requestReceived.entrySet()) {
            if (entry.getKey().equals(userId))
                return true;
        }
        return false;
    }

    private void notifyChildSearchResultChanged(final ChildSearchFragment fragment, final ArrayList<SearchResultModel> searchResultModels) {
        if (getActivity() != null)
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragment.setSearchResultModels(searchResultModels);
                }
            });
    }

    private void loadApiResponse(String query) {
        if (userId_ApiSearch_cached == null)
            return;
        if (query == null)
            query = previousQuery;
        ArrayList<SearchResultModel> searchResults_byUserName = new ArrayList<>();

        for (Map.Entry<String, SearchResultModel> entry : userId_ApiSearch_cached.entrySet()) {
            if (ignoreUserSearchSet.contains(entry.getKey()))
                continue;
            final SearchResultModel value = entry.getValue();
            if ((!isAlreadyAFriend(value.searchId)&&!isFriendRequestAlreadySent(value.searchId)) && (value.handle.toLowerCase().contains(query) || value.name.toLowerCase().contains(query)))
                searchResults_byUserName.add(value);
        }
        for (ChildSearchFragment childFragment : childFragments) {
            if (childFragment.getSearchTag() == SEARCH_ADD_BY_USERNAME) {
                notifyChildSearchResultChanged(childFragment, searchResults_byUserName);
                break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        isDestroyed = true;

        if (mFireBaseHelper==null || socialFireBase==null) return;

        mFireBaseHelper.removeSearchFriendListener(searchFriendListener);
        socialFireBase.removeEventListener(socialModelListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (searchHandler==null) return;

        searchHandler.removeCallbacksAndMessages(null);
//        searchThread.quitSafely();
    }
}
