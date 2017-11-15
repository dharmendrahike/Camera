package com.pulseapp.android.fragments;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by indianrenters on 7/29/16.
 */
public class BirthdayFragment extends BaseFragment implements View.OnClickListener {

    private TextView birthday;
    private UserModel mUserModel;
    private CheckBox birthdayReveal;
    private SettingsModel mSettingsModel;
    private String[] dateArr;

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
        View rootView = inflater.inflate(R.layout.fragment_birthday, container, false);
        initializeViews(rootView);
        return rootView;
    }

    private void initializeViews(View rootView) {
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        birthday = (TextView) rootView.findViewById(R.id.tv_birthday);
        birthdayReveal = (CheckBox) rootView.findViewById(R.id.cb_birthday_reveal);
        mUserModel = mFireBaseHelper.getMyUserModel();
        mSettingsModel = mFireBaseHelper.getSettingsModel();


        setupActionBar(rootView.findViewById(R.id.action_bar));
        setUpViews();
    }

    private void setUpViews() {
        if (mUserModel != null) {
            // TODO check for birthday -- if available from fb profile dont let user edit
        }

        dateArr = new String[3];

        if (mSettingsModel != null) {
            if (mSettingsModel.birthday != null && !mSettingsModel.birthday.isEmpty()) {
                birthday.setText(mSettingsModel.birthday);
                dateArr = mSettingsModel.birthday.split("/");
                dateArr[1] = String.valueOf(Integer.valueOf(dateArr[1]) - 1);

            } else {
                birthday.setHint("Update Birthday");
                dateArr[0] = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                dateArr[1] = String.valueOf(Calendar.getInstance().get(Calendar.MONTH));
                dateArr[2] = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            }
            birthdayReveal.setChecked(mSettingsModel.birthdayPartyFlag);
        } else {
            birthday.setHint("Update Birthday");
            dateArr[0] = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            dateArr[1] = String.valueOf(Calendar.getInstance().get(Calendar.MONTH));
            dateArr[2] = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        }


        birthday.setOnClickListener(this);


//        birthdayReveal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//
//            }
//        });

    }

    private void setupActionBar(View actionBar) {
//        actionBar.findViewById(R.id.action_bar_IV_1).setVisibility(View.GONE);
        ImageView back = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        back.setImageResource(R.drawable.back_svg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        ((TextView) actionBar.findViewById(R.id.titleTV)).setText("BIRTHDAY");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(Utils.dp2Px(72));
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();


    }

    public void saveBirthdayDataToFireBase() {
        String s = null;
        if (birthday != null && birthdayReveal != null && !birthday.getText().toString().trim().isEmpty()) {
            s = birthday.getText().toString();
            mFireBaseHelper.updateBirthdayInFireBase(birthday.getText().toString(), birthdayReveal.isChecked());
        }
        if (s != null) {
            ((CameraActivity) getActivity()).updateBirthdayOnSettingsFragment(s);
        }
    }

    @Override
    public void onDestroyView() {
        if (mSettingsModel != null && birthday.getText() != null && !birthday.getText().toString().equals(mSettingsModel.birthday))
            saveBirthdayDataToFireBase();
        super.onDestroyView();
    }


    @Override
    public void onClick(View v) {
        if(mSettingsModel!=null && !mSettingsModel.facebookBirthday){
            DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    monthOfYear++;
                    String str = dayOfMonth + "/" + monthOfYear + "/" + year;
                    birthday.setText(str);
                }
            },
                    Integer.valueOf(dateArr[2]),
                    Integer.valueOf(dateArr[1]),
                    Integer.valueOf(dateArr[0]));

            Calendar c = Calendar.getInstance();
            c.add(Calendar.YEAR, -13);
            datePickerDialog.getDatePicker().setMaxDate(c.getTime().getTime());
            datePickerDialog.setTitle(null);

            datePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (mSettingsModel != null && mSettingsModel.birthday != null) {
                        birthday.setText(mSettingsModel.birthday);
                    } else {
                        birthday.setText("");
                    }
                }
            });

            datePickerDialog.show();
        } else {
            showCustomToast(getActivity(), R.layout.only_text_toast, Gravity.BOTTOM | Gravity.CENTER, 0, 100, 2000, "Please change your birthday on Facebook");
        }
    }
}
