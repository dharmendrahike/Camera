package com.pulseapp.android.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.regions.Regions;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.services.UpdateProfileService;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.ColoredRoundedTransformation;
import com.squareup.picasso.Target;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;


/**
 * Created by deepankur on 08-12-2015.
 */
public class EditProfileFragment extends BaseFragment {

    private static BitmapDrawable background;
    private static int RESULT_LOAD_IMG = 1;
    private final static String TAG = "Edit Profile";

    private ImageView defaultIV;
    private RelativeLayout backgoundBlurLL;
    private String imgDecodableString;
    private String mUploadProfilePicPath = "";
    private SharedPreferences prefs;
    private TransferManager transferManager;
    private String newProfilePictureUrl = "";
    private ImageView blurredImageView;
    private EditText userNameEditText, bioEditText;
    private Target profileTarget;

    public EditProfileFragment() {
    }


    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getActivity().getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity().getApplicationContext(),    // get the context for the current activity
                "us-east-1:5d48bd03-d736-4a6c-9f0b-c4761abe73f5",    /* Identity Pool ID */
                Regions.US_EAST_1           /* Region for your identity pool*/
        );
        transferManager = new TransferManager(credentialsProvider);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_profile, container, false);
        defaultIV = (ImageView) rootView.findViewById(R.id.defaultIV);
//        backgoundBlurLL = (RelativeLayout) rootView.findViewById(R.id.background_layout);
//        blurredImageView = (ImageView) rootView.findViewById(R.id.blurredIV);
        userNameEditText = ((EditText) rootView.findViewById(R.id.nameTV));
        bioEditText = ((EditText) rootView.findViewById(R.id.aboutMeTV));
//        bioEditText.setEnabled(false);
        bioEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        bioEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(42)});
        bioEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                AppLibrary.log_i("Key Event", "Value: " + actionId + "--->"/* + event.getAction()*/);
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
                    AppLibrary.hideKeyboard(getActivity(), bioEditText);
                    bioEditText.setCursorVisible(false);
                    return true;
                }
                return false;
            }
        });


        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
//            f.set(bioEditText, R.drawable.edittext_cursor);
        } catch (Exception ignored) {
        }


        userNameEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        userNameEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(42)});
        userNameEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                AppLibrary.log_i("Key Event", "Value: " + actionId + "--->"/* + event.getAction()*/);
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
                    AppLibrary.hideKeyboard(getActivity(), userNameEditText);
                    userNameEditText.setCursorVisible(false);
                    return true;
                }
                return false;
            }
        });

        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
