package com.pulseapp.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.InstituteSearchResultsAdapter;
import com.pulseapp.android.downloader.ChatCameraStickerDownloader;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.ArrayList;

/**
 * Created by indianrenters on 8/24/16.
 */
public class InstitutionEditFragment extends BaseFragment implements AdapterView.OnItemClickListener {

    private EditText institutionName;
    private InputMethodManager imm;
    private ArrayList<UserModel.InstitutionData> allInstitutesData;
    private ArrayList<UserModel.InstitutionData> names = new ArrayList<>();
    private InstituteSearchResultsAdapter instituteSearchResultsAdapter;
    private UserModel.InstitutionData selectedInstitute;
    private ListView lvInstitute;
    private ImageView ivClear;
    private int count = 0;
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().trim().length() > 0)
                startPartialSearch(s.toString().trim().toLowerCase());
            else {
                names.clear();
                instituteSearchResultsAdapter.notifyDataSetChanged();
            }

        }
    };
    private String TAG = getClass().getSimpleName();

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mFireBaseHelper.loadAllInstitutesData();
    }

    private boolean isComingFromStickers;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null)
            isComingFromStickers = getArguments().getBoolean(CameraActivity.IS_COMING_FROM_STICKERS, false);

        if (isComingFromStickers)
            ((CameraActivity) getActivity()).toggleFullScreen(true);

        View rootView = inflater.inflate(R.layout.fragment_edit_institute, container, false);
        mFireBaseHelper.setAllInstitutesLoaded(new FireBaseHelper.AllInstitutesLoaded() {
            @Override
            public void onInstituteLoaded() {
                allInstitutesData = mFireBaseHelper.getAllInstitutesData();
                if (instituteSearchResultsAdapter != null) {
                    instituteSearchResultsAdapter.notifyDataSetChanged();
                    if (institutionName != null && institutionName.getText().toString().trim().length() > 0) {
                        startPartialSearch(institutionName.getText().toString().trim());
                    }
                }
            }
        });
        initializeViews(rootView);
        setupActionBar(rootView);
        return rootView;
    }

    private void initializeViews(View rootView) {
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        institutionName = (EditText) rootView.findViewById(R.id.et_institution);
        lvInstitute = (ListView) rootView.findViewById(R.id.lv_institute);
        institutionName.setTypeface(fontPicker.getMontserratRegular());

        ivClear = (ImageView) rootView.findViewById(R.id.iv_clear);
        institutionName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                institutionName.setCursorVisible(hasFocus);
                institutionName.setSelection(institutionName.getText().length());
                if (hasFocus) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                } else {
                    imm.hideSoftInputFromWindow(institutionName.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });

        if (mFireBaseHelper.getMyUserModel().miscellaneous != null && mFireBaseHelper.getMyUserModel().miscellaneous.institutionData != null
                && mFireBaseHelper.getMyUserModel().miscellaneous.institutionData.name != null)
            institutionName.setText(mFireBaseHelper.getMyUserModel().miscellaneous.institutionData.name);

        institutionName.addTextChangedListener(textWatcher);
        institutionName.requestFocus();

        instituteSearchResultsAdapter = new InstituteSearchResultsAdapter(names, getActivity());

        lvInstitute.setAdapter(instituteSearchResultsAdapter);
        lvInstitute.setOnItemClickListener(this);
        ivClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                institutionName.setText("");
                names.clear();
            }
        });
    }

    private void setupActionBar(View actionBar) {
//        actionBar.findViewById(R.id.action_bar_IV_1).setVisibility(View.GONE);
        ImageView back = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        back.setImageResource(R.drawable.back_svg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
                if (institutionName != null)
                    toggleSoftKeyboard(context, institutionName, false);
            }
        });
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        ((TextView) actionBar.findViewById(R.id.titleTV)).setText(getResources().getString(R.string.institute_edit_heading));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(Utils.dp2Px(72));
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();


    }

    private void startPartialSearch(String query) {
        names.clear();
        if (allInstitutesData != null) {
            for (UserModel.InstitutionData data : allInstitutesData) {
                if (data.name.toLowerCase().contains(query) || query.contains(data.name.toLowerCase()) || isTagFound(data, query)) {
                    names.add(data);
                }
            }
            instituteSearchResultsAdapter.notifyDataSetChanged();
        }
    }

    private boolean isTagFound(UserModel.InstitutionData data, String query) {
        if (data != null && data.tags != null) {
            for (String s : data.tags.keySet()) {
                s = s.toLowerCase();
                if (s.equals(query) || s.contains(query) || query.contains(s))
                    return true;
            }
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        institutionName.clearFocus();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectedInstitute = names.get(position);
        institutionName.setText("");
        institutionName.append(selectedInstitute.name);
//        names.clear();
//        instituteSearchResultsAdapter.notifyDataSetChanged();
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UserModel myUserModel = mFireBaseHelper.getMyUserModel();
        if (isComingFromStickers)
            ((CameraActivity) getActivity()).toggleFullScreen(true);
        if (selectedInstitute != null) {
            if (myUserModel.miscellaneous != null && myUserModel.miscellaneous.institutionData != null) {
                if (!myUserModel.miscellaneous.institutionData.name.equalsIgnoreCase(selectedInstitute.name)) {
                    mFireBaseHelper.sendInstituteChangeRequest(selectedInstitute);
                    ((CameraActivity) getActivity()).updateInstitutionOnSettingFragment(selectedInstitute.name, selectedInstitute.momentId);
                    onInstitutionSelected(selectedInstitute.momentId);
                }
            } else {
                mFireBaseHelper.sendInstituteChangeRequest(selectedInstitute);
                ((CameraActivity) getActivity()).updateInstitutionOnSettingFragment(selectedInstitute.name, selectedInstitute.momentId);
                onInstitutionSelected(selectedInstitute.momentId);
            }
        }
    }

    private void onInstitutionSelected(String momentId) {
        ChatCameraStickerDownloader.getChatCameraStickerDownloader(getActivity()).onInstitutionSelectedAtRuntime(momentId);
    }
}
