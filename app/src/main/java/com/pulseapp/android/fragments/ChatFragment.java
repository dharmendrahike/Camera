package com.pulseapp.android.fragments;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.ChatAdapter;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.customViews.CustomFrameLayout;
import com.pulseapp.android.customViews.LetterTileRoundedTransformation;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.ChatMediaModel;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.OnSwipeTouchListener;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by deepankur on 14/4/16.
 */
public class ChatFragment extends BaseFragment implements ChatAdapter.ChatViewController,
        FireBaseHelper.onUploadStatusChanges {

    private final String TAG = this.getClass().getSimpleName();
    private RecyclerView mRecyclerView;
    private ImageView chatImageIv;
    private TextView chatNameTv;
    private String roomId;
    private EditText messageEditText;
    private ChatAdapter chatAdapter;
    private RelativeLayout chatRl;
    private ImageView tintLayout;
    private String displayName, displayImageUrl;
    private int roomType;
    @Nullable //in  case of group room friend id would be null
    private String friendId;
    private LinkedHashMap<String, RoomsModel.Messages> unseenDownloadedChatMediaMap = new LinkedHashMap<>();
    private ViewControlsCallback viewControlsCallback;
    public LinkedHashMap<String, RoomsModel.Messages> messageLinkedMap = new LinkedHashMap<>();
    DatabaseReference chatFireBase;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {
        AppLibrary.log_d(TAG, "onEvent signal received");
        if (eventSignal instanceof BroadCastSignals.DownloadChatMediaStatus) {
            final BroadCastSignals.DownloadChatMediaStatus downloadChatMediaStatus = (BroadCastSignals.DownloadChatMediaStatus) eventSignal;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<String,RoomsModel.Messages> entry : messageLinkedMap.entrySet()){
                        if (entry.getValue().mediaId.equals(downloadChatMediaStatus.mediaId)){
                            AppLibrary.log_d(TAG, "changing download status of media with mediaId " + downloadChatMediaStatus.mediaId);
                            RoomsModel.Messages message = messageLinkedMap.get(entry.getKey());
                            message.mediaStatus = MEDIA_DOWNLOAD_COMPLETE;
                            message.uri = downloadChatMediaStatus.localUrl;
                            if (chatAdapter != null) {
                                AppLibrary.log_d(TAG, "notifying adapter for download status changed");
                                chatAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            });
        }
    }

    public ChatFragment() {
        super.registerForInAppSignals(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGesture();
//        mFireBaseHelper.setMessageCallbackListener(this);
        mFireBaseHelper.setOnUploadStatusChangesCallback(this);
        SliderMessageModel sliderMessageModel = CameraActivity.currentRoomBeingOpened;
        this.isInDeletionMode = false;
        roomId = sliderMessageModel.roomId;
        roomType = sliderMessageModel.roomType;
        displayName = sliderMessageModel.displayName;
        displayImageUrl = sliderMessageModel.imageUrl;
        if (roomId == null) throw new RuntimeException("Cannot open this Fragment without room Id");
        friendId = sliderMessageModel.friendId;
        chatAdapter = new ChatAdapter(sliderMessageModel.roomType,this, isInDeletionMode, getActivity(), messageLinkedMap, recyclerViewClickInterface, unseenDownloadedChatMediaMap);//initially, no messages in the room
        chatAdapter.setChatBoxDisplayName(displayName);
        chatFireBase = mFireBaseHelper.getFireBaseReference(ANCHOR_ROOMS, new String[]{roomId, MESSAGES});
        chatFireBase.addChildEventListener(chatListener);
        mFireBaseHelper.setChatMediaStatusChangeListener(new FireBaseHelper.ChatMediaStatusChangeListener() {
            @Override
            public void onMediaChanged() {
                if (chatAdapter != null)
                    chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void deleteMediaMessage(String messageId) {
                if (messageLinkedMap.containsKey(messageId)) {
                    messageLinkedMap.remove(messageId);
                    mFireBaseHelper.removeMessage(roomId, messageId);
                }
                if (chatAdapter != null)
                    chatAdapter.notifyDataSetChanged();
            }
        });
        viewControlsCallback.onRoomOpen(roomId);
    }

    CustomFrameLayout customFrameLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        final View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        customFrameLayout = (CustomFrameLayout) rootView.findViewById(R.id.customFrame);
        initActionBar(rootView.findViewById(R.id.action_bar));
        initViewObjects(rootView);
        mFireBaseHelper.roomOpen(mFireBaseHelper.getMyUserId(), roomId, roomType);
        messageEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mRecyclerView != null && messageLinkedMap != null)
                    mRecyclerView.scrollToPosition(messageLinkedMap.size() - 1);
            }
        });
        final ImageView sendMessageIv = (ImageView) rootView.findViewById(R.id.send_messageIV);
        sendMessageIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageEditText.getText().toString();
                if (messageText.trim().length() == 0) {
                    messageEditText.setText("");
                    return;
                }


                RoomsModel.Messages message = new RoomsModel.Messages(myUserId, null, MESSAGE_TYPE_TEXT,
                        messageEditText.getText().toString(), System.currentTimeMillis(), null, 2, null);
                if (roomType == FRIEND_ROOM)
                    mFireBaseHelper.sendMessageToFriend(myUserId, friendId, roomId, message, NEW_TEXT, null, null);
                if (roomType == GROUP_ROOM)
                    mFireBaseHelper.sendMessageToGroup(myUserId, roomId, message, NEW_TEXT, null, null);

                messageEditText.setText("");
            }
        });
        initRecyclerView(rootView);


        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            //not changing drawable everyTime ; only when editText length toggles bwn 0 and 1
            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, " changing text , new size " + s.length());

