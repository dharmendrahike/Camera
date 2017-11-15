package com.pulseapp.android.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.OnBoardingActivity;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.OverRideBackEventEditText;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by deepankur on 6/23/16.
 */
public class SelectInstitutionFragment extends BaseFragment {

    private OverRideBackEventEditText institutionET;
    private View rootView;
    private String TAG = getClass().getSimpleName();
    private LinearLayout suggestionsLayout;
    private TextView instituteSelectedTv;
    private View schoolIv;
    private TextView schollTv, schollTv2;
    private HorizontalScrollView scrollView;
    InputMethodManager imm;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) return;

        getData();
    }

    final int PROCESS_QUERY_THRESHOLD = 800;//we give user 800ms to finish typing and only then do the  processing
    Handler queryHandler = new Handler();
    Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            if (previousQuery.equals(institutionET.getText().toString().trim().toLowerCase())) {
                queryHandler.removeCallbacksAndMessages(null);
                Log.d(TAG, "starting search @ " + System.currentTimeMillis());
                startPartialSearch(previousQuery);
                Log.d(TAG, "finished search @ " + System.currentTimeMillis());

            }
        }
    };

    String previousQuery = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_select_institution, container, false);

        if (savedInstanceState!=null) return rootView;

        rootView.getLayoutParams().height = AppLibrary.getDeviceParams(getActivity(), "height")*2;//if we don't do this the
        //views which have margin top > screen height won't be drawn, (the cancel button in this case)

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        convertDpToPixel();
        scrollView = (HorizontalScrollView) rootView.findViewById(R.id.scrollView);
        schollTv = (TextView) rootView.findViewById(R.id.school_1_TV);
        schollTv2 = (TextView) rootView.findViewById(R.id.school_2_TV);
        selectedLL = (LinearLayout) rootView.findViewById(R.id.instituteSelectedLL);
        schoolIv = rootView.findViewById(R.id.schoolIV);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        institutionET = (OverRideBackEventEditText) rootView.findViewById(R.id.schoolET);
//        institutionET.requestFocus();
        toggleSoftKeyboard(context, institutionET, true);
        suggestionsLayout = (LinearLayout) rootView.findViewById(R.id.suggestionLL);
        instituteSelectedTv = (TextView) rootView.findViewById(R.id.instituteSelectedTV);
        institutionET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "onEditorAction: " + " actionId " + actionId + " event " + event);
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    //translateDown();
//                    Log.d(TAG, "starting search @ " + System.currentTimeMillis());
//                    startPartialSearch(institutionET.getText().toString());
                }
                return true;
            }
        });

        institutionET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                previousQuery = s.toString().trim().toLowerCase();
                queryHandler.postDelayed(queryRunnable, PROCESS_QUERY_THRESHOLD);
            }
        });
//        institutionET.setFocusable(true);
//        institutionET.setFocusableInTouchMode(true);
//        institutionET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                Log.d(TAG, "onFocusChange " + hasFocus);
//                if (!hasFocus) {
//                    // code to execute when EditText loses focus
//                }
//            }
//        });

        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(institutionET, R.drawable.cursor);
        } catch (Exception ignored) {
        }

