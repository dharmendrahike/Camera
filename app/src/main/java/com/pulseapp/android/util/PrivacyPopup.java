package com.pulseapp.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.customViews.CustomScrollView;
import com.pulseapp.android.customViews.MaxHeightLinearLayout;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.PrivacyPolicy;
import com.pulseapp.android.models.SettingsModel;

import java.util.Map;

/**
 * Created by user on 5/10/2016.
 */
public class PrivacyPopup extends FrameLayout {

    private RadioButton radioButton;
    private ImageView updateImageView;
    //    private ViewControlsCallback viewControlsCallback;
    private View itemRowView;
    private String id;
    private TextView descTextView;
    public final static short ALL_FRIEND_ROW = 1, FRIENDS_EXCEPT_ROW = 2,
            CREATE_NEW_LIST_ROW = 44, EXISTING_CUSTOM_LIST_SELECTED_ROW = 3,
            ALL_FRIENDS_AND_FOLLOWERS = 4;
    static int customListsPresent;
    private SharedPreferences preferences;

    public String getLastUsedPrivacy() {
        return lastUsedPrivacy;
    }

    private String lastUsedPrivacy;

    public PrivacyPopup(Context context) {
        super(context);
        init(context);
    }

    public PrivacyPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PrivacyPopup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public RowData getCurrentSelectionTag() {
        for (int i = 0; i < listLayout.getChildCount(); i++) {
            Object tag = listLayout.getChildAt(i).getTag();
            if (tag == null)
                continue;
            RowData rowData = (RowData) tag;
            if (rowData.isChecked) {
                return rowData;
            }
        }
        return null;
//        throw new RuntimeException("none selected");
    }

    public interface PopupListener {

        void onUpdatePrivacyTextView(String text, int resourceId);

        void onOpeneditExistingList(String listId);

        void onShareWithAllFriends();

        void onShareWithAllFriendsAndFollowers();

        void onOpenExcludedFriends();

        void onCreateCustomListFriend();

        void onSelectionDone();

        void updatePrivacy();

        void onDismissPopup();
    }

    PopupListener popupListener;

    public void setPopupListener(PopupListener popupListener) {
        this.popupListener = popupListener;
    }

    private void init(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && popupListener != null)
                    popupListener.onDismissPopup();
                return true;
            }
        });
        popup = new LinearLayout(context);
        listLayout = new MaxHeightLinearLayout(context);
//        listLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,500));
//        listLayout.setmMaxHeight(0);
        popup.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        popup.setOrientation(LinearLayout.VERTICAL);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        if (context != null) {
            popup.setBackground(context.getResources().getDrawable(R.drawable.rounded_corner_background));
        }