//                boolean isEmpty = s.length() < 1;
//                if (isEmpty != isEditTextEmpty)
//                    sendMessageIv.setImageResource(isEmpty ?
//                            android.R.drawable.ic_menu_gallery : android.R.drawable.btn_plus);
//                isEditTextEmpty = isEmpty;
            }
        });
        customFrameLayout.setInterceptTouchListener(new CustomFrameLayout.InterceptTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                Log.d(TAG, " customFrameLayout intercept " + event);
                swipeDetectorView.dispatchTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //reset everything
                        currentScrollDirection = UNDETERMINED;
                        notifyDeletionAllowed();
                        numberOfMoves = 0;
                        swipeDetected = false;
                        firstDownPoints[0] = event.getRawX();
                        firstDownPoints[1] = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (currentScrollDirection == UNDETERMINED)
                            determineDirection(event);
                        if (currentScrollDirection == HORIZONTAL)
                            translate(event);
                        break;
                    case MotionEvent.ACTION_UP:
                        currentScrollDirection = UNDETERMINED;
                        notifyDeletionAllowed();
                        Log.d(TAG, "current direction " + currentScrollDirection + "swipeDetected " + swipeDetected);
                        if (!swipeDetected)
                            settleDownTheView();
                        break;
                }
                return false;
            }
        });

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return currentScrollDirection == HORIZONTAL;
            }
        });
        customFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //currentScrollDirection = Horizontal now
                Log.d(TAG, " customFrameLayout onTouch " + event);
                return true;
            }
        });
        return rootView;
    }

    float[] firstDownPoints = new float[2];//0 for x; 1 for y


    public void determineDirection(MotionEvent event) {
        Log.d(TAG, " determineDirection newX " + event.getRawX() + " newY " + event.getRawY());
        numberOfMoves++;
        if (currentScrollDirection == UNDETERMINED && numberOfMoves >= 3) {
            float deltaX = Math.abs(firstDownPoints[0] - event.getRawX());
            float deltaY = Math.abs(firstDownPoints[1] - event.getRawY());
            currentScrollDirection = deltaX > deltaY ? HORIZONTAL : VERTICAL;
            if (deltaX > deltaY) {
                currentScrollDirection = HORIZONTAL;
                deltaFirstTouch = event.getX();
                toggleSoftKeyboard(context, messageEditText, false);
                messageEditText.clearFocus();
                Log.d(TAG, " assigning HORIZONTAL direction ");
                notifyDeletionAllowed();
            } else if (deltaX < deltaY) {
                Log.d(TAG, " assigning VERTICAL direction ");
                currentScrollDirection = VERTICAL;
                notifyDeletionAllowed();
            } else {//deltaX equals deltaY
                Log.d(TAG, "unable to assign direction ");
                currentScrollDirection = UNDETERMINED;
            }
        }
    }

    private boolean isEditTextEmpty = true;
    private boolean swipeDetected;

    private void initViewObjects(View rootView) {
        tintLayout = (ImageView) rootView.findViewById(R.id.tintFrame);
        chatRl = (RelativeLayout) rootView.findViewById(R.id.chatRL);

        messageEditText = (EditText) rootView.findViewById(R.id.messageEditText);
        messageEditText.setTypeface(fontPicker.getMontserratRegular());
        chatNameTv.setText(displayName);
        if (this.roomType == FRIEND_ROOM)
            Picasso.with(getActivity()).load(displayImageUrl).
                    transform(new RoundedTransformation()).into(chatImageIv);
        else {
            Transformation t = new LetterTileRoundedTransformation(context, this.displayName);
            Picasso.with(context).load(R.drawable.transparent_image).
                    transform(t).into(chatImageIv);
        }
    }

    private ImageView backIv, deleteIv;

    void initActionBar(View actionBar) {
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        deleteIv = (ImageView) actionBar.findViewById(R.id.action_bar_IV_3);
        ((RelativeLayout.LayoutParams) deleteIv.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_END);
        deleteIv.setVisibility(View.GONE);
        deleteIv.setImageResource(R.drawable.delete_svg);
        deleteIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final HashMap<String, Boolean> deletionListMap = chatAdapter.getDeletionListMap();
                for (Map.Entry<String, Boolean> entry : deletionListMap.entrySet())
                    if (entry.getValue())
                        mFireBaseHelper.getFireBaseReference(ANCHOR_ROOMS, new String[]{roomId, MESSAGES, entry.getKey()}).setValue(null);
                toggleDeletionMode();

            }
        });

        chatImageIv = (ImageView) actionBar.findViewById(R.id.action_bar_IV_2);
        chatImageIv.setPadding(0, 0, 0, 0);
        ((RelativeLayout.LayoutParams) chatImageIv.getLayoutParams()).addRule(RelativeLayout.CENTER_VERTICAL);
        int diameter = (int) getActivity().getResources().getDimension(R.dimen.profile_pic_diameter);
        Log.d(TAG, " hard setting profile pic dimens to " + diameter);
        chatImageIv.getLayoutParams().height = diameter;
        chatImageIv.getLayoutParams().width = diameter;
        ((RelativeLayout.LayoutParams) chatImageIv.getLayoutParams()).rightMargin = AppLibrary.convertDpToPixels(context, 16);

        backIv = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        backIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInDeletionMode)
                    popFragmentFromBackStack();
                else toggleDeletionMode();
            }
        });
        backIv.setImageResource(R.drawable.back_svg);

        chatNameTv = ((TextView) actionBar.findViewById(R.id.titleTV));
        culpritView = actionBar.findViewById(R.id.status_bar_background);
        culpritView.getLayoutParams().height = 0 /** AppLibrary.getStatusBarHeight(getActivity())*/;