//        institutionET.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                Log.d(TAG, "onTouch " + event);
//                if (event.getAction() == MotionEvent.ACTION_DOWN)
//                    if (institutionET.getTranslationY() == 0)
//                        translateUP();
//                return false;
//            }
//        });

        institutionET.setOnKeyPreImeListener(new OverRideBackEventEditText.OnKeyPreImeListener() {
            @Override
            public void onKeyPreImePressed(int keyCode, KeyEvent event) {
                Log.d(TAG, "onKeyPreIme: keycode " + keyCode + " event " + event);
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    translateDown();
                }
            }
        });
        institutionET.setTypeface(fontPicker.getMuseo500());
        ((TextView) rootView.findViewById(R.id.school_1_TV)).setTypeface(fontPicker.getMuseo700());
        ((TextView) rootView.findViewById(R.id.skipTV)).setTypeface(fontPicker.getMontserratRegular());

        ((TextView) rootView.findViewById(R.id.instituteSelectedTV)).setTypeface(fontPicker.getMuseo500());

        rootView.findViewById(R.id.nextPageTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                imm.hideSoftInputFromWindow(institutionET.getApplicationWindowToken(), 0);

                if (currentlySelectedData != null)
                    ((OnBoardingActivity) getActivity()).onInstitutionSelectionComplete(true, currentlySelectedData, institutionET.getText().toString().trim());
                else {
                    if (institutionET != null && institutionET.getText().toString().trim().length() > 0)
                        ((OnBoardingActivity) getActivity()).onInstitutionSelectionComplete(false, null, institutionET.getText().toString().trim());
                }
            }
        });

        rootView.findViewById(R.id.cancelIV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(institutionET.getApplicationWindowToken(), 0);
                currentlySelectedData = null;
                toggleSelectionState(false);
            }
        });

        rootView.findViewById(R.id.skipTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(institutionET.getApplicationWindowToken(), 0);
                if (institutionET != null) {
                    ((OnBoardingActivity) getActivity()).onInstitutionSelectionComplete(false, null, institutionET.getText().toString().trim());
                }
            }
        });

        schollTv.setOnClickListener(moveViewUpListener);
        return rootView;
    }

    private View.OnClickListener moveViewUpListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (institutionET.getTranslationY() == 0)
                translateUP();
        }
    };

    private ArrayList<UserModel.InstitutionData> filteredData = new ArrayList<>();
    private int IMAGE_OFFEST = -504;
    private int TEXT_VIEW_OFFSET = 108 - 504;

    void convertDpToPixel() {
        IMAGE_OFFEST = AppLibrary.convertDpToPixels(context, IMAGE_OFFEST);
        TEXT_VIEW_OFFSET = AppLibrary.convertDpToPixels(context, TEXT_VIEW_OFFSET);

    }

    void translateUP() {
        ObjectAnimator.ofFloat(schollTv, "translationY", schollTv.getTranslationY(), TEXT_VIEW_OFFSET).setDuration(200).start();
        ObjectAnimator.ofFloat(schollTv2, "translationY", schollTv2.getTranslationY(), TEXT_VIEW_OFFSET).setDuration(200).start();
        ObjectAnimator.ofFloat(schoolIv, "translationY", schoolIv.getTranslationY(), IMAGE_OFFEST).setDuration(200).start();
        ObjectAnimator.ofFloat(selectedLL, "translationY", selectedLL.getTranslationY(), TEXT_VIEW_OFFSET).setDuration(200).start();
        ObjectAnimator.ofFloat(scrollView, "translationY", scrollView.getTranslationY(), TEXT_VIEW_OFFSET).setDuration(200).start();
        ObjectAnimator.ofFloat(institutionET, "translationY", institutionET.getTranslationY(), TEXT_VIEW_OFFSET).setDuration(200).start();
        redrawAddSchoolCollege(true);
        institutionET.requestFocus();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toggleSoftKeyboard(context, institutionET, true);

            }
        }, 500);
    }

    void translateDown() {
        ObjectAnimator.ofFloat(schollTv, "translationY", TEXT_VIEW_OFFSET, 0).setDuration(200).start();
        ObjectAnimator.ofFloat(schollTv2, "translationY", TEXT_VIEW_OFFSET, 0).setDuration(200).start();
        ObjectAnimator.ofFloat(schoolIv, "translationY", IMAGE_OFFEST, 0).setDuration(200).start();
        ObjectAnimator.ofFloat(selectedLL, "translationY", TEXT_VIEW_OFFSET, 0).setDuration(200).start();
        ObjectAnimator.ofFloat(scrollView, "translationY", TEXT_VIEW_OFFSET, 0).setDuration(200).start();
        ObjectAnimator.ofFloat(institutionET, "translationY", TEXT_VIEW_OFFSET, 0).setDuration(200).start();
        redrawAddSchoolCollege(false);
    }

    void redrawAddSchoolCollege(boolean translateUp) {
        schollTv.setBackgroundResource(translateUp ? 0 : R.drawable.purple_button);
        schollTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, translateUp ? 20 : 14);
        FontPicker instance = FontPicker.getInstance(context);
        schollTv.setTypeface(translateUp ? instance.getMontserratRegular() : instance.getMuseo700());
        schollTv2.setVisibility(translateUp ? View.VISIBLE : View.INVISIBLE);
        institutionET.setVisibility(translateUp ? View.VISIBLE : View.GONE);
        scrollView.setVisibility(translateUp ? View.VISIBLE : View.GONE);