//        popup.setLayoutParams(new FrameLayout.LayoutParams(AppLibrary.convertDpToPixels(context, 296), ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(AppLibrary.convertDpToPixels(context, 296), ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        this.addView(popup, params);
        MontserratRegularTextView header = new MontserratRegularTextView(context);
        header.setText("Who can see this moment on my stream?");
        header.setTextColor(Color.BLACK);
        header.setAlpha(0.87f);
        int padding = AppLibrary.convertDpToPixels(context, 16);
        header.setPadding(padding, padding, padding, AppLibrary.convertDpToPixels(context, 8));
        header.setGravity(Gravity.START);
        popup.addView(header);
        CustomScrollView scrollView = new CustomScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(listLayout);
        popup.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawInitialSettingRows();
    }

    private LinearLayout popup;
    private MaxHeightLinearLayout listLayout;
    private Context context;

    private void drawInitialSettingRows() {
        for (int i = 0; i < 4; i++) {
            final View view = inflate(getContext(), R.layout.settings_model_view, null);
            ImageView radioButton = (ImageView) view.findViewById(R.id.contentRadio);
            if (i == 0) {
                view.setBackgroundColor(Color.WHITE);
                ((TextView) view.findViewById(R.id.desc)).setText("Public");
                ((TextView) view.findViewById(R.id.subDesc)).setText("Anyone on or off Pulse");
                ((ImageView) view.findViewById(R.id.contentDescIV)).setImageResource(R.drawable.privacy_public_svg);
                ;
                view.findViewById(R.id.updateImageView).setVisibility(GONE);
                view.setTag(new RowData(ALL_FRIENDS_AND_FOLLOWERS, false, false, null));
                radioButton.setTag(view.getTag());
            } else if (i == 1) {
                view.setBackgroundColor(Color.WHITE);
                ((TextView) view.findViewById(R.id.desc)).setText("Friends only");
                ((TextView) view.findViewById(R.id.subDesc)).setText("Your friends on Pulse");
                ((ImageView) view.findViewById(R.id.contentDescIV)).setImageResource(R.drawable.privacy_friendsonly_svg);
                view.findViewById(R.id.updateImageView).setVisibility(GONE);
                view.setTag(new RowData(ALL_FRIEND_ROW, false, false, null));
                radioButton.setTag(view.getTag());
            } else if (i == 2) {
                view.setBackgroundColor(Color.WHITE);
                ((TextView) view.findViewById(R.id.desc)).setText("Friends except");
                ((TextView) view.findViewById(R.id.subDesc)).setText("Your friends except specific people");
                ((ImageView) view.findViewById(R.id.contentDescIV)).setImageResource(R.drawable.privacy_friendsexcept_svg);

                view.setTag(new RowData(FRIENDS_EXCEPT_ROW, false, false, null));
                radioButton.setTag(view.getTag());
                view.findViewById(R.id.updateImageView).setTag(view);
                view.findViewById(R.id.updateImageView).setOnClickListener(customArrowClickListener);
            } else if (i == 3) {
                if (context != null) {
                    view.setBackground(context.getResources().getDrawable(R.drawable.rounded_bottom_corners));
                }
                ((TextView) view.findViewById(R.id.desc)).setText("Create Custom List");
                ((TextView) view.findViewById(R.id.subDesc)).setText("Your custom friend list");
                ((ImageView) view.findViewById(R.id.contentDescIV)).setImageResource(R.drawable.privacy_addnew_svg);

                //((TextView) view.findViewById(R.id.desc)).setTextColor(Color.parseColor("#9900ff"));
                view.setTag(new RowData(CREATE_NEW_LIST_ROW, false, false, null));
                radioButton.setTag(view.getTag());
                radioButton.setVisibility(GONE);
                view.findViewById(R.id.createListIV).setVisibility(INVISIBLE);
                view.findViewById(R.id.updateImageView).setVisibility(INVISIBLE);
            }
//            view.setOnClickListener(rowItemListener);
//            view.findViewById(R.id.contentRadio).setOnClickListener(radioListener);

            view.setOnClickListener(customItemClickListener);
            radioButton.setOnClickListener(customItemClickListener);

            View line = new View(context);
            listLayout.addView(view);
            line.setBackgroundColor(Color.BLACK);
            line.setAlpha(0.11f);
            listLayout.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLibrary.convertDpToPixels(context, 1)));
        }
    }

    @SuppressLint("CommitPrefEdits")
    public void setLastUsedChecked() {
        if (settingModel != null) {
            if (!preferences.getBoolean(AppLibrary.UPDATED_PRIVACY_TO_PUBLIC, false) || settingModel.lastUsedPrivacy == null) {
                settingModel.lastUsedPrivacy = FireBaseKEYIDS.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS;
                lastUsedPrivacy = settingModel.lastUsedPrivacy;
                if (popupListener != null) {
                    popupListener.updatePrivacy();
                    preferences.edit().putBoolean(AppLibrary.UPDATED_PRIVACY_TO_PUBLIC, true).commit();
                }
            }
            for (int i = 0; i < listLayout.getChildCount(); i++) {
                RowData tag = (RowData) listLayout.getChildAt(i).getTag();
                if (tag == null) {
                    continue;
                }
                if (settingModel.lastUsedPrivacy.equalsIgnoreCase(FireBaseKEYIDS.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS) && tag.rowType == ALL_FRIENDS_AND_FOLLOWERS) {
                    tag.isChecked = true;
                    selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS);
                    break;
                } else if (settingModel.lastUsedPrivacy.equalsIgnoreCase(FireBaseKEYIDS.PRIVACY_ALL_FRIENDS) && tag.rowType == ALL_FRIEND_ROW) {
                    tag.isChecked = true;
//                    setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), true);
//                    popupListener.onUpdatePrivacyTextView("All Friends");
                    selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS);
                    break;
                } else if (settingModel.lastUsedPrivacy.equalsIgnoreCase(FireBaseKEYIDS.PRIVACY_FRIENDS_EXCEPT) && tag.rowType == FRIENDS_EXCEPT_ROW) {
                    tag.isChecked = true;
//                    setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), true);
                    selectRowItem(PrivacyPolicy.PRIVACY_FRIENDS_EXCEPT);
                    String exceptString = "";
                    if (settingModel != null) {
                        if (settingModel.ignoredList != null) {
//                    final HashMap<String, SettingsModel.MemberDetails> ignoredList = settingModel.ignoredList;
                            int count = 0;
                            for (Map.Entry<String, SettingsModel.MemberDetails> entry : settingModel.ignoredList.entrySet()) {
                                final SettingsModel.MemberDetails value = entry.getValue();
                                ++count;
                                exceptString += value.name;
                                if (count == 2) {
//                                    if (count < settingModel.ignoredList.size())
//                                        exceptString += " ...";
                                    break;
                                } else exceptString += ", ";
                            }
                            exceptString = exceptString.substring(0, exceptString.length() - 2);
                        } else {
                            popupListener.onOpenExcludedFriends();
                        }
                        popupListener.onUpdatePrivacyTextView(exceptString, R.drawable.privacy_friendsexcept_svg);
                    }
                    break;
                } else if (tag.customListId != null && settingModel.lastUsedPrivacy.equals(tag.customListId)) {
                    selectRowItem(tag.customListId);
                    break;
                }
            }
        } else {
//            View childAt = listLayout.getChildAt(0);
//            RowData tag = (RowData) childAt.getTag();
//            tag.isChecked = true;
//            setImage(childAt.findViewById(R.id.contentRadio), true);
            selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS);
        }
    }


