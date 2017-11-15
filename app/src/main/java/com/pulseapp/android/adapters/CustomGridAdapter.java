package com.pulseapp.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.OnBoardingCategoriesFragment;
import com.pulseapp.android.util.AppLibrary;

import models.CategoryImageModel;


/**
 * Created by deepankur on 16-12-2015.
 */
public class CustomGridAdapter extends BaseAdapter {

    private boolean isTagset = false;
    private Bitmap bitmapChecked;
    private Context context;
    private Bitmap[] bitmaps;
    private LayoutInflater inflater;
    private Bitmap transparentBitmap;
    public static CategoryImageModel[] categoryImageModels;
    private String[] categoryName;
    private String[] serverUID;

    public CustomGridAdapter(Context context, Bitmap[] bitmaps, String[] categoryName, String[] serverUID) {
        this.context = context;
        this.bitmaps = bitmaps;
        this.categoryName = categoryName;
        this.serverUID = serverUID;
        inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        bitmapChecked = BaseFragment.getRoundedCornerBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.tint_with_tick_result), 10);
//        transparentBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.transparent_check_result);
        categoryImageModels = new CategoryImageModel[bitmaps.length];

    }


    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.cell, null);
        }

        ImageView imageViewBackground = (ImageView) convertView.findViewById(R.id.categoryImageBackground);
        imageViewBackground.setImageBitmap(BaseFragment.getRoundedCornerBitmap((bitmaps[position]), 10));

        TextView categoryNameTv = (TextView) convertView.findViewById(R.id.categoryNameTV);

        Typeface latoRegular= Typeface.createFromAsset(context.getAssets(), "fonts/Lato-Regular.ttf");
        categoryNameTv.setTypeface(latoRegular);

        if (AppLibrary.checkStringObject(categoryName[position]) != null) {
            String category_name = categoryName[position].trim();
            categoryNameTv.setText(category_name.toUpperCase());
        }

        ImageView imageViewToggle = (ImageView) convertView.findViewById(R.id.toggleIV);
        imageViewToggle.setOnClickListener(tileClickListener);
        imageViewToggle.setImageBitmap((transparentBitmap));
        if (!isTagset) {
            categoryImageModels[position] = new CategoryImageModel(position, categoryName[position], false, serverUID[position]);
            imageViewToggle.setTag(categoryImageModels[position]);
        }

      //  Log.d("bbbb", "" + ((CategoryImageModel) imageViewToggle.getTag()).getIndex());
        return convertView;
    }

    @Override
    public int getCount() {
        return bitmaps.length;
    }

    @Override
    public Object getItem(int position) {
        return bitmaps[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private View.OnClickListener tileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            isTagset = true;
            CategoryImageModel categoryImageModel = (CategoryImageModel) view.getTag();

            if (!categoryImageModel.getIsChecked()) {
                ((ImageView) view).setImageBitmap(bitmapChecked);
                categoryImageModel.setIsChecked(true);
                view.setTag(categoryImageModel);
//                Log.d("asdd", "" + categoryImageModels);

            } else {
                ((ImageView) view).setImageBitmap((transparentBitmap));
                categoryImageModel.setIsChecked(false);
                view.setTag(categoryImageModel);
            }
            updateNumberOfCategories();
        }
    };

    public void updateNumberOfCategories() {
        int categories = 0;
        for (int i = 0; i < categoryImageModels.length; i++) {
            if (categoryImageModels[i].getIsChecked())
                categories++;
        }
        OnBoardingCategoriesFragment.validateNumberOfCategories(categories, context);
//        Log.d("aaaa", "" + categories);
    }
}