//        rootView.findViewById(R.id.cancelIV).invalidate();
//        rootView.findViewById(R.id.cancelIV).requestLayout();

    }

    private boolean isTagFound(UserModel.InstitutionData data, String query) {
        for (String s :
                data.tags.keySet()) {
            s= s.toLowerCase();
            if (s.contains(query) || query.contains(s))
                return true;
        }
        return false;
    }

    private void startPartialSearch(String query) {
        int match = 0;
        boolean broken = false;//we break the search at 10 elements per query
        filteredData.clear();
        for (UserModel.InstitutionData data : allInstitutesData) {
            if (data.name.toLowerCase().contains(query) || query.contains(data.name.toLowerCase()) || isTagFound(data, query)) {
                filteredData.add(data);
                match++;
                if (match == 10) {
                    broken = true;
                    break;
                }
            }
        }
        if (!broken)
            Log.d(TAG, " iterated through " + allInstitutesData.size() + " elements @ " + System.currentTimeMillis());

        filteredData.add(null);
        suggestionsLayout.removeAllViews();

        for (int i = 0; i < filteredData.size(); i++) {
            UserModel.InstitutionData data = filteredData.get(i);
            TextView textView = new TextView(context);
            suggestionsLayout.addView(textView);
            suggestionsLayout.setVisibility(View.VISIBLE);
            textView.setBackgroundResource(R.drawable.institute_suggestion_background);
            textView.setTextColor(Color.parseColor("#FFFFFF"));
            textView.setTypeface(fontPicker.getMontserratRegular());
            textView.setPadding(AppLibrary.convertDpToPixels(context,16),AppLibrary.convertDpToPixels(context,8),AppLibrary.convertDpToPixels(context,16),AppLibrary.convertDpToPixels(context,8));
            ((LinearLayout.LayoutParams) textView.getLayoutParams()).leftMargin = AppLibrary.convertDpToPixels(context, 8);
            textView.setGravity(Gravity.CENTER);
            textView.setTag(data);
            if (data != null) {
                textView.setText(data.name);
            } else textView.setText("Unable to find?");
            textView.setOnClickListener(suggestionClickedListener);
        }
    }

    UserModel.InstitutionData currentlySelectedData;

    private View.OnClickListener suggestionClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            imm.hideSoftInputFromWindow(institutionET.getApplicationWindowToken(), 0);
            UserModel.InstitutionData data = (UserModel.InstitutionData) v.getTag();
            if (data != null) {
                currentlySelectedData = data;
                instituteSelectedTv.setText(data.name);
                toggleSelectionState(true);
            } else {
                saveUnableToFindInstituteInFireBase();
            }
        }
    };

    private void saveUnableToFindInstituteInFireBase() {
        //// TODO: 6/24/16
        if (institutionET != null && institutionET.getText().toString().trim().length() > 0)
            ((OnBoardingActivity) getActivity()).onInstitutionSelectionComplete(false, null, institutionET.getText().toString().trim());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    LinearLayout selectedLL;

    void toggleSelectionState(boolean selected) {
        selectedLL.setVisibility(selected ? View.VISIBLE : View.GONE);
        rootView.findViewById(R.id.nextPageTV).setVisibility(selected ? View.VISIBLE : View.GONE);

        institutionET.setVisibility(selected ? View.GONE : View.VISIBLE);
        suggestionsLayout.setVisibility(selected ? View.GONE : View.VISIBLE);
    }

    private ArrayList<UserModel.InstitutionData> allInstitutesData = new ArrayList<>();


    void getData() {
        this.allInstitutesData = OnBoardingActivity.getInstitutionDetails();
    }

    final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private SecureRandom rnd = new SecureRandom();

    private String randomString(final int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
            if (i == 5) sb.append(" ");
        }
        return sb.toString();
    }
}