//    private View.OnClickListener radioListener = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            RowData rowDataOnClicked = (RowData) v.getTag();
//            boolean recordedCheck = rowDataOnClicked.isChecked;
//
//            for (int i = 0; i < listLayout.getChildCount(); i++) {
//                Object tag = listLayout.getChildAt(i).getTag();
//                if (tag == null)
//                    continue;
//                RowData rowData = (RowData) tag;
//                rowData.isChecked = false;
//                setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), false);
//            }
//            rowDataOnClicked.isChecked = !recordedCheck;
//            setImage(v, rowDataOnClicked.isChecked);
//
//            switch (rowDataOnClicked.rowType){
//                case ALL_FRIEND_ROW:
//                    popupListener.onUpdatePrivacyTextView("All Friends");
//                    break;
//                case FRIENDS_EXCEPT_ROW:
//                    String exceptString = "Except : ";
//                    if (settingModel != null && settingModel.ignoredList != null) {
////                    final HashMap<String, SettingsModel.MemberDetails> ignoredList = settingModel.ignoredList;
//                        int count = 0;
//                        for (Map.Entry<String, SettingsModel.MemberDetails> entry : settingModel.ignoredList.entrySet()) {
//                            final SettingsModel.MemberDetails value = entry.getValue();
//                            ++count;
//                            exceptString += value.name;
//                            if (count == 2) {
//                                if (count < settingModel.ignoredList.size())
//                                    exceptString += " ...";
//                                break;
//                            } else exceptString += ", ";
//
//                        }
//
//                    }
//                    popupListener.onUpdatePrivacyTextView(exceptString);
//                    break;
//                case EXISTING_CUSTOM_LIST_SELECTED_ROW:
//                    popupListener.onUpdatePrivacyTextView(settingModel.customFriendList.get(rowDataOnClicked.customListId).name);
//                    break;
//            }
//        }
//    };
//
//    private View.OnClickListener rowItemListener = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            final RowData tag = (RowData) v.getTag();
//            if (tag.rowType == CREATE_NEW_LIST_ROW) {
//                popupListener.onCreateCustomListFriend();
//                return;
//            }
////            tag.isChecked = !tag.isChecked;
//            tag.isChecked = true;
//            for (int i = 0; i < listLayout.getChildCount(); i++) {
//                if (listLayout.getChildAt(i).findViewById(R.id.contentRadio) != null) {
//                    if (v == listLayout.getChildAt(i)) continue;
////                    ((Radio Button) listLayout.getChildAt(i).findViewById(R.id.contentRadio)).setChecked(false);
//                    setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), false);
//                    final RowData tag1 = (RowData) listLayout.getChildAt(i).getTag();
//                    tag1.isChecked = false;
//                }
//            }
////            ((RadioBu tton) v.findViewById(R.id.contentRadio)).setChecked(tag.isChecked);
//            setImage(v.findViewById(R.id.contentRadio), tag.isChecked);
//
//            if (tag.rowType == ALL_FRIEND_ROW) {
//                popupListener.onShareWithAllFriends();
//                popupListener.onUpdatePrivacyTextView("All Friends");
//            } else if (tag.rowType == FRIENDS_EXCEPT_ROW) {
//                popupListener.onOpenExcludedFriends();
//                String exceptString = "Except :";
//                if (settingModel != null && settingModel.ignoredList != null) {
////                    final HashMap<String, SettingsModel.MemberDetails> ignoredList = settingModel.ignoredList;
//                    int count = 0;
//                    for (Map.Entry<String, SettingsModel.MemberDetails> entry : settingModel.ignoredList.entrySet()) {
//                        final SettingsModel.MemberDetails value = entry.getValue();
//                        ++count;
//                        exceptString += value.name;
//                        if (count == 2) {
//                            if (count < settingModel.ignoredList.size())
//                                exceptString += " ...";
//                            break;
//                        } else exceptString += ", ";
//
//                    }
//                    popupListener.onUpdatePrivacyTextView(exceptString);
//                } else popupListener.onUpdatePrivacyTextView(exceptString);
//            } else if (tag.rowType == EXISTING_CUSTOM_LIST_SELECTED_ROW) {
//                popupListener.onOpeneditExistingList(tag.customListId);
//                popupListener.onUpdatePrivacyTextView(settingModel.customFriendList.get(tag.customListId).name);
//            }
//
//        }
//    };

    public View.OnClickListener customItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            RowData rowDataOnClicked = (RowData) v.getTag();
            boolean recordedCheck = rowDataOnClicked.isChecked;

            if (rowDataOnClicked.rowType == CREATE_NEW_LIST_ROW) {
                popupListener.onCreateCustomListFriend();
                return;
            }

            if (recordedCheck) {
                if (rowDataOnClicked.rowType == FRIENDS_EXCEPT_ROW)
                    popupListener.onOpenExcludedFriends();
                return;
            }

