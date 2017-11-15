package com.pulseapp.android.fragments;

import android.app.Service;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;

/**
 * Created by indianrenters on 7/28/16.
 */
public class ChangeNameFragment extends BaseFragment {

    private EditText name;
    private UserModel myUserModel;
    private String oldName;
    private FireBaseHelper mFireBaseHelper;
    private InputMethodManager inputMethodManager;
    private final String TAG = ChangeNameFragment.class.getSimpleName();

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     * <p/>
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_name_change,container,false);
        initializeViews(rootView);
        return rootView;
    }

    private void initializeViews(View rootView) {
        mFireBaseHelper = FireBaseHelper.getInstance(getActivity());
        myUserModel = mFireBaseHelper.getMyUserModel();
        oldName = myUserModel.name;
        name = (EditText) rootView.findViewById(R.id.tv_name);
        name.setText(oldName);

        if (fontPicker == null)
            fontPicker = FontPicker.getInstance(MasterClass.getGlobalContext());

        name.setTypeface(fontPicker.getMontserratRegular());
        inputMethodManager = (InputMethodManager)getActivity().getApplicationContext().getSystemService(Service.INPUT_METHOD_SERVICE);
//        inputMethodManager.showSoftInput(name,InputMethodManager.SHOW_FORCED);
        name.requestFocus();
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
        name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    inputMethodManager.hideSoftInputFromWindow(name.getWindowToken(),0);
                    getActivity().onBackPressed();
                }
                return true;
            }
        });
        setupActionBar(rootView.findViewById(R.id.action_bar));

    }


    @Override
    public void onDestroyView() {
        saveIfNameChanged();
        super.onDestroyView();
    }

    private void setupActionBar(View actionBar){
//        actionBar.findViewById(R.id.action_bar_IV_1).setVisibility(View.GONE);
        ImageView back = (ImageView)actionBar.findViewById(R.id.action_bar_IV_1);
        back.setImageResource(R.drawable.back_svg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputMethodManager.hideSoftInputFromWindow(name.getWindowToken(),0);
                getActivity().onBackPressed();
            }
        });
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        ((TextView)actionBar.findViewById(R.id.titleTV)).setText("NAME");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(Utils.dp2Px(72));
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();


    }

    private void saveIfNameChanged() {
        if (oldName != null && name.getText()!=null &&
                !name.getText().toString().trim().isEmpty() && !oldName.equals(name.getText().toString())) {
            mFireBaseHelper.updateNameInFireBase(name.getText().toString());
            mFireBaseHelper.sendNameUpdateRequestToFireBase(name.getText().toString(), mFireBaseHelper.getMyUserId());
            ((CameraActivity)getActivity()).updateNameInSettingsFragment();


            Log.e(TAG, "saveIfNameChanged: ");
        } else {
            // show any message
        }
    }

    void popFragmentFromBackStack() {
        if (getFragmentInBackStack(SettingsFragment.class.getSimpleName()) != null) {
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
