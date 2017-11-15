package com.pulseapp.android.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.OnBoardingActivity;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by deepankur on 6/23/16.
 */
public class ChooseUserNameFragment extends BaseFragment {

    private String userName;
    private EditText userHandleEt;
    private String TAG = this.getClass().getSimpleName();
    private View progressView;
    private TextView nextPageTV;
    private SharedPreferences preferences;
//    private int displayWidth;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) return;
        if (context == null || FireBaseHelper.getInstance(context).getMyUserModel() == null) return;

        userName = FireBaseHelper.getInstance(context).getMyUserModel().name;
        preferences = getActivity().getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
//        displayWidth = AppLibrary.getDeviceParams(getActivity(), "width");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_choose_username, container, false);

        if (savedInstanceState != null || mFireBaseHelper == null || mFireBaseHelper.getMyUserModel() == null)
            return rootView;

        String s = "Hello " + userName + ", welcome to Pulse.";

        Picasso.with(context).load(mFireBaseHelper.getMyUserModel().imageUrl).transform(new RoundedTransformation(20)).
                into((ImageView) rootView.findViewById(R.id.chooseUserNameIV));

        ((TextView) rootView.findViewById(R.id.helloUserNameTV)).setText(s);
        ((TextView) rootView.findViewById(R.id.helloUserNameTV)).setTypeface(fontPicker.getMuseo700());
        ((TextView) rootView.findViewById(R.id.chooseUserNameTV)).setTypeface(fontPicker.getMuseo500());

        userHandleEt = (EditText) rootView.findViewById(R.id.chooseUserNameET);
        progressView = (ProgressBar) rootView.findViewById(R.id.progressBar);
        userHandleEt.setTypeface(fontPicker.getMuseo500());
        progressView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        progressView.setVisibility(View.GONE);

        ((ProgressBar) rootView.findViewById(R.id.progressBar))
                .getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#80FFFFFF"), PorterDuff.Mode.SRC_IN);

        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(userHandleEt, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        nextPageTV = (TextView) rootView.findViewById(R.id.nextPageTV);

        nextPageTV.setVisibility(View.GONE);
        nextPageTV.setTypeface(fontPicker.getMontserratRegular());
        nextPageTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUserNameInputed();
            }
        });

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);

                int screenHeight = rootView.getRootView().getHeight();
                int resourceId = getResources().getIdentifier("status_bar_height",
                        "dimen", "android");

                int heightDifference = screenHeight - (r.bottom - r.top);
                if (resourceId > 0) {
                    heightDifference -= getResources()
                            .getDimensionPixelSize(resourceId);
                }

//                if (heightDifference>100) {
//                    nextPageTV.setTranslationY(-heightDifference);
//                    Log.d(TAG, "Keyboard Size: " + heightDifference);
//                } else {
//                    nextPageTV.setTranslationY(0);
//                }
            }
        });

        userHandleEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "onEditorAction: " + " actionId " + actionId + " event " + event);
                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    userHandleEt.clearFocus();
//                    showShortToastMessage("input");
                    if (userHandleEt != null && userHandleEt.getText().toString().trim().length() > 0)
                        onUserNameInputed();
                    else showShortToastMessage("Please select a valid username");
                }
                return false;
            }
        });

        userHandleEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0) {

                    if (!s.toString().equals(s.toString().toLowerCase())) {
                        userHandleEt.setText(s.toString().toLowerCase());
                        userHandleEt.setSelection(s.toString().length());
                    }

                    showButtonToProceed();
                } else
                    hideButtonToProceed();
            }
        });

        return rootView;
    }

    private void onUserNameInputed() {
        if (checkUserHandle(userHandleEt.getText().toString().trim())) {
            userHandleEt.clearFocus();
            userHandleEt.setFocusable(false);
            hitApiForHandle(mFireBaseHelper.getMyUserId(), userHandleEt.getText().toString().trim().toLowerCase());
            progressView.setVisibility(View.VISIBLE);
        }
    }

    private void hideButtonToProceed() {
        nextPageTV.setVisibility(View.GONE);
    }

    private boolean checkUserHandle(String s) {
        String pattern = "^[a-zA-Z0-9_]*$"; //Alphanumeric characters and underscore
        if (s.matches(pattern)) {
            if (s.length() >= 4 && s.length() <= 15)
                return true;
            else {
                if (s.length() < 4)
                    showShortToastMessage("Username should atleast be 4 characters long");
                else
                    showShortToastMessage("Username can at max be 15 characters long");

                return false;
            }
        } else {
            showShortToastMessage("Username should contain alpha-numeric characters only");
            return false;
        }
    }

    private void showButtonToProceed() {
        nextPageTV.setVisibility(View.VISIBLE);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null || mFireBaseHelper == null || mFireBaseHelper.getMyUserModel() == null || userHandleEt==null)
            return;

        userHandleEt.requestFocus();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toggleSoftKeyboard(context, userHandleEt, true);
            }
        }, 200);

    }

    @SuppressWarnings("deprecation")
    private void hitApiForHandle(String userId, String handle) {

        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("userId", userId));
        pairs.add(new BasicNameValuePair("handle", handle));

        RequestManager.makePostRequest(getActivity(), RequestManager.ADD_USER_HANDLE_REQUEST, RequestManager.ADD_USER_HANDLE_RESPONSE, null, pairs, usrHandleCallBack);
    }

    private boolean isDestroyed;
    private RequestManager.OnRequestFinishCallback usrHandleCallBack = new RequestManager.OnRequestFinishCallback() {
        @SuppressLint("CommitPrefEdits")
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                Log.d(TAG, " server says " + response);
                final JSONObject object = (JSONObject) response;
                if (success) {
                    boolean error = object.getBoolean("error");
                    if (!error) {
                        progressView.setVisibility(View.VISIBLE);
//                        userHandleEt.setFocusable(false);
//                        userHandleEt.clearFocus();
                        toggleSoftKeyboard(context, userHandleEt, false);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt(AppLibrary.USER_ONBOARDING_STATUS, USER_NAME_SELECTION_DONE);
                        editor.commit();
//                        toggleSoftKeyboard(context, userHandleEt, false);

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(userHandleEt.getApplicationWindowToken(), 0);

                        mFireBaseHelper.getMyUserModel().handle = object.getString("value");
                        ((OnBoardingActivity) getActivity()).onUserNameSelectedSuccessfully();
                    } else {
                        progressView.setVisibility(View.GONE);
                        showShortToastMessage("Username already taken. Please try a different username");
                    }
                } else {
                    Log.e(TAG, "usrHandleCallBack Error, response -" + object);
                    showShortToastMessage("Please try again");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "usrHandleCallBack JsonException " + e);
                showShortToastMessage("Please try again");
            }
        }

        @Override
        public boolean isDestroyed() {
            return isDestroyed;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
    }
}