//            f.set(userNameEditText, R.drawable.edittext_cursor);
        } catch (Exception ignored) {
        }

        ((ImageView) rootView.findViewById(R.id.defaultIV)).setOnClickListener(optionCLickListener);
        ((TextView) rootView.findViewById(R.id.nextPageTv)).setOnClickListener(optionCLickListener);
        rootView.findViewById(R.id.btn_edit_profile_image).setOnClickListener(optionCLickListener);

        ((LinearLayout) rootView.findViewById(R.id.bottomLayout)).setOnClickListener(optionCLickListener);
        setFonts(rootView);
        initializeViews();
        return rootView;
    }

    private void setFonts(View rootView) {
        Typeface latoBold, latoRegular, latoLight;
        latoBold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Lato-Bold.ttf");
        latoRegular = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Lato-Regular.ttf");
        latoLight = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Lato-Light.ttf");
        ((TextView) rootView.findViewById(R.id.gettingReadyTV)).setTypeface(latoBold);
        ((TextView) rootView.findViewById(R.id.nameTV)).setTypeface(latoRegular);
        ((TextView) rootView.findViewById(R.id.aboutMeTV)).setTypeface(latoLight);
    }

    public void initializeViews() {
        AppLibrary.log_d(TAG, "Picture url -" + prefs.getString(AppLibrary.USER_PROFILE_PIC_URL, ""));
//        if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.USER_PROFILE_PIC_URL, "")) != null)
////            Picasso.with(getActivity()).load(prefs.getString(AppLibrary.USER_PROFILE_PIC_URL, ""))
////                    .fit().centerCrop().placeholder(R.drawable.profile_thumbnail)
////                    .transform(new ColoredRoundedTransformation("white", 8))
////                    .into(defaultIV);
//        else {
////            defaultIV.setImageResource(R.drawable.profile_thumbnail);
//        }
        if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.FACEBOOK_BIO_INFO, "")) != null) {
            bioEditText.setText(prefs.getString(AppLibrary.FACEBOOK_BIO_INFO, ""));
        } else {
            bioEditText.setVisibility(View.GONE);
        }
        if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.USER_NAME, "")) != null) {
            userNameEditText.setText(prefs.getString(AppLibrary.USER_NAME, ""));
        } else {
            userNameEditText.setText("Your Name");
        }
    }

    protected View.OnClickListener optionCLickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.nameTV:
                    focusEditText(userNameEditText);
                    break;
                case R.id.aboutMeTV:
                    focusEditText(bioEditText);
                    break;
                case R.id.btn_edit_profile_image:
                case R.id.defaultIV:
                    newGalleryUpload();
                    break;
                case R.id.nextPageTv:
                case R.id.bottomLayout:
                    launchFragments();
                    break;
            }
        }
    };

    private void focusEditText(EditText editText) {
        AppLibrary.log_d(TAG, "edit text clicked");
        editText.requestFocus();
        editText.setCursorVisible(true);
        editText.setFocusable(true);
        if(editText.length() > 0)
            editText.setSelection(editText.length());
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == Activity.RESULT_OK
                    && null != data) {
                // Get the Image from data

                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                // Get the cursor
                Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgDecodableString = cursor.getString(columnIndex);
                cursor.close();

                Bitmap b = BitmapFactory.decodeFile(imgDecodableString);
                Bitmap out = null;
                int startX = 0;
                int startY = 0;
                int side = 512;
                if (b.getByteCount() > 100 * 1024 || (b.getWidth() > 512 && b.getHeight() > 512)) {
                    int dx, dy;
                    if (b.getWidth() > b.getHeight()) {
                        dy = 512;
                        dx = b.getWidth() * 512 / b.getHeight();
                    } else {
                        dx = 512;
                        dy = b.getHeight() * 512 / b.getWidth();
                    }
                    b = Bitmap.createScaledBitmap(b, dx, dy, false);
                    AppLibrary.log_d("ImgCompress", "Scale:: " + b.getWidth() + " " + b.getHeight());
                }
                if (b.getWidth() != b.getHeight()) {
                    AppLibrary.log_d("ImgCompress", "b.getWidth()!=b.getHeight():: " + b.getWidth() + " " + b.getHeight());
                    if (b.getWidth() > b.getHeight()) {
                        side = b.getHeight();
                        startX = (b.getWidth() - side) / 2;
                    } else {
                        side = b.getWidth();
                        startY = (b.getHeight() - side) / 2;
                    }
                }
                if ((startX != 0 || startY != 0) && side != 0) {
                    out = Bitmap.createBitmap(b, startX, startY, side, side);
                    AppLibrary.log_d("ImgCompress", "(startX!=0||startY!=0)&&side!=0:: " + out.getWidth() + " " + out.getHeight());
                } else
                    out = b;
                if (out != null) {
                    File file = new File(getActivity().getApplicationContext().getFilesDir(), "avatar.jpg");
                    FileOutputStream fOut;
                    try {
                        fOut = new FileOutputStream(file);
                        out.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
                        fOut.flush();
                        fOut.close();
                        b.recycle();
                        out.recycle();
                        AppLibrary.log_d("ImgCompress", "File size:: " + file.length() / 1024);
                        imgDecodableString = getActivity().getApplicationContext().getFilesDir() + "/avatar.jpg";
                        mUploadProfilePicPath = imgDecodableString;
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Unable to set picked Image",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    b.recycle();
                }
                defaultIV.setImageBitmap(new ColoredRoundedTransformation("white", 5).transform(BitmapFactory.decodeFile(imgDecodableString)));
                if (isInternetAvailable(true))
                    checkForProfilePicUpload();
            } else {
                Toast.makeText(getActivity(), "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            AppLibrary.log_d(TAG, String.valueOf(e));
            Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void checkForProfilePicUpload() {
        String uploadProfilePicPath = "";
        uploadProfilePicPath = mUploadProfilePicPath;
        if (uploadProfilePicPath != null && !uploadProfilePicPath.isEmpty()) {
            getActivity().getIntent().putExtra("UPLOAD_PROFILE_PIC_PATH", "");
            AppLibrary.log_d("shwstppr", "UPLOAD_PROFILE_PIC_PATH:: " + uploadProfilePicPath);
            String userID = prefs.getString(AppLibrary.USER_LOGIN, null);
            if (userID != null && !userID.isEmpty()) {
                File file = new File(uploadProfilePicPath);
                TransferObserver upload = MasterClass.getTransferUtility().upload("instalively.data", "profile/" + userID + ".jpg", file);

                upload.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int i, TransferState transferState) {
                        AppLibrary.log_e(TAG, "On State changed" + transferState.toString());

                        if (transferState == TransferState.COMPLETED) {
                            newProfilePictureUrl = "profile/" + prefs.getString(AppLibrary.USER_LOGIN, null) + ".jpg";
//                            updateUserRequest(newProfilePictureUrl,null,null);
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        AppLibrary.log_e(TAG, "On Progress changed: " + bytesCurrent + "out of " + bytesTotal);
                    }

                    @Override
                    public void onError(int i, Exception e) {
                        AppLibrary.log_e(TAG, "Some S3Client error while uploading profile pic: " + e.getMessage());
                    }
                });
//            if (transferManager != null && userID != null && !userID.isEmpty()) {
//                File file = new File(uploadProfilePicPath);
//                final Upload upload = this.transferManager.upload("instalively.images", "profile/" + userID + ".jpg", file);
//                Thread s3UploadThread = new Thread() {
//                    @Override
//                    public void run() {
//                        try {
//                            upload.waitForCompletion();
//                            AppLibrary.log_d("S3Upload", "UPLOAD complete!");
//                            newProfilePictureUrl = RequestManager.S3_IMAGE_BUCKET_PATH_PREFIX +
//                                    "profile/" + prefs.getString(AppLibrary.USER_LOGIN, null) +
//                                    ".jpg";
//                            updateUserRequest(newProfilePictureUrl, null, null);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                            AppLibrary.log_e("S3Upload", "UPLOAD error: " + e);
//                            upload.abort();
//                        }
//                    }
//                };
//                s3UploadThread.start();
            } else {
                AppLibrary.log_e(TAG, "Unable to upload user seleted profile picture!");
            }
        }
    }

//    private void updateUserRequest(String userProfilePictureUrl, String userName, String bio) {
//        List<NameValuePair> pairs = new ArrayList<>();
//        if (userProfilePictureUrl != null) {
//            pairs.add(new BasicNameValuePair("[user][pictureUrl]", userProfilePictureUrl));
//            pairs.add(new BasicNameValuePair("bucket", "instalively.data"));
//        }
//        if (userName != null)
//            pairs.add(new BasicNameValuePair("[user][fullName]", userName));
//        if (bio != null)
//            pairs.add(new BasicNameValuePair("[user][bio]", bio));
//        RequestManager.makePostRequest(getActivity(), RequestManager.USER_PROFILE_PICTURE_UPDATE_REQUEST, RequestManager.USER_PROFILE_PICTURE_UPDATE_RESPONSE, null, pairs, userProfileUpdateCallback);
//    }

    private RequestManager.OnRequestFinishCallback userProfileUpdateCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            if (success) {
                JSONObject object = (JSONObject) response;
                try {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        AppLibrary.log_d("shwstppr", "User Profile Update!");
                        prefs.edit().putString(AppLibrary.USER_PROFILE_PIC_URL, RequestManager.S3_IMAGE_BUCKET_PATH_PREFIX + newProfilePictureUrl).commit();
                    } else {
                        //do nothing
                        AppLibrary.log_e("shwstppr", "Error updating user profile!" + object.getString("value"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                //request error
                AppLibrary.log_e("shwstppr", "Request failed");
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }

    };

    private void launchFragments() {
        if (!userNameEditText.getText().toString().equals(prefs.getString(AppLibrary.USER_NAME, ""))
                || !bioEditText.getText().toString().equals(prefs.getString(AppLibrary.FACEBOOK_BIO_INFO, ""))) {
            Intent intent = new Intent(getActivity(), UpdateProfileService.class);
            if (!userNameEditText.getText().toString().equals(prefs.getString(AppLibrary.USER_NAME, "")))
                intent.putExtra("updatedUserName", userNameEditText.getText().toString());
            if (!bioEditText.getText().toString().equals(prefs.getString(AppLibrary.FACEBOOK_BIO_INFO, "")))
                intent.putExtra("updateBioInfo", bioEditText.getText().toString());
            getActivity().startService(intent);
        }
        Fragment fragment = new OnBoardingCategoriesFragment();
        FragmentManager fragmentManager;
        FragmentTransaction fragmentTransaction;
        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment, "EditProfile");
//        fragmentTransaction.addToBackStack("EditProfile");
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void newGalleryUpload() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{galleryIntent});
        startActivityForResult(chooserIntent, RESULT_LOAD_IMG);
    }

    public static BitmapDrawable getBackgroundImage() {
        return background;
    }

}