//            for (int i = 0; i < listLayout.getChildCount(); i++) {
//                Object tag = listLayout.getChildAt(i).getTag();
//                if (tag == null)
//                    continue;
//                RowData rowData = (RowData) tag;
//                rowData.isChecked = false;
//                setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), false);
//            }
            rowDataOnClicked.isChecked = !recordedCheck;
//            setImage(v.findViewById(R.id.contentRadio), rowDataOnClicked.isChecked);

            switch (rowDataOnClicked.rowType) {
                case ALL_FRIENDS_AND_FOLLOWERS:
                    selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS);
                    popupListener.onUpdatePrivacyTextView("Public", R.drawable.privacy_public_svg);
                    break;
                case ALL_FRIEND_ROW:
                    selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS);
                    popupListener.onUpdatePrivacyTextView("Friends only", R.drawable.privacy_friendsonly_svg);
                    break;
                case FRIENDS_EXCEPT_ROW:
                    selectRowItem(PrivacyPolicy.PRIVACY_FRIENDS_EXCEPT);
                    String exceptString = "";
                    if (settingModel != null) {
                        if (settingModel.ignoredList != null) {
//                    final HashMap<String, SettingsModel.MemberDetails> ignoredList = settingModel.ignoredList;
                            int count = 0;
                            for (Map.Entry<String, SettingsModel.MemberDetails> entry : settingModel.ignoredList.entrySet()) {
                                final SettingsModel.MemberDetails value = entry.getValue();
                                ++count;
                                exceptString += value.name;
                                if (count == 2) {
//                                    if (count < settingModel.ignoredList.size())
//                                        exceptString += " ...";
                                    break;
                                } else exceptString += ", ";

                            }
                            exceptString = exceptString.substring(0, exceptString.length() - 2);

                        } else {
                            popupListener.onOpenExcludedFriends();
                        }
                        popupListener.onUpdatePrivacyTextView(exceptString, R.drawable.privacy_friendsexcept_svg);
                    }
                    break;
                case EXISTING_CUSTOM_LIST_SELECTED_ROW:
                    selectRowItem(rowDataOnClicked.customListId);
                    popupListener.onUpdatePrivacyTextView(settingModel.customFriendList.get(rowDataOnClicked.customListId).name, R.drawable.privacy_custom_svg);
                    break;
            }
        }
    };

    public View.OnClickListener customArrowClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            v = (View) v.getTag();
            final RowData tag = (RowData) v.getTag();
            if (tag.rowType == CREATE_NEW_LIST_ROW) {
                popupListener.onCreateCustomListFriend();
                return;
            }