//        culpritView.requestLayout();

    }

    /**
     * @return true if want on consue backPress ;
     * false otherWise
     */
    public boolean onBackPressed() {
        if (isInDeletionMode) {
            toggleDeletionMode();
            Log.d(TAG, " exiting deletion mode on backPress");
            return true;
        }
        return false;
    }

    View culpritView;
    int numberOfMessagesUnderDeletion;
    private RecyclerViewClickInterface recyclerViewClickInterface = new RecyclerViewClickInterface() {
        @Override
        public void onItemClick(int extras, Object data) {
            numberOfMessagesUnderDeletion = extras;
            if (numberOfMessagesUnderDeletion > 0 && !isInDeletionMode)
                toggleDeletionMode();
            if (numberOfMessagesUnderDeletion == 0 && isInDeletionMode)
                toggleDeletionMode();
        }
    };

    private Boolean isInDeletionMode;

    private void toggleDeletionMode() {
        isInDeletionMode = !isInDeletionMode;
//        chatImageIv.setVisibility(isInDeletionMode ? View.GONE : View.VISIBLE);
        deleteIv.setVisibility(isInDeletionMode ? View.VISIBLE : View.GONE);
        chatNameTv.setVisibility(isInDeletionMode ? View.GONE : View.VISIBLE);
        if (!isInDeletionMode)
            chatAdapter.clearDeletionList();

    }

    private void initRecyclerView(View rootView) {
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.chat_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ViewTreeObserver viewTreeObserver = mRecyclerView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    displayWidth = mRecyclerView.getMeasuredWidth();

                }
            });
        }
        mRecyclerView.setAdapter(chatAdapter);
        mRecyclerView.addOnScrollListener(scrollListener);
    }


    private float deltaFirstTouch;
    private int numberOfMoves;

    /**
     * tell the chat adapter whether deletion can be done or not .
     */
    private void notifyDeletionAllowed() {
//       Log.d(TAG, buildStackTraceString(Thread.currentThread().getStackTrace()));
        if (chatAdapter != null) {
            chatAdapter.setDeletionAllowed(this.currentScrollDirection != HORIZONTAL);
        }
    }

    private final int HORIZONTAL = 1111, VERTICAL = 2222, UNDETERMINED = 0;//0 is the default
    private int currentScrollDirection = UNDETERMINED;

    private void translate(MotionEvent event) {
//        Log.d(TAG, " translate: " + chatRl.getX());
        float displacement = event.getRawX() - deltaFirstTouch;
        if (displacement > 0)
            chatRl.setTranslationX(displacement);
        else
            chatRl.setTranslationX(0);

        resetAlpha(displacement);
    }

    private void resetAlpha(float displacement) {
        float currentFactor = (1 - (displacement / displayWidth));

        if (currentFactor >= 0.99f)
            tintLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else if (currentFactor == 0f)
            tintLayout.setLayerType(View.LAYER_TYPE_NONE, null);

        tintLayout.setAlpha((currentFactor));
    }

    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            //Log.d(TAG, " recycler Scroll " + dx + " " + dy);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) context;
    }

    private ObjectAnimator dismissAnimator, settleAnimator;
    int displayWidth;
    final int MAXIMUM_FRAGMENT_DISMISS_DURATION = 150;
    boolean inAnimation;

    private void settleDownTheView() {
        Log.d(TAG, "settling down the view");
//        if (settleAnimator == null) {
        settleAnimator = ObjectAnimator.ofFloat(chatRl, "translationX", 0);
        settleAnimator.removeAllListeners();
        settleAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                inAnimation = true;
                chatRl.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                tintLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                inAnimation = false;
                chatRl.setLayerType(View.LAYER_TYPE_NONE, null);
                tintLayout.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                inAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
//        }

//        float effectiveDuration = (chatRl.getTranslationX() / displayWidth) * MAXIMUM_FRAGMENT_DISMISS_DURATION;
        settleAnimator.setDuration((long) MAXIMUM_FRAGMENT_DISMISS_DURATION);
        settleAnimator.setFloatValues(chatRl.getTranslationX(), 0);
        settleAnimator.start();
    }

    private void dismissChatFragment() {
//        if (dismissAnimator == null) {
        dismissAnimator = ObjectAnimator.ofFloat(chatRl, "translationX", /*chatRl.getTranslationX(),*/ displayWidth);
        dismissAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                chatRl.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                tintLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                inAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                chatRl.setLayerType(View.LAYER_TYPE_NONE, null);
                tintLayout.setLayerType(View.LAYER_TYPE_NONE, null);

//                getActivity().onBackPressed();
                popFragmentFromBackStack();
                inAnimation = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                inAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
//        }
//        float dismissEndPoint = currentDisplacement > 0 ? displayWidth : -displayWidth;
//        dismissAnimator.setFloatValues(currentDisplacement < 0 ? chatRl.getTranslationX() : dismissEndPoint);
//
//        float effectiveDuration = (chatRl.getTranslationX() / displayWidth) * MAXIMUM_FRAGMENT_DISMISS_DURATION;
//        dismissAnimator.setDuration((long) effectiveDuration);
        dismissAnimator.setDuration(MAXIMUM_FRAGMENT_DISMISS_DURATION);
        dismissAnimator.setFloatValues(displayWidth);
        dismissAnimator.start();

        ObjectAnimator.ofFloat(tintLayout, "alpha", 0)
                .setDuration(MAXIMUM_FRAGMENT_DISMISS_DURATION)
                .start();
    }

    @Override
    public void onDestroyView() {
        context.setTheme(R.style.AppTheme);
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        chatFireBase.removeEventListener(chatListener);
        CameraActivity.currentRoomBeingOpened = null;//manually nullify static objects
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private ChildEventListener chatListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if (dataSnapshot == null || dataSnapshot.getValue() == null) return;
            RoomsModel.Messages message = dataSnapshot.getValue(RoomsModel.Messages.class);
            String messageKey = dataSnapshot.getKey();

            message.messageId = messageKey;
            if (message.type == MESSAGE_TYPE_MEDIA && message.expiryType == REMOVED_UPON_ROOM_OPEN) {
                mFireBaseHelper.removeMessage(roomId, message.messageId);
            } else {
                if (message.type == MESSAGE_TYPE_MEDIA && message.expiryType != VIEW_ONCE_AND_VIEWED) {
                    if (message.mediaUploadingStatus != MEDIA_UPLOADING_COMPLETE) {
                        if (message.memberId.equals(mFireBaseHelper.getMyUserId())) {
                            // retry upload option , status upload failed
                            messageLinkedMap.put(messageKey, message);
                            mFireBaseHelper.checkChatMediaDownloadStatus(messageLinkedMap.get(messageKey));
                        }
                    } else {
                        // uploaded but not downloaded
                        if (!message.memberId.equals(mFireBaseHelper.getMyUserId()) && (message.viewers == null || !message.viewers.containsKey(mFireBaseHelper.getMyUserId()))){
                            unseenDownloadedChatMediaMap.put(message.mediaId,null);
                        }
                        messageLinkedMap.put(messageKey, message);
                        mFireBaseHelper.checkChatMediaDownloadStatus(messageLinkedMap.get(messageKey));
                    }
                } else if (message.type == MESSAGE_TYPE_MEDIA){
                    messageLinkedMap.put(messageKey, message);
                } else if (message.type == MESSAGE_TYPE_TEXT) {
                    if (!myUserId.equals(message.memberId) && message.expiryType == REMOVED_UPON_ROOM_OPEN) {
                        mFireBaseHelper.removeMessage(roomId, messageKey);
                    } else {
                        mFireBaseHelper.getFireBaseReference(ANCHOR_ROOMS, new String[]{roomId, MESSAGES, messageKey, VIEWERS, myUserId}).setValue(1);
                        if (message.expiryType == MESSAGE_EXPIRED_BUT_NOT_VIEWED) {
                            message.expiryType = REMOVED_UPON_ROOM_OPEN;
                            mFireBaseHelper.getFireBaseReference(ANCHOR_ROOMS, new String[]{roomId, MESSAGES, messageKey, EXPIRY_TYPE}).setValue(REMOVED_UPON_ROOM_OPEN);
                        }
                        messageLinkedMap.put(messageKey, message);
                    }
                }

                chatAdapter.notifyDataSetChanged();
                chatAdapter.notifyItemInserted(messageLinkedMap.size() - 1);
                mRecyclerView.scrollToPosition(messageLinkedMap.size() - 1);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            RoomsModel.Messages message = dataSnapshot.getValue(RoomsModel.Messages.class);
            // handles the case of the other user opens the view once media and the change is to be reflected to the sender
            if (message.expiryType == VIEW_ONCE_AND_VIEWED) {
                message.messageId = dataSnapshot.getKey();
                messageLinkedMap.put(dataSnapshot.getKey(), message);
                chatAdapter.notifyDataSetChanged();
                chatAdapter.notifyItemInserted(messageLinkedMap.size() - 1);
                mRecyclerView.scrollToPosition(messageLinkedMap.size() - 1);
            } else {
                // handle the case when the user open the room and some one sent a media and changes
                // is to reflected real time on the other user side after the media uploaded successfully
                if (!messageLinkedMap.containsKey(dataSnapshot.getKey()) && message.type == MESSAGE_TYPE_MEDIA && message.mediaUploadingStatus == MEDIA_UPLOADING_COMPLETE && !message.memberId.equals(mFireBaseHelper.getMyUserId())) {
                    message.messageId = dataSnapshot.getKey();
                    message.mediaStatus = MEDIA_DOWNLOADING;
                    messageLinkedMap.put(message.messageId,message);
                    mFireBaseHelper.checkChatMediaDownloadStatus(messageLinkedMap.get(message.messageId));
                    chatAdapter.notifyDataSetChanged();
                    chatAdapter.notifyItemInserted(messageLinkedMap.size() - 1);
                    mRecyclerView.scrollToPosition(messageLinkedMap.size() - 1);
                }
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            messageLinkedMap.remove(dataSnapshot.getKey());
            chatAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

    };

    @Override
    public void onMediaClicked(RoomsModel.Messages message) {
        viewControlsCallback.onLoadViewChatMediaFragment(roomId, roomType, message.mediaId,message.messageId);
    }

    public LinkedHashMap<String, RoomsModel.Messages> getDownloadedChatMediaMap(String mediaId,String messageId) {
        LinkedHashMap<String, RoomsModel.Messages> localDownloadedMediaMap = new LinkedHashMap<>();
        if (unseenDownloadedChatMediaMap != null && unseenDownloadedChatMediaMap.size() > 0 && unseenDownloadedChatMediaMap.containsKey(messageId)) {
            int counter = 0;
            int foundIndex = -1;
            for (Map.Entry<String, RoomsModel.Messages> entry : unseenDownloadedChatMediaMap.entrySet()) {
                if (entry.getKey().equals(messageId) && entry.getValue() != null && entry.getValue().mediaStatus >= MEDIA_DOWNLOAD_COMPLETE) {
                    localDownloadedMediaMap.put(entry.getKey(), entry.getValue());
                    foundIndex = counter;
                } else if (foundIndex != -1 && entry.getValue() != null && entry.getValue().mediaStatus >= MEDIA_DOWNLOAD_COMPLETE) {
                    localDownloadedMediaMap.put(entry.getKey(), entry.getValue());
                }
                counter++;
            }
            counter = 0;
            for (Map.Entry<String, RoomsModel.Messages> entry : unseenDownloadedChatMediaMap.entrySet()) {
                if (counter < foundIndex && entry.getValue() != null && entry.getValue().mediaStatus >= MEDIA_DOWNLOAD_COMPLETE) {
                    localDownloadedMediaMap.put(entry.getKey(), entry.getValue());
                }
                counter++;
            }
        } else {
            localDownloadedMediaMap.put(messageId, messageLinkedMap.get(messageId));
        }
        return localDownloadedMediaMap;
    }

    public void onOpenMedia(String messageId,String mediaId,int position) {
        if (unseenDownloadedChatMediaMap != null && unseenDownloadedChatMediaMap.containsKey(messageId)) {
            RoomsModel.Messages message = unseenDownloadedChatMediaMap.get(messageId);
            if (message.expiryType == VIEW_ONCE || message.expiryType == QUICK_PEEK) {
                message.expiryType = VIEW_ONCE_AND_VIEWED;
                messageLinkedMap.get(message.messageId).expiryType = VIEW_ONCE_AND_VIEWED;
            }
            if (messageLinkedMap.get(messageId).viewers == null)
                messageLinkedMap.get(messageId).viewers = new HashMap<>();

            messageLinkedMap.get(messageId).viewers.put(mFireBaseHelper.getMyUserId(),1);
            unseenDownloadedChatMediaMap.remove(messageId);
            chatAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onUploadingStatusModified(String messageId, int status) {
        if (messageLinkedMap != null && messageLinkedMap.containsKey(messageId)) {
            messageLinkedMap.get(messageId).mediaUploadingStatus = status;
            chatAdapter.notifyDataSetChanged();
        }
    }

    private View swipeDetectorView;

    private void initGesture() {
        swipeDetectorView = new View(context);
        swipeDetectorView.setOnTouchListener(new OnSwipeTouchListener(context) {

            public void onSwipeRight() {
                swipeDetected = true;
                Log.d(TAG, "onSwipeRight currentScrollDirection: " + currentScrollDirection);
                if (currentScrollDirection == HORIZONTAL) {
                    notifyDeletionAllowed();
                    Log.d(TAG, " on SwipeRight dismissing with current X as : " + chatRl.getX());
                    dismissChatFragment();
                }
            }
        });
    }

    public interface ViewControlsCallback {
        void onLoadViewChatMediaFragment(String roomId, int roomType, String mediaId,String messageId);

        void onRoomOpen(String roomId);
    }

    void popFragmentFromBackStack() {
        if (getFragmentInBackStack(ChatFragment.class.getSimpleName()) != null) {
            boolean popped = getActivity().getSupportFragmentManager().popBackStackImmediate();
            Log.d(TAG, "popFragmentFromBackStack: popped " + popped);
        } else Log.d(TAG, "popFragmentFromBackStack: fragment is null");
    }

    /**
     * @param fragmentTag the String supplied while fragment transaction
     * @return null if not found
     */
    private Fragment getFragmentInBackStack(String fragmentTag) {
        return getActivity().getSupportFragmentManager().findFragmentByTag(fragmentTag);
    }

}
