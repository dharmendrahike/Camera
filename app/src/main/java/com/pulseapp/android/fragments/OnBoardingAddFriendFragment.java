package com.pulseapp.android.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.OnBoardingActivity;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.FaceBookFriendsModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by deepankur on 6/23/16.
 */
public class OnBoardingAddFriendFragment extends BaseFragment {

    private RelativeLayout friendsPresent;
    private RelativeLayout noFriendsPresent;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView nextPageTv, skipTV;
    private String TAG = getClass().getSimpleName();
    public static final int MESSENGER_BUTTON = 111;
    CallbackManager callbackManager;
    ShareDialog shareDialog;
    final String SKIP = "Skip", ADD_ALL = "Add all", CLEAR_ALL = "Clear all";

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_onboarding_add_friends, container, false);

        if (savedInstanceState!=null) return rootView;

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        final ArrayList<FaceBookFriendsModel> models = new ArrayList<>();
        rootView.findViewById(R.id.facebookButtonLL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (AppLibrary.isPackageInstalled("com.facebook.orca", context))
                    startMessenger(true);
                else showShortToastMessage("Please install Facebook Messenger to invite friends");

                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.INVITE_FRIENDS_ONBOARDING);
//                startMessenger(true);
            }
        });

        rootView.findViewById(R.id.skip_no_friendsTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((OnBoardingActivity) getActivity()).onFriendsSelectionDone(null);
            }
        });
        friendsPresent = (RelativeLayout) rootView.findViewById(R.id.hasFriendsLayout);
        noFriendsPresent = (RelativeLayout) rootView.findViewById(R.id.noFriendsLayout);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        nextPageTv = (TextView) rootView.findViewById(R.id.nextPageTV);
        nextPageTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userIds="";
                for (FaceBookFriendsModel model : models) {
                    if (model != null && model.isChecked) {
                        mFireBaseHelper.sendFriendRequest(myUserId, model.userId);
                        userIds += model.userId + ",";
                    }
                }
                ((OnBoardingActivity) getActivity()).onFriendsSelectionDone(userIds);
            }
        });
        skipTV = (TextView) rootView.findViewById(R.id.skipTV);
        skipTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
/*                if (skipTV.getText().toString().equals(ADD_ALL)){
                    skipTV.setText(CLEAR_ALL);
                    for (FaceBookFriendsModel model : models) {
                        if (model != null)
                            model.isChecked = false;
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                    return;
                }
                if (skipTV.getText().toString().equals(CLEAR_ALL)){
                    skipTV.setText(SKIP);
                    for (FaceBookFriendsModel model : models) {
                        if (model != null)
                            model.isChecked = false;
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                    return;
                }*/

                boolean checkedItemFound = false;
                for (FaceBookFriendsModel model : models) {
                    if (model != null)
                        if (model.isChecked) {
                            checkedItemFound = true;
                            break;
                        }
                }
                if (checkedItemFound) {
                    for (FaceBookFriendsModel model : models) {
                        if (model != null)
                            model.isChecked = true;
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                } else {
                    ((OnBoardingActivity) getActivity()).onFriendsSelectionDone(null);
                }
            }
        });
        mFireBaseHelper.getFireBaseReference(ANCHOR_SOCIALS, new String[]{myUserId, FACEBOOK_FRIENDS}).keepSynced(true);
        mFireBaseHelper.getFireBaseReference(ANCHOR_SOCIALS, new String[]{myUserId, FACEBOOK_FRIENDS}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final boolean testing = false;
                if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                    noFriendsPresent.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    if (!testing)
                        return;
                }
                if (testing) {
                    for (int i = 0; i < 7; i++) {
                        FaceBookFriendsModel model = new FaceBookFriendsModel("" + i, "Abc", null, "handle", false);
                        models.add(model);
                    }
                } else {
                    for (DataSnapshot friendSnap : dataSnapshot.getChildren()) {
                        FaceBookFriendsModel model = new FaceBookFriendsModel(friendSnap.getKey(), friendSnap.child(NAME).getValue(String.class), friendSnap.child(IMAGE_URL).getValue(String.class), friendSnap.child(HANDLE).getValue(String.class), false);
                        models.add(model);
                    }
                }
                progressBar.setVisibility(View.GONE);

                if (models.size() == 0) {
                    noFriendsPresent.setVisibility(View.VISIBLE);
                } else {
                    initActionBar(rootView.findViewById(R.id.includedHeader));
                    friendsPresent.setVisibility(View.VISIBLE);
                    models.add(null);//adding footer view
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.setAdapter(new OnBoardingAddFriendsAdapter(context, models, new RecyclerViewClickInterface() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onItemClick(int extras, Object data) {
                            if (extras == MESSENGER_BUTTON) {
                                if (AppLibrary.isPackageInstalled("com.facebook.orca", context))
                                    startMessenger(false);
                                else showShortToastMessage("Please install Facebook Messenger to invite friends");

                                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.INVITE_FRIENDS_ONBOARDING);

                                return;
                            }

                            boolean checkedFound = false;
                            for (FaceBookFriendsModel model : models)
                                if (model != null && model.isChecked) {
                                    checkedFound = true;
                                    break;
                                }
                            if (!checkedFound) {
                                nextPageTv.setVisibility(View.GONE);
                                skipTV.setText(SKIP);
                            } else {
                                nextPageTv.setVisibility(View.VISIBLE);
                                skipTV.setText(ADD_ALL);
                            }
                        }
                    }));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        ((TextView) rootView.findViewById(R.id.skip_no_friendsTV)).setTypeface(fontPicker.getMontserratRegular());
        ((TextView) rootView.findViewById(R.id.facebookBTN)).setTypeface(fontPicker.getMontserratSemiBold());
        ((TextView) rootView.findViewById(R.id.popularTV)).setTypeface(fontPicker.getMontserratRegular());
        ((TextView) rootView.findViewById(R.id.inviteFriendsTV)).setTypeface(fontPicker.getMuseo700());


        ((TextView) rootView.findViewById(R.id.suggestedFriendTV)).setTypeface(fontPicker.getMontserratRegular());
        ((TextView) rootView.findViewById(R.id.skipTV)).setTypeface(fontPicker.getMontserratRegular());
        return rootView;
    }

    private void initActionBar(View actionBar) {
        actionBar.findViewById(R.id.action_bar_IV_1).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        ((TextView) actionBar.findViewById(R.id.titleTV)).setText("CHOOSE YOUR FRIENDS");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        ((TextView) actionBar.findViewById(R.id.titleTV)).setTextColor(Color.WHITE);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        Log.d(TAG, "initActionBar " + AppLibrary.getStatusBarHeight(getActivity()));
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
    }

    void initContent() {
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse("https://developers.facebook.com"))
                .build();
    }

    public void startMessenger(final boolean autoForward) {

        MessageDialog dialog = new MessageDialog(getActivity());
        dialog.registerCallback(MasterClass.callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                if (autoForward)
                    ((OnBoardingActivity) getActivity()).onFriendsSelectionDone(null);
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {
                e.printStackTrace();
            }
        });

        if (MessageDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setContentTitle("Add Me on Pulse")
                    .setContentDescription("Pulse - Live in the moment")
                    .setContentUrl(Uri.parse("https://mypulse.tv/add/" + FireBaseHelper.getInstance(getActivity()).getMyUserModel().handle))
                    .build();
            dialog.show(linkContent);
        }
    }
}

class OnBoardingAddFriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<FaceBookFriendsModel> models;
    private RecyclerViewClickInterface recyclerViewClickInterface;
    private final int VIEW_TYPE_NORMAL = 11, VIEW_TYPE_FOOTER = 22;
    private final String TAG = getClass().getSimpleName();
    private FontPicker fontPicker;

    public OnBoardingAddFriendsAdapter(Context context, ArrayList<FaceBookFriendsModel> models, RecyclerViewClickInterface recyclerViewClickInterface) {
        this.context = context;
        this.models = models;
        this.recyclerViewClickInterface = recyclerViewClickInterface;
        fontPicker = FontPicker.getInstance(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == VIEW_TYPE_NORMAL)
            return new VHItem(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_suggested_friend_request, parent, false));
        if (viewType == VIEW_TYPE_FOOTER)
            return new VHFooter(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_start_messenger, parent, false));

        throw new RuntimeException("there is no type that matches the type " + viewType + "  wtf ");
    }

    @Override
    public int getItemViewType(int position) {
        return models.get(position) == null ? VIEW_TYPE_FOOTER : VIEW_TYPE_NORMAL;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.d(TAG, "position " + position);
        if (holder instanceof VHItem) {
            Picasso.with(context).load(models.get(position).imageUrl).transform(new RoundedTransformation()).into(((VHItem) holder).profileIv);
            ((VHItem) holder).nameTv.setText(models.get(position).name);
            ((VHItem) holder).handleTv.setText("@" + models.get(position).handle);
            ((VHItem) holder).rootView.setTag(position);
            ((VHItem) holder).friendSelectedIv.setImageResource(models.get(position).isChecked ? R.drawable.selected2_svg : R.drawable.add_svg);
        }
    }

    @Override
    public int getItemCount() {
        return models.size();
    }


    class VHItem extends RecyclerView.ViewHolder {
        ImageView profileIv, friendSelectedIv;
        TextView nameTv, handleTv;
        View rootView;

        public VHItem(View itemView) {
            super(itemView);
            rootView = itemView;
            profileIv = (ImageView) itemView.findViewById(R.id.profilePicIV);
            nameTv = (TextView) itemView.findViewById(R.id.cardNameTV);
            handleTv = (TextView) itemView.findViewById(R.id.cardHandleTV);
            friendSelectedIv = (ImageView) itemView.findViewById(R.id.friendSelectedIV);

            nameTv.setTypeface(fontPicker.getMontserratRegular());
            handleTv.setTypeface(fontPicker.getMontserratRegular());
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (int) rootView.getTag();
                    models.get(position).isChecked = !models.get(position).isChecked;
                    notifyItemChanged(position);
                    recyclerViewClickInterface.onItemClick(0, null);
                }
            });
        }
    }

    class VHFooter extends RecyclerView.ViewHolder {
        View rootView;
        TextView inviteFriendTV;

        public VHFooter(View itemView) {
            super(itemView);
            rootView = itemView;
            inviteFriendTV = (TextView) itemView.findViewById(R.id.inviteFriendTV);
            inviteFriendTV.setTypeface(fontPicker.getMontserratRegular());
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recyclerViewClickInterface.onItemClick(OnBoardingAddFriendFragment.MESSENGER_BUTTON, null);
                }
            });
        }
    }

}