//            tag.isChecked = !tag.isChecked;
            tag.isChecked = true;
//            for (int i = 0; i < listLayout.getChildCount(); i++) {
//                if (listLayout.getChildAt(i).findViewById(R.id.contentRadio) != null) {
//                    if (v == listLayout.getChildAt(i)) continue;
////                    ((Radio Button) listLayout.getChildAt(i).findViewById(R.id.contentRadio)).setChecked(false);
//                    setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), false);
//                    final RowData tag1 = (RowData) listLayout.getChildAt(i).getTag();
//                    tag1.isChecked = false;
//                }
//            }
//            ((RadioButton) v.findViewById(R.id.contentRadio)).setChecked(tag.isChecked);
//            setImage(v.findViewById(R.id.contentRadio), tag.isChecked);
            if (tag.rowType == ALL_FRIENDS_AND_FOLLOWERS) {
                selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS);
                popupListener.onShareWithAllFriendsAndFollowers();
                popupListener.onUpdatePrivacyTextView("All Friends and Followers", R.drawable.privacy_public_svg);
            } else if (tag.rowType == ALL_FRIEND_ROW) {
                selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS);
                popupListener.onShareWithAllFriends();
                popupListener.onUpdatePrivacyTextView("All Friends", R.drawable.privacy_friendsonly_svg);
            } else if (tag.rowType == FRIENDS_EXCEPT_ROW) {
                selectRowItem(PrivacyPolicy.PRIVACY_FRIENDS_EXCEPT);
                popupListener.onOpenExcludedFriends();
                String exceptString = "Friends except: ";
                if (settingModel != null && settingModel.ignoredList != null) {
//                    final HashMap<String, SettingsModel.MemberDetails> ignoredList = settingModel.ignoredList;
                    int count = 0;
                    for (Map.Entry<String, SettingsModel.MemberDetails> entry : settingModel.ignoredList.entrySet()) {
                        final SettingsModel.MemberDetails value = entry.getValue();
                        ++count;
                        exceptString += value.name;
                        if (count == 2) {
//                            if (count < settingModel.ignoredList.size())
//                                exceptString += " ...";
                            break;
                        } else exceptString += ", ";

                    }
                    exceptString = exceptString.substring(0, exceptString.length() - 2);
                    popupListener.onUpdatePrivacyTextView(exceptString, R.drawable.privacy_friendsexcept_svg);
                } else
                    popupListener.onUpdatePrivacyTextView(exceptString, R.drawable.privacy_friendsexcept_svg);
            } else if (tag.rowType == EXISTING_CUSTOM_LIST_SELECTED_ROW) {
                selectRowItem(tag.customListId);
                popupListener.onOpeneditExistingList(tag.customListId);
                popupListener.onUpdatePrivacyTextView(settingModel.customFriendList.get(tag.customListId).name, R.drawable.privacy_custom_svg);
            }
        }
    };

    void setImage(View imageView, boolean isChecked) {
        ((ImageView) imageView).setImageResource(isChecked ? R.drawable.radio_on_svg : R.drawable.radio_off_svg);
    }

    SettingsModel settingModel;

    public void onSettingsDataChanged(SettingsModel settingModel) {
        this.settingModel = settingModel;
        if (settingModel != null) {
            if (settingModel.ignoredList == null) {
                for (int i = 0; i < listLayout.getChildCount(); i++) {
                    Object tag = listLayout.getChildAt(i).getTag();
                    if (tag == null) continue;
                    RowData rowData = (RowData) tag;
                    if (rowData.rowType == FRIENDS_EXCEPT_ROW) {
                        String s = "Friends except: ";
                        ((TextView) listLayout.getChildAt(i).findViewById(R.id.desc)).setText(s);
                        ((TextView) listLayout.getChildAt(i).findViewById(R.id.subDesc)).setText("Your friends except specific people");
                        listLayout.getChildAt(i).findViewById(R.id.updateImageView).setVisibility(GONE);
                        if (lastUsedPrivacy != null && lastUsedPrivacy.equals(PrivacyPolicy.PRIVACY_FRIENDS_EXCEPT)) {
                            selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS);
                            if (popupListener != null) {
                                popupListener.onUpdatePrivacyTextView("Public", R.drawable.privacy_public_svg);
                            }
                        }
                    }
                }
            } else {//update ignoreList
                for (int i = 0; i < listLayout.getChildCount(); i++) {
                    Object tag = listLayout.getChildAt(i).getTag();
                    if (tag == null) continue;
                    RowData rowData = (RowData) tag;
                    if (rowData.rowType == FRIENDS_EXCEPT_ROW) {
                        String s = "";
                        for (Map.Entry<String, SettingsModel.MemberDetails> entry : settingModel.ignoredList.entrySet()) {
                            if (entry.getValue() != null)
                                s += entry.getValue().name + ", ";
                        }
                        s = s.substring(0, s.length() - 2);
                        ((TextView) listLayout.getChildAt(i).findViewById(R.id.subDesc)).setText(s);
                        listLayout.getChildAt(i).findViewById(R.id.updateImageView).setVisibility(VISIBLE);
                        if (popupListener != null && lastUsedPrivacy != null && lastUsedPrivacy.equals(PrivacyPolicy.PRIVACY_FRIENDS_EXCEPT)) {
                            popupListener.onUpdatePrivacyTextView(s, R.drawable.privacy_friendsexcept_svg);
                        }
                    }
                }
            }

            if (settingModel.customFriendList == null)
                return;
            for (Map.Entry<String, SettingsModel.CustomFriendListDetails> entry : settingModel.customFriendList.entrySet()) {//update custom lists
                boolean listFound = false;
                for (int i = 0; i < listLayout.getChildCount(); i++) {
                    Object tag = listLayout.getChildAt(i).getTag();
                    TextView tv = (TextView) listLayout.getChildAt(i).findViewById(R.id.desc);
                    if (tag == null) continue;
                    if (((RowData) tag).rowType == EXISTING_CUSTOM_LIST_SELECTED_ROW && ((RowData) tag).customListId.equals(entry.getKey())) {
                        if (!tv.getText().toString().equals(entry.getValue().name)) {
                            tv.setText(entry.getValue().name);
                        }
                        ((TextView) listLayout.getChildAt(i).findViewById(R.id.subDesc).findViewById(R.id.subDesc)).setText(entry.getValue().members.size() + " members ");
                        listFound = true;
                        break;
                    }
                }
                if (!listFound) {
                    addCustomLists(entry);
                    selectRowItem(entry.getKey());
                    if (popupListener != null) {
                        popupListener.onUpdatePrivacyTextView(entry.getValue().name, R.drawable.privacy_custom_svg);
                    }
                }
                //else list exists do nothing
            }
        }
    }

    public void selectRowItem(String id) {

        for (int i = 0; i < listLayout.getChildCount(); i++) {
            RowData tag = (RowData) listLayout.getChildAt(i).getTag();
            if (tag == null)
                continue;
            if (tag.isChecked) {
                tag.isChecked = false;
                setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), false);
            }
        }

        for (int i = 0; i < listLayout.getChildCount(); i++) {
            RowData tag = (RowData) listLayout.getChildAt(i).getTag();
            if (tag == null)
                continue;
            if (tag.rowType == ALL_FRIENDS_AND_FOLLOWERS && id.equals(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS)) {
                tag.isChecked = true;
                setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), true);
                if (popupListener != null)
                    popupListener.onUpdatePrivacyTextView("Public", R.drawable.privacy_public_svg);
                lastUsedPrivacy = FireBaseKEYIDS.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS;
                break;
            } else if (tag.rowType == ALL_FRIEND_ROW && id.equals(PrivacyPolicy.PRIVACY_ALL_FRIENDS)) {
                tag.isChecked = true;
                setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), true);
                if (popupListener != null)
                    popupListener.onUpdatePrivacyTextView("All Friends", R.drawable.privacy_friendsonly_svg);
                lastUsedPrivacy = FireBaseKEYIDS.PRIVACY_ALL_FRIENDS;
                break;
            } else if (tag.rowType == FRIENDS_EXCEPT_ROW && id.equals(PrivacyPolicy.PRIVACY_FRIENDS_EXCEPT)) {
                tag.isChecked = true;
                setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), true);
                lastUsedPrivacy = FireBaseKEYIDS.PRIVACY_FRIENDS_EXCEPT;
                break;
            } else if (tag.rowType == EXISTING_CUSTOM_LIST_SELECTED_ROW) {
                if (tag.customListId.equals(id)) {
                    tag.isChecked = true;
                    setImage(listLayout.getChildAt(i).findViewById(R.id.contentRadio), true);
                    if (popupListener != null)
                        popupListener.onUpdatePrivacyTextView(settingModel.customFriendList.get(tag.customListId).name, R.drawable.privacy_custom_svg);
                    lastUsedPrivacy = tag.customListId;
                    break;
                }
            }
        }
    }

    void addCustomLists(Map.Entry<String, SettingsModel.CustomFriendListDetails> entry) {

        final View view = inflate(getContext(), R.layout.settings_model_view, null);
        ((TextView) view.findViewById(R.id.desc)).setText(entry.getValue().name);
        view.findViewById(R.id.updateImageView).setVisibility(VISIBLE);
        view.setOnClickListener(customItemClickListener);
//        view.findViewById(R.id.contentRadio).setOnClickListener(radioListener);
        view.setTag(new RowData(EXISTING_CUSTOM_LIST_SELECTED_ROW, false, false, entry.getKey()));
        view.findViewById(R.id.updateImageView).setTag(view);
        view.findViewById(R.id.updateImageView).setOnClickListener(customArrowClickListener);
        view.findViewById(R.id.contentRadio).setTag(view.getTag());
        view.findViewById(R.id.contentRadio).setOnClickListener(customItemClickListener);

        ((TextView) view.findViewById(R.id.subDesc)).setText(entry.getValue().members.size() + " members ");

        view.setBackgroundColor(Color.WHITE);
        listLayout.addView(view, listLayout.getChildCount() - 2);

        View line = new View(context);
        line.setBackgroundColor(Color.BLACK);
        line.setAlpha(0.11f);
        listLayout.addView(line, listLayout.getChildCount() - 2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLibrary.convertDpToPixels(context, 1)));

    }

    public class RowData {
        public int rowType;
        public boolean isChecked;
        public boolean isSubListType;//boolean representing whther it is the member of custom list or groups
        public String customListId;

        public RowData(int rowType, boolean isChecked, boolean isSubListType, String customListId) {
            this.rowType = rowType;
            this.isChecked = isChecked;
            this.isSubListType = isSubListType;
            this.customListId = customListId;
        }
    }

//
//    public void setViewControlsCallback(Fragment fragment) {
//        viewControlsCallback = (ViewControlsCallback) fragment;
//    }

//    public void initializeViewObjects(String settingName, final String id) {
//        this.id = id;
//        this.radioButton.setText(settingName);
//        this.updateImageView = (ImageView) itemRowView.findViewById(R.id.updateImageView);
//        this.updateImageView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                viewControlsCallback.onUpdateListClicked(id);
//                radioButton.setChecked(true);
//            }
//        });
//        this.itemRowView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                AppLibrary.log_d("SettingsLayout", "clicked");
//                viewControlsCallback.onSettingRowItemClicked(id);
//                radioButton.setChecked(true);
//            }
//        });
//    }


    public void setRadioChecked(boolean check) {
        this.radioButton.setChecked(check);
    }

    public void setDescText(String descText) {
        descTextView.setText(descText);
        descTextView.setVisibility(View.VISIBLE);
    }

//    public interface ViewControlsCallback {
//        void onUpdateListClicked(String id);
//
//        void onSettingRowItemClicked(String id);
//    }

    public View getUpdateView() {
        return updateImageView;
    }
}