package com.pulseapp.android.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.HomeMomentAdapter;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;


/**
 * Created by deepankur on 27/4/16.
 */
public class AroundYouFragment extends BaseFragment implements LocationListener {

    private RecyclerView recyclerView;
    private String TAG = getClass().getSimpleName();
    private boolean isDestroyed;
    private View rootView;
    public static Location knownLocation; //This knownLocation in AroundYouFragment is continuously updated to the last known location within the whole app
    private GoogleApiClient googleApiClient;
    private ProgressBar progressBar;
    private static final int REQUEST_CHECK_SETTINGS = 7;
    private final int PERMISSION_ACCESS_LOCATION = 0;
    LocationRequest locationRequest;
    private static boolean requestLocationUpdate = false;
    private TextView locationButton;
    private TextView aroundYouHeaderTv;
    private static boolean aroundYouMomentsLoaded = false;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            return;

        downloader = DynamicDownloader.getInstance(context);
        googleApiClient = new GoogleApiClient.Builder(context, connectionCallbacks, connectionFailedListener).addApi(LocationServices.API).build();
        Log.d(TAG, " onCreateCalled @ " + System.currentTimeMillis());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_around_you, container, false);
        this.rootView = rootView;
        if (savedInstanceState != null)
            return rootView;

        locationButton = (TextView) rootView.findViewById(R.id.locationBtn);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.ENABLE_LOCATION_STREAMS);
                requestStartLocationUpdate();
            }
        });
        this.progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        aroundYouHeaderTv = (TextView) rootView.findViewById(R.id.aroundYouHeaderTV);
        setStreamsPopupListener(new StreamsPopupListener() {
            @Override
            public void onWatchNowClicked(HomeMomentViewModel model) {
                AppLibrary.log_d(TAG, "on Watch now clicked init recycler");
                DynamicDownloader.notifyWatchNowTapped(model.momentId);
                addPublicMomentFromShare(model, true);

                // move moment to left
                ArrayList<HomeMomentViewModel> modelsList;
                HomeMomentAdapter adapter;
                final RecyclerView recycler;
                boolean isArticle = isAnArticle(model.momentId);
                if (isArticle) {
                    modelsList = articleMomentViewModels;
                    adapter = articleAdapter;
                    recycler = articleRecycler;
                } else {
                    modelsList = momentViewModels;
                    adapter = homeMomentAdapter;
                    recycler = recyclerView;
                }

                if (modelsList != null)
                    for (int i = 0; i < modelsList.size(); i++) {
                        HomeMomentViewModel momentViewModel = modelsList.get(i);
                        if (momentViewModel.momentId != null && momentViewModel.momentId.equals(model.momentId)) {
                            swapMomentToLeft(modelsList, momentViewModel, adapter, 0);
                            if (recycler != null) {
                                new android.os.Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!recycler.isComputingLayout())
                                            recycler.smoothScrollToPosition(0);
                                    }
                                }, 1000);
                            }
                            break;
                        }
                    }
            }
        });
        return rootView;
    }

    private void startLocationUpdate() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) { //Will check permissions in Marshmallow
            Log.e(TAG, "Request location permissions");
            {
//                locationButton.setVisibility(View.VISIBLE); //TODO change as per design
                refreshAroundYouData(null, null);//hitting api although we don't have permissions itself
            }
        } else {
            if (knownLocation == null)
                knownLocation = getNewLocationFromFusedProvider();

            if (knownLocation != null) {
                locationButton.setVisibility(View.GONE);
                double lat = knownLocation.getLatitude(), lon = knownLocation.getLongitude();
                Log.d(TAG, " getNewLocationFromFusedProvider: lat " + lat + " long " + lon);
                refreshAroundYouData(lat, lon);
            } else {
//                locationButton.setVisibility(View.VISIBLE); //TODO change as per design
                Log.d(TAG, "Unable to fetch location");
                refreshAroundYouData(null, null);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResumeCalled");
    }


    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (googleApiClient != null)
            googleApiClient.disconnect();
    }

    boolean isFirstTime = true;
    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, " onConnectionFailed " + connectionResult);
        }
    };
    private final short PERMISSION_ACCESS_COARSE_LOCATION = 32213;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, " onConnected " + bundle);

            if (isFirstTime) {
                startLocationUpdate();
                isFirstTime = false;
            }

            if (requestLocationUpdate) {
                requestStartLocationUpdate();
                requestLocationUpdate = false;
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, " onConnectionSuspended " + i);
        }
    };

    private Location getNewLocationFromFusedProvider() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            Log.d(TAG, " getNewLocationFromFusedProvider " + lastLocation);
            return lastLocation;
        } else {
            Log.e(TAG, " getNewLocationFromFusedProvider failed");
            return null;
        }
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.nearbyRecycler);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                DynamicDownloader.notifyScrollDetectedInDashboard();
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        if (momentViewModels != null && momentViewModels.size() > 0)
            rootView.setVisibility(View.VISIBLE);

        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        recyclerView.setLayoutManager(layoutManager);
        if (homeMomentAdapter == null) {
            homeMomentAdapter = new HomeMomentAdapter(myUserId, context, momentViewModels, AROUND_YOU_MOMENT_RECYCLER, new RecyclerViewClickInterface() {
                @Override
                public void onItemClick(int extras, Object data) {
                    HomeMomentViewModel model = (HomeMomentViewModel) data;
                    if (model.clickType == HomeMomentViewModel.ClickType.SINGLE_TAP) {
                        onSingleTapOnMoment(model);
                    } else if (model.clickType == HomeMomentViewModel.ClickType.DOUBLE_TAP) {
                        Log.d(TAG, " got double click on " + model.momentId);
                    } else if (model.clickType == HomeMomentViewModel.ClickType.LONG_PRESS) {
                        showStreamPopup(getActivity(), model.momentId, model.name, model.imageUrl, model.handle, model.description, model.isAnArticle);
                    }
                }
            });
            homeMomentAdapter.setRecyclerView(recyclerView);
            recyclerView.setAdapter(homeMomentAdapter);
        } else {
            homeMomentAdapter.setMomentModelArrayList(momentViewModels);
        }
        initArticles();
    }

    private HomeMomentAdapter homeMomentAdapter;

    public void performSingleTapOnNearbyMoment(String momentId) {
        final ArrayList<HomeMomentViewModel> momentModelArrayList = ((HomeMomentAdapter) recyclerView.getAdapter()).getMomentModelArrayList();
        for (HomeMomentViewModel moment : momentModelArrayList) {
            if (moment.momentId.equals(momentId)) {
                onSingleTapOnMoment(moment);
                return;
            }
        }
        showShortToastMessage("Please try again!");
    }

    private void onSingleTapOnMoment(HomeMomentViewModel model) {
        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.PUBLIC_STREAM_CLICK, AnalyticsEvents.STREAM_NAME, model.name, AnalyticsEvents.STREAM_HANDLE, model.handle, AnalyticsEvents.STREAM_ID, model.momentId);

        final boolean ready = downloader.notifyTapOnMoment(model.momentId);
        if (ready)
            ((CameraActivity) getActivity()).loadViewPublicMomentFragment(model.momentId, READY_TO_VIEW_MOMENT, "public");

    }


    private final long API_CUT_OUT_TIME = 3500;
    Handler handler = new Handler();

    @SuppressWarnings("deprecation")
    public void refreshAroundYouData(Double lati, Double longi) {

        Log.d(TAG, "Refreshing aroundYou data");

        if (!isInternetAvailable(false) || mFireBaseHelper.getMyUserModel() == null) {
            Log.e(TAG, "Not hitting api; no internet or no social model");
            getMomentIdsFromFireBase();
            if (recyclerView != null)
                recyclerView.setVisibility(View.VISIBLE);

            if (progressBar != null)
                progressBar.setVisibility(View.GONE);
            return;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getMomentIdsFromFireBase();
            }
        }, API_CUT_OUT_TIME);

        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("userId", mFireBaseHelper.getMyUserId()));

        if (lati != null && longi != null) {
            pairs.add(new BasicNameValuePair("latitude", String.valueOf(lati)));
            pairs.add(new BasicNameValuePair("longitude", String.valueOf(longi)));

            if (knownLocation == null) {
                knownLocation = new Location("");
            }

            knownLocation.setLatitude(lati);
            knownLocation.setLongitude(longi);
            locationButton.setVisibility(View.GONE);
        }

        RequestManager.makePostRequest(context, RequestManager.MOMENTS_AROUND_YOU_REQUEST, RequestManager.MOMENTS_AROUND_YOU_RESPONSE,
                null, pairs, momentsAroundYouCallBack);
    }

    private static ArrayList<HomeMomentViewModel> momentViewModels;
    private static ArrayList<HomeMomentViewModel> articleMomentViewModels;

    public static ArrayList<HomeMomentViewModel> getMomentViewModels() {
        return momentViewModels;
    }

    private RequestManager.OnRequestFinishCallback momentsAroundYouCallBack = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                Log.d(TAG, " server says " + response);
                final JSONObject object = (JSONObject) response;
                if (success) {
                    JSONArray momentArray = object.getJSONArray("value");
                    Log.d(TAG, "momentsAroundYouCallBack response " + momentArray);

                    handler.removeCallbacksAndMessages(null);

                    final ArrayList<HomeMomentViewModel> momentViewModels = writeMomentIdsToFireBase(momentArray);
                    HashMap<String, String> loadedMomentThumbnail = null;
                    if (AroundYouFragment.momentViewModels == null)
                        AroundYouFragment.momentViewModels = new ArrayList<>();
                    else {
                        loadedMomentThumbnail = new HashMap<>();
                        for (HomeMomentViewModel model : AroundYouFragment.momentViewModels) {
                            if (model.thumbnailUrl != null)
                                loadedMomentThumbnail.put(model.momentId, model.thumbnailUrl);
                        }
                        AroundYouFragment.momentViewModels.clear();
                    }
                    downloader.setOnAroundYouDownloadListener(downloadListener);

                    if (articleMomentViewModels == null) {
                        articleMomentViewModels = new ArrayList<>();
                    } else articleMomentViewModels.clear();
                    for (HomeMomentViewModel model : momentViewModels) {
                        if (model.isAnArticle) {
                            AroundYouFragment.articleMomentViewModels.add(model);
                        } else {
                            if (loadedMomentThumbnail != null && loadedMomentThumbnail.containsKey(model.momentId)) {
                                model.showDisplayPicture = true;
                                model.thumbnailUrl = loadedMomentThumbnail.get(model.momentId);
                            }
                            AroundYouFragment.momentViewModels.add(model);
                        }
                        downloader.registerNewStreamForDownload(generateMomentModelFromHomeMomentModelView(model), DynamicDownloader.MomentType.PUBLIC_MOMENT);
                    }

                    if (waitingMomentModel != null) {
                        DynamicDownloader.notifyWatchNowTapped(waitingMomentModel.momentId);
                        addPublicMomentFromShare(waitingMomentModel, true);
                    }
                    downloader.setOnAroundYouDownloadListener(downloadListener);
                    initRecyclerView();
                    downloader.updateStatusForAllMomentsOnRefresh();

                } else {
                    Log.e(TAG, "momentsAroundYouCallBack Error, response -" + object);
                    // request failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "momentsAroundYouCallBack JsonException " + e);
            }
        }

        @Override
        public boolean isDestroyed() {
            return isDestroyed;
        }
    };

    private ArrayList<HomeMomentViewModel> writeMomentIdsToFireBase(JSONArray momentArray) {
        //clearing previous moments
        mFireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, MOMENTS_AROUND_YOU_LOCAL_REF}).setValue(null);

        ArrayList<HomeMomentViewModel> newList = new ArrayList<>();
        DatabaseReference fireBase = mFireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, MOMENTS_AROUND_YOU_LOCAL_REF});
        String momentId = null;
        for (int i = 0; i < momentArray.length(); i++) {
            try {
                JSONObject momentObject = momentArray.getJSONObject(i);
                momentId = momentObject.getString("_id");
                fireBase.child(momentId).child(NAME).setValue(momentObject.getString(NAME));
                fireBase.child(momentId).child(MOMENT_ID).setValue(momentId);
                if (momentObject.getBoolean(ISARTICLE) && AppLibrary.checkStringObject(momentObject.optString("articleThumbnail")) != null) {
                    fireBase.child(momentId).child(IMAGE_URL).setValue(momentObject.getString("articleThumbnail"));
                } else {
                    fireBase.child(momentId).child(IMAGE_URL).setValue(momentObject.getString("thumbnail"));
                }
                fireBase.child(momentId).child(HANDLE).setValue(momentObject.getString(HANDLE));

                HomeMomentViewModel model = new HomeMomentViewModel(null, momentObject.getString(NAME), momentObject.getString("thumbnail"), null, 0, momentId, false, FRIEND_ROOM, UNSEEN_MOMENT, null);
                model.handle = momentObject.getString(HANDLE);
                if (momentObject.getBoolean(ISARTICLE) && AppLibrary.checkStringObject(momentObject.optString("articleThumbnail")) != null) {
                    model.imageUrl = momentObject.getString("articleThumbnail");
                }

                try {
                    fireBase.child(momentId).child(DESCRIPTION).setValue(momentObject.getString(DESCRIPTION));
                    model.description = momentObject.getString(DESCRIPTION);
                } catch (JSONException e) {
                    Log.e(TAG, "description not provided by server for" + momentId);
                    e.printStackTrace();
                }

                try {
                    fireBase.child(momentId).child(TIMER).setValue(momentObject.getInt(TIMER));
                    model.fixedtimer = momentObject.getInt(TIMER);
                } catch (JSONException e) {
                    Log.e(TAG, "timer not provided by server for" + momentId);
                    fireBase.child(momentId).child(TIMER).setValue(0);
                    model.fixedtimer = 0;
                }

                try {
                    fireBase.child(momentId).child(ISARTICLE).setValue(momentObject.getBoolean(ISARTICLE));
                    model.isAnArticle = momentObject.getBoolean(ISARTICLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                try {
                    fireBase.child(momentId).child(PALLET_COLOR).setValue(momentObject.getJSONObject("pallete").getString("hex"));
                    model.palleteColor = momentObject.getJSONObject("pallete").getString("hex");
                } catch (JSONException ignored) {

                }

                try {
                    if (momentObject.getBoolean(CONTRIBUTABLE_ANYWHERE) && momentObject.getJSONObject(FLAGS).getBoolean("allowContribution")) {
                        fireBase.child(momentId).child(CONTRIBUTABLE_ANYWHERE).setValue(momentObject.getBoolean(CONTRIBUTABLE_ANYWHERE));
                        model.contributableNoLocation = momentObject.getBoolean(CONTRIBUTABLE_ANYWHERE);
                    }
                } catch (JSONException ignored) {

                }

                newList.add(model);
            } catch (Exception e) {
                e.printStackTrace();
                if (momentId != null)
                    fireBase.child(momentId).setValue(null);
            }
            momentId = null;
        }
        return newList;
    }

    private void getMomentIdsFromFireBase() {
        Log.d(TAG, "loadedMomentIdsFromFireBase");
        mFireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, MOMENTS_AROUND_YOU_LOCAL_REF}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange " + dataSnapshot);
                if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                    Log.e(TAG, "getMomentIdsFromFireBase failed, dataSnapshot null ");
                    rootView.setVisibility(View.GONE);
                    return;
                }
                HashMap<String, String> loadedMomentThumbnail = null;
                if (momentViewModels == null) momentViewModels = new ArrayList<>();
                else {
                    loadedMomentThumbnail = new HashMap<>();
                    for (HomeMomentViewModel model : momentViewModels) {
                        if (model.thumbnailUrl != null)
                            loadedMomentThumbnail.put(model.momentId, model.thumbnailUrl);
                    }
                    momentViewModels.clear();
                }


                if (articleMomentViewModels == null) {
                    articleMomentViewModels = new ArrayList<>();
                } else articleMomentViewModels.clear();

                HashSet<String> momentIdsSet = new HashSet<>();
                for (DataSnapshot momentSnap : dataSnapshot.getChildren()) {
                    HomeMomentViewModel model = new HomeMomentViewModel();
                    model.momentId = momentSnap.getKey();
                    momentIdsSet.add(model.momentId);
                    model.name = momentSnap.child("name").getValue(String.class);
                    model.imageUrl = momentSnap.child("imageUrl").getValue(String.class);
                    if (momentSnap.child("handle").exists())
                        model.handle = momentSnap.child("handle").getValue(String.class);
                    if (momentSnap.child(DESCRIPTION).exists())
                        model.description = momentSnap.child(DESCRIPTION).getValue(String.class);
                    if (momentSnap.child(TIMER).exists()) {
                        model.fixedtimer = 100; //ToDo - Temporary fix //FixMe
//                        model.fixedtimer = momentSnap.child(TIMER).getValue(Integer.class);
                    }
                    if (momentSnap.child(ISARTICLE).exists())
                        model.isAnArticle = momentSnap.child(ISARTICLE).getValue(Boolean.class);

                    if (momentSnap.child(PALLET_COLOR).exists())
                        model.palleteColor = momentSnap.child(PALLET_COLOR).getValue(String.class);

                    if (momentSnap.child(CONTRIBUTABLE_ANYWHERE).exists())
                        model.contributableNoLocation = momentSnap.child(CONTRIBUTABLE_ANYWHERE).getValue(Boolean.class);

                    model.momentStatus = UNSEEN_MOMENT;//unseen moment w.r.t. current session

                    if (model.isAnArticle)
                        articleMomentViewModels.add(model);
                    else {
                        if (loadedMomentThumbnail != null && loadedMomentThumbnail.containsKey(model.momentId)) {
                            model.showDisplayPicture = true;
                            model.thumbnailUrl = loadedMomentThumbnail.get(model.momentId);
                        }
                        momentViewModels.add(model);
                    }
                    downloader.registerNewStreamForDownload(generateMomentModelFromHomeMomentModelView(model), DynamicDownloader.MomentType.PUBLIC_MOMENT);
                }

                if (waitingMomentModel != null) {
                    addPublicMomentFromShare(waitingMomentModel, true);
                }
                downloader.setOnAroundYouDownloadListener(downloadListener);
                initRecyclerView();
                downloader.updateStatusForAllMomentsOnRefresh();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private HomeMomentViewModel waitingMomentModel;

    private void addPublicMomentFromShare(HomeMomentViewModel model, boolean openOnAdding) {
        HomeMomentViewModel watchNowClickedModel = null;

        boolean isArticle = model.isAnArticle;
        Log.d(TAG, " watch now clicked on " + model.name + " article  " + model.isAnArticle);
        ArrayList<HomeMomentViewModel> modelsList;
        RecyclerView recyclerViewTemp;
        if (isArticle) {
            modelsList = articleMomentViewModels;
            recyclerViewTemp = articleRecycler;
        } else {
            modelsList = momentViewModels;
            recyclerViewTemp = recyclerView;
        }

        if (modelsList != null) {
            boolean exists = false;
            for (HomeMomentViewModel momentViewModel : modelsList) {
                if (momentViewModel.momentId.equals(model.momentId)) {
                    exists = true;
                    watchNowClickedModel = momentViewModel;
                    break;
                }
            }
            if (!exists) {
                downloader.registerNewStreamForDownload(generateMomentModelFromHomeMomentModelView(model), DynamicDownloader.MomentType.PUBLIC_MOMENT);
                modelsList.add(0, model);
                if (openOnAdding)
                    onSingleTapOnMoment(model);
                if (recyclerViewTemp != null && recyclerViewTemp.getAdapter() != null)
                    recyclerViewTemp.getAdapter().notifyDataSetChanged();
            } else {//already exists but watch now clicked
                if (openOnAdding && watchNowClickedModel != null)
                    onSingleTapOnMoment(watchNowClickedModel);
            }
        } else waitingMomentModel = model;
    }

    boolean isAnArticle(String momentId) {
        if (articleMomentViewModels != null)
            for (HomeMomentViewModel model : articleMomentViewModels) {
                if (model.momentId.equals(momentId) && model.isAnArticle) {
                    return true;
                }
            }
        return false;
    }

    private DynamicDownloader downloader;
    private DynamicDownloader.OnMomentDownloadListener downloadListener = new DynamicDownloader.OnMomentDownloadListener() {
        @Override
        public void onMomentStateChangedForViews(final String momentId, final int newState) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<HomeMomentViewModel> modelsList;
                        HomeMomentAdapter adapter;
                        if (isAnArticle(momentId)) {
                            modelsList = articleMomentViewModels;
                            adapter = articleAdapter;
                        } else {
                            modelsList = momentViewModels;
                            adapter = homeMomentAdapter;
                        }

                        for (int i = 0; i < modelsList.size(); i++) {
                            if (modelsList.get(i).momentId.equals(momentId)) {
                                int previousState = modelsList.get(i).momentStatus;
                                Log.d(TAG, modelsList.get(i).name + " has state " + DynamicDownloader.getMomentStatusString(newState));
                                modelsList.get(i).momentStatus = newState;
                                if (adapter != null && previousState != newState) {
                                    adapter.safeNotifyItemChanged(i);
                                }
                                break;
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void openWaitingMoments(final String momentId) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((CameraActivity) getActivity()).loadViewPublicMomentFragment(momentId, READY_TO_VIEW_MOMENT, "public");
                }
            });
        }

        @Override
        public void onLastMediaLoaded(final MomentModel.Media mediaObj, final MomentModel momentModel) {
            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean isArticle = isAnArticle(momentModel.momentId);
                    int pos = 0;
                    if (!isArticle) {
                        final String momentId = momentModel.momentId;
                        if (AppLibrary.checkStringObject(mediaObj.thumbnail) == null) {
                            for (final HomeMomentViewModel model : momentViewModels) {
                                if (model.momentId != null && model.momentId.equals(momentId)) {
                                    pos = momentViewModels.indexOf(model);
                                    model.updatedAt = mediaObj.createdAt;
                                    model.updatedAtText = AppLibrary.timeAccCurrentTime(model.updatedAt);
                                    model.lastMediaId = mediaObj.mediaId;
                                    model.showDisplayPicture = true;
                                    homeMomentAdapter.safeNotifyItemChanged(pos);
                                    break;
                                }
                            }
                        } else {
                            for (final HomeMomentViewModel model : momentViewModels) {
                                if (model.momentId != null && model.momentId.equals(momentId)) {
                                    pos = momentViewModels.indexOf(model);
                                    model.updatedAt = mediaObj.createdAt;
                                    model.updatedAtText = AppLibrary.timeAccCurrentTime(model.updatedAt);
                                    model.lastMediaId = mediaObj.mediaId;
                                    model.thumbnailUrl = mediaObj.thumbnail;
                                    model.showDisplayPicture = true;
                                    AppLibrary.log_d(TAG, "Got the last media thumbnail for media Id -" + mediaObj.mediaId + ", momentId -" + momentId);
                                    homeMomentAdapter.safeNotifyItemChanged(pos);
                                    break;
                                }
                            }
                        }
                    } else {
                        final String momentId = momentModel.momentId;
                        for (final HomeMomentViewModel model : articleMomentViewModels) {
                            if (model.momentId != null && model.momentId.equals(momentId)) {
                                pos = articleMomentViewModels.indexOf(model);
                                model.showDisplayPicture = true;
                                break;
                            }
                        }
                    }
                    validateMomentsForShuffle(isArticle, pos);
                }
            });
        }

        @Override
        public void onThumbnailChanged(final MomentModel.Media mediaObj, final MomentModel momentModel) {
            if (getActivity() == null || isAnArticle(momentModel.momentId))
                return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final String momentId = momentModel.momentId;
                    for (final HomeMomentViewModel model : momentViewModels) {
                        if (model.momentId != null && model.momentId.equals(momentId)) {
                            int pos = momentViewModels.indexOf(model);
                            if ((model.lastMediaId == null || model.lastMediaId.equals(mediaObj.mediaId)) && (model.thumbnailUrl == null || !model.thumbnailUrl.equals(mediaObj.thumbnail))) {
                                model.thumbnailUrl = mediaObj.thumbnail;
                                model.showDisplayPicture = true;
                                AppLibrary.log_d(TAG, "onThumbnailChanged, thumbnail -" + mediaObj.thumbnail);
                                homeMomentAdapter.safeNotifyItemChanged(pos);
                            }
                            break;
                        }
                    }
                }
            });
        }

        @Override
        public void nullifyMomentPreviousThumbnail(MomentModel momentModel) {
            String momentId = momentModel.momentId;
            for (int i = 0;i < momentViewModels.size();i++) {
                HomeMomentViewModel model = momentViewModels.get(i);
                if (model.momentId != null && model.momentId.equals(momentId)) {
                    model.thumbnailUrl = null;
                    model.lastMediaId = null;
                    break;
                }
            }
        }
    };

    private void validateMomentsForShuffle(boolean isArticle, int itemPosition) {
        boolean shuffle = true;
        List<HomeMomentViewModel> list;
        if (isArticle)
            list = articleMomentViewModels;
        else
            list = momentViewModels;
        if (list == null || list.size() == 0)
            return;
        for (HomeMomentViewModel model : list) {
            if (!model.showDisplayPicture) {
                shuffle = false;
                break;
            }
        }

        if (shuffle) {
            shuffleUnseenMoments(isArticle, itemPosition);
            if (!isArticle && !aroundYouMomentsLoaded)
                aroundYouMomentsLoaded = true;
        }
    }

    private void shuffleUnseenMoments(boolean isArticle, int itemPosition) {
        if (isArticle) {
            List<HomeMomentViewModel> modelList = (List<HomeMomentViewModel>) articleMomentViewModels.clone();
            for (int i = 0; i < modelList.size(); i++) {
                HomeMomentViewModel momentViewModel = modelList.get(i);
                if (momentViewModel.momentStatus == UNSEEN_MOMENT) {
                    swapMomentToLeft(articleMomentViewModels, momentViewModel, articleAdapter, 0);
                }
            }
            if (articleRecycler != null) {
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!articleRecycler.isComputingLayout())
                            articleRecycler.smoothScrollToPosition(0);
                    }
                }, 1000);
            }
        } else {
            if (aroundYouMomentsLoaded) {
                swapMomentToLeft(momentViewModels, momentViewModels.get(itemPosition), homeMomentAdapter, 0);
            } else {
                List<HomeMomentViewModel> modelList = (List<HomeMomentViewModel>) momentViewModels.clone();
                for (int i = 0; i < modelList.size(); i++) {
                    HomeMomentViewModel momentViewModel = modelList.get(i);
                    if (momentViewModel.momentStatus == UNSEEN_MOMENT) {
                        if (mFireBaseHelper.getMyUserModel().miscellaneous != null
                                && mFireBaseHelper.getMyUserModel().miscellaneous.institutionData != null
                                && mFireBaseHelper.getMyUserModel().miscellaneous.institutionData.momentId != null
                                && mFireBaseHelper.getMyUserModel().miscellaneous.institutionData.momentId.equals(momentViewModel.momentId)) {
                            swapMomentToLeft(momentViewModels, momentViewModel, homeMomentAdapter, 0);
                        } else {
                            swapMomentToLeft(momentViewModels, momentViewModel, homeMomentAdapter, 1);
                        }
                    }
                }
                if (momentViewModels.get(0).momentStatus == SEEN_MOMENT) {
                    swapMomentToLeft(momentViewModels, momentViewModels.get(0), homeMomentAdapter, momentViewModels.size() - 1);
                }
            }
            if (recyclerView != null) {
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!recyclerView.isComputingLayout())
                            recyclerView.smoothScrollToPosition(0);
                    }
                }, 1000);
            }
        }
    }

    /**
     * We run loop from n to 0 instead of 0 to n as the list size is dynamic because of
     * remove while iteration ; hence n is variable
     * Wee maintain stack of all the seen moments and add it back after  iteration
     */
    public void refreshPositionsOfSeenMoments() {
        Log.d(TAG, "refreshPositionsOfSeenMoments");
        Stack<HomeMomentViewModel> removedMomentStack = new Stack<>();
        if (momentViewModels != null) {
            for (int i = momentViewModels.size() - 1; i >= 0; i--) {
                HomeMomentViewModel model = momentViewModels.get(i);
                if (model.momentStatus == SEEN_MOMENT) {
                    removedMomentStack.add(model);
                    momentViewModels.remove(model);
                }
            }

            while (removedMomentStack.size() != 0) {
                momentViewModels.add(removedMomentStack.pop());
            }

            if (homeMomentAdapter != null)
                homeMomentAdapter.safeNotifyDataSetChanged();
        }

        Stack<HomeMomentViewModel> removedArticleMomentStack = new Stack<>();
        if (articleMomentViewModels != null) {
            for (int i = articleMomentViewModels.size() - 1; i >= 0; i--) {
                HomeMomentViewModel model = articleMomentViewModels.get(i);
                if (model.momentStatus == SEEN_MOMENT) {
                    removedArticleMomentStack.add(model);
                    articleMomentViewModels.remove(model);
                }
            }

            while (removedArticleMomentStack.size() != 0) {
                articleMomentViewModels.add(removedArticleMomentStack.pop());
            }

            if (articleAdapter != null)
                articleAdapter.safeNotifyDataSetChanged();
        }
    }

    public void swapMomentToLeft(final ArrayList<HomeMomentViewModel> modelsList, final HomeMomentViewModel momentModel, final HomeMomentAdapter adapter, int position) {
        if (modelsList != null && momentModel != null && modelsList.size() > 0) {
            int removedIndex = modelsList.indexOf(momentModel);
            modelsList.remove(momentModel);
            if (adapter != null)
                adapter.notifyItemRemoved(removedIndex);
            try {
                modelsList.add(position, momentModel);
            } catch (Exception e) {
                e.printStackTrace();
                modelsList.add(0, momentModel);
                position = 0;
            }
            if (adapter != null)
                adapter.notifyItemInserted(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        if (googleApiClient != null && googleApiClient.isConnected()) {
                            requestStartLocationUpdate(); //Restart location update to get location
                        } else {
                            requestLocationUpdate = true;
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        showShortToastMessage("Please give location access to see streams around you");
                        break;
                    default:
                        break;
                }
                break;
        }
    }


    //on click of button
    private void requestStartLocationUpdate() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_LOCATION);
        } else {
            showSmartGpsDialog();
        }
    }

    private void showSmartGpsDialog() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000);
        locationRequest.setFastestInterval(3 * 1000);

        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            if (recyclerView != null)
                                recyclerView.setVisibility(View.GONE);
                            progressBar.setVisibility(View.VISIBLE);
                            locationButton.setVisibility(View.GONE);

                            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, AroundYouFragment.this);

                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        showGpsDialog(); //Fallback - Show custom dialog for location settings
                        break;
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (location.getAccuracy() < 100) {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                knownLocation = location;
                startLocationUpdate();
            } else {
                //Continue listening
            }
        } else {
            if (recyclerView != null)
                recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    private Location getLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            Log.d(TAG, " getLastLocation " + lastLocation);
            return lastLocation;
        } else {
            Log.e(TAG, " getLastLocation null");
            return null;
        }
    }

    private void showGpsDialog() {
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_enable_gps);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.findViewById(R.id.rootView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        Button enableBtn = (Button) dialog.findViewById(R.id.enableBTN),
                denyBtn = (Button) dialog.findViewById(R.id.denyBTN);
        enableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gpsOptionsIntent = new Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(gpsOptionsIntent, REQUEST_CHECK_SETTINGS);
                dialog.dismiss();
            }
        });
        denyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (googleApiClient != null && googleApiClient.isConnected()) {
                        requestStartLocationUpdate(); //Restart location update to get location
                    } else {
                        requestLocationUpdate = true;
                    }
                } else {
                    showShortToastMessage("Please give location access to see streams around you");
                }
                break;
        }
    }

    public interface ViewControlsCallback {
        void onLoadViewMomentFragment(String momentId, int momentStatus, String momentType);
    }

    RecyclerView articleRecycler;
    HomeMomentAdapter articleAdapter;

    private void initArticles() {
        articleRecycler = (RecyclerView) rootView.findViewById(R.id.articleRecycler);
        LockableLinearLayoutManager layoutManager = new LockableLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        articleRecycler.setLayoutManager(layoutManager);
        if (articleAdapter == null) {
            articleAdapter = new HomeMomentAdapter(myUserId, context, articleMomentViewModels, ARTICLE_RECYCLER, new RecyclerViewClickInterface() {
                @Override
                public void onItemClick(int extras, Object data) {
                    HomeMomentViewModel model = (HomeMomentViewModel) data;
                    if (model.clickType == HomeMomentViewModel.ClickType.SINGLE_TAP) {
                        onSingleTapOnMoment(model);
                    } else if (model.clickType == HomeMomentViewModel.ClickType.DOUBLE_TAP) {
                        Log.d(TAG, " got double click on " + model.momentId);
                    } else if (model.clickType == HomeMomentViewModel.ClickType.LONG_PRESS) {
                        showStreamPopup(getActivity(), model.momentId, model.name, model.imageUrl, model.handle, model.description, model.isAnArticle);
                    }
                }
            });
            articleAdapter.setRecyclerView(articleRecycler);
            articleRecycler.setAdapter(articleAdapter);

        } else {
            articleAdapter.setMomentModelArrayList(articleMomentViewModels);
        }
    }

    public class LockableLinearLayoutManager extends LinearLayoutManager {
        HomeMomentAdapter adapter;

        public LockableLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }


        @Override
        public boolean canScrollHorizontally() {
            return adapter == null || adapter.canScrollHorizontally();
        }
    }
}
