package com.pulseapp.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.adapters.CustomGridAdapter;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BlurBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by deepankur on 09-12-2015.
 */
public class OnBoardingCategoriesFragment extends BaseFragment implements View.OnClickListener {

    private Target backgroundTarget;
    private RelativeLayout backgroundLayout;
    private SharedPreferences prefs;
    private static TextView numberOfCategoriesPickedTv;
    GridView gridView;
    private static Boolean isMinimumCategoriesSelected = false;

    public OnBoardingCategoriesFragment() {
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_onboarding_categories, container, false);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        setFonts(rootView);
        gridView = (GridView) rootView.findViewById(R.id.gridView);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        setFonts(rootView);
        //  initializeGridView();

        prefs = getActivity().getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        numberOfCategoriesPickedTv = (TextView) rootView.findViewById(R.id.nextPageTV);
        numberOfCategoriesPickedTv.setOnClickListener(this);
        (rootView.findViewById(R.id.nextPageLL)).setOnClickListener(this);

        backgroundLayout = ((RelativeLayout) rootView.findViewById(R.id.mainLayoutRL));
        backgroundTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                backgroundLayout.setBackgroundDrawable(new BitmapDrawable(BlurBuilder.blur(getActivity(), bitmap)));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.USER_PROFILE_PIC_URL, "")) != null)
            Picasso.with(getActivity()).load(prefs.getString(AppLibrary.USER_PROFILE_PIC_URL, "")).into(backgroundTarget);
//        fetchCategoriesFromServer();
        rootView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        return rootView;
    }

    private void setFonts(View rootView) {
        Typeface latoBold;
        latoBold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Lato-Bold.ttf");
        ((TextView) rootView.findViewById(R.id.preferenceTV)).setTypeface(latoBold);
    }

    private void initializeGridView(List<nameResource> list) {

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inDither = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inScaled = false;
        String[] categoryNames = new String[list.size()];
        String[] serverUniqueId = new String[list.size()];
        int[] resourceIds = new int[list.size()];
        Bitmap[] bitmapArray = new Bitmap[list.size()];
        for (int i = 0; i < list.size(); i++) {
            resourceIds[i] = list.get(i).getResourceId();
            categoryNames[i] = list.get(i).getCategoryName();
            serverUniqueId[i] = list.get(i).getServerUniqueId();
            Bitmap bitmapImage = BitmapFactory.decodeResource(getActivity().getResources(),
                    resourceIds[i], opts);
            bitmapArray[i] = bitmapImage;
        }
        CustomGridAdapter gridAdapter = new CustomGridAdapter(getActivity(), bitmapArray, categoryNames, serverUniqueId);
        gridView.setAdapter(gridAdapter);

    }


    public static void validateNumberOfCategories(int totalCategories, Context context) {
//        Drawable img = context.getResources().getDrawable(R.drawable.arrow_result);

        if (totalCategories < 3) {
            String category = (3 - totalCategories) > 1 ? " more categories" : " more category";
            numberOfCategoriesPickedTv.setText("Pick " + String.valueOf(3 - totalCategories) + category);
            numberOfCategoriesPickedTv.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            isMinimumCategoriesSelected = false;

        } else {
            numberOfCategoriesPickedTv.setText("Awesome! Take me to the dashboard  ");
//            numberOfCategoriesPickedTv.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
            isMinimumCategoriesSelected = true;
        }
    }


    @Override
    public void onClick(View v) {
        if (!isInternetAvailable(true))
            return;
        if (isMinimumCategoriesSelected) {

            String categoriesSelected = "";
            for (int i = 0; i < CustomGridAdapter.categoryImageModels.length; i++) {
                if (CustomGridAdapter.categoryImageModels[i].getIsChecked())
                    categoriesSelected += CustomGridAdapter.categoryImageModels[i].getServerUID() + ",";
            }
//            uploadCategoriesToServer(categoriesSelected.substring(0, categoriesSelected.length() - 1));
        } else showShortToastMessage("Select at least three categories");
    }


//    private void uploadCategoriesToServer(String s) {
//        List<NameValuePair> pairs = new ArrayList<>();
//        pairs.add(new BasicNameValuePair("tags", s));
//        pairs.add(new BasicNameValuePair("login_step", "2"));
//        RequestManager.makePostRequest(getActivity(), RequestManager.POST_CATEGORIES_REQUEST,
//                RequestManager.POST_CATEGORIES_RESPONSE, null, pairs, postCategoriesCallback);
//    }

    private RequestManager.OnRequestFinishCallback postCategoriesCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
//            AppLibrary.log_d("OnBoardingCategoriesFragment", response.toString());
            if (success) {
                JSONObject object = (JSONObject) response;
                try {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        Intent intent = new Intent(getActivity(), CameraActivity.class);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                //request error
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };

    public class nameResource {
        private String categoryName;
        private String serverUniqueId;
        private int resourceId;

        public nameResource(String categoryName, String serverUniqueId, int resourceId) {

            this.categoryName = categoryName;
            this.serverUniqueId = serverUniqueId;
            this.resourceId = resourceId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getServerUniqueId() {
            return serverUniqueId;
        }

        public void setServerUniqueId(String serverUniqueId) {
            this.serverUniqueId = serverUniqueId;
        }

        public int getResourceId() {
            return resourceId;
        }

        public void setResourceId(int resourceId) {
            this.resourceId = resourceId;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

//    private void fetchCategoriesFromServer() {
//        RequestManager.makeGetRequest(getActivity(), RequestManager.FETCH_CATEGORIES_REQUEST, RequestManager.FETCH_CATEGORIES_RESPONSE
//                , null, fetchCategoriesCallback);
//    }

    private RequestManager.OnRequestFinishCallback fetchCategoriesCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
//            AppLibrary.log_d("asdf", response.toString());
            if (success) {
                JSONObject object = (JSONObject) response;
                try {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        JSONArray values = object.getJSONArray("value");
                        List<nameResource> list = new ArrayList<>();
                        for (int i = 0; i < values.length(); i++) {
                            JSONObject categoryObjectt = values.getJSONObject(i);
                            list.add(new nameResource(categoryObjectt.getString("name"),
                                    categoryObjectt.getString("_id"),
                                    categoryNameToResourceId(categoryObjectt.getString("_id"))));
                        }
                        initializeGridView(list);

                    } else {

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

    private int categoryNameToResourceId(String categoryName) {
        int resourceId = 0;
        switch (categoryName) {
            case "56b47b0822e6bd0c3e02ac6d":
//                resourceId = R.drawable.food_result;
                break;
            case "56b47b0822e6bd0c3e02ac6e":
//                resourceId = R.drawable.humour_result;
                break;
            case "56b47b0822e6bd0c3e02ac6f":
//                resourceId = R.drawable.fashion_result;
                break;
            case "56b47b0822e6bd0c3e02ac70":
//                resourceId = R.drawable.lifestyle_result;
                break;
            case "56b47b0822e6bd0c3e02ac71":
//                resourceId = R.drawable.technology_result;
                break;
            case "56b47b0822e6bd0c3e02ac74":
//                resourceId = R.drawable.sports_result;
                break;
            case "56b47b0822e6bd0c3e02ac72":
//                resourceId = R.drawable.music_result;
                break;
            case "56b47b0822e6bd0c3e02ac73":
//                resourceId = R.drawable.fitness_result;
                break;
        }
        return resourceId;
    }
}
