package com.pulseapp.android.customViews;

/**
 * Created by deepankur on 6/8/16.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.StickerCategoryModel;
import com.squareup.picasso.Picasso;

import java.io.File;


public class EmojiPagerTab extends HorizontalScrollView implements FireBaseKEYIDS {

    private static final int[] ATTRS = new int[]{
            android.R.attr.textSize,
            android.R.attr.textColor
    };
    private Context context;
    private LinearLayout.LayoutParams defaultTabLayoutParams;
    private final PageListener pageListener = new PageListener();
    private LinearLayout tabsContainer;
    private ViewPager pager;
    private int tabCount;
    private int currentPosition = 0;
    private float currentPositionOffset = 0f;

    private Paint rectPaint;
    private Paint dividerPaint;
    private int indicatorColor = Color.parseColor("#00FFFFFF");
    private int underlineColor = Color.parseColor("#00FFFFFF");
    private int dividerColor = Color.parseColor("#00FFFFFF");

    private boolean shouldExpand = true;
    private boolean textAllCaps = true;

    private int scrollOffset = 52;
    private int indicatorHeight = 2;
    private int underlineHeight = 2;
    private int dividerPadding = 0;
    private int tabPadding = 10;
    private int dividerWidth = 0;

    private int tabTextSize = 14;
    private int tabTextColor = Color.parseColor("#FFFFFF");

    private int lastScrollX = 0;

    private int tabBackgroundResId = R.drawable.background_tab;
    private InstaSlidingPanel instaSlidingPanel;
    private final String TAG = getClass().getSimpleName();

    public EmojiPagerTab(Context context) {
        this(context, null);
        this.context = context;
    }

    public EmojiPagerTab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public EmojiPagerTab(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;

        setFillViewport(true);
        setWillNotDraw(false);

        tabsContainer = new LinearLayout(context);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        tabsContainer.setPadding(0, 0, 0, 0);
        addView(tabsContainer);

        DisplayMetrics dm = getResources().getDisplayMetrics();

        scrollOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
        indicatorHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
        underlineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
        dividerPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
        tabPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
        dividerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
        tabTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);

        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);

        tabTextSize = a.getDimensionPixelSize(0, tabTextSize);
//        tabTextColor = a.getColor(1, tabTextColor);
        rectPaint = new Paint();
        rectPaint.setAntiAlias(true);
        rectPaint.setStyle(Style.FILL);

        dividerPaint = new Paint();
        dividerPaint.setAntiAlias(true);
        dividerPaint.setStrokeWidth(dividerWidth);
    }

    public void setViewPager(ViewPager pager) {
        this.pager = pager;
        if (pager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        pager.addOnPageChangeListener(pageListener);
        notifyDataSetChanged();
    }

    StickerCategoryModel[] categories;

    public void setCategoryModelsList(StickerCategoryModel[] categories) {
        this.categories = categories;
    }

    public void notifyDataSetChanged() {
        tabsContainer.removeAllViews();
        tabCount = pager.getAdapter().getCount();
        for (int i = 0; i < tabCount; i++) {
            addTextTab(i, categories[i]);
        }
        updateTabStyles();
        changeTabsColor(0);
    }

    private void addTextTab(final int position, final StickerCategoryModel stickerCategory) {

        /*LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(position);
                changeTabsColor(position);
            }
        });
        TextView tab = new TextView(getContext());
        tab.setText(stickerCategory.title);
        tab.setFocusable(true);
        tab.setGravity(Gravity.CENTER);

        ImageView imageView = new ImageView(getContext());
        if (stickerCategory.imagePresentInAssets) {
            //// TODO: 6/8/16
        } else {
            Picasso.with(context).load(new File(stickerCategory.localUri)).into(imageView);
        }*/

        View view = LayoutInflater.from(getContext()).inflate(R.layout.emoji_pager_tab, null);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(position);
                changeTabsColor(position);
            }
        });
        TextView textView = (TextView) view.findViewById(R.id.categoryTV);
        textView.setText(stickerCategory.title);
        textView.setFocusable(true);
        textView.setGravity(Gravity.CENTER);
        ImageView imageView = (ImageView) view.findViewById(R.id.categoryIV);
        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        if (stickerCategory.imagePresentInAssets) {
            Picasso.with(context).load(stickerCategory.localUri).into(imageView);
        } else {
            if (stickerCategory.localUri != null && new File(stickerCategory.localUri).exists()) {
                Picasso.with(context).load(new File(stickerCategory.localUri)).into(imageView,
                        new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "onSuccess for sticker with Id " + stickerCategory.localUri);

                            }

                            @Override
                            public void onError() {
                                Log.d(TAG, "onError for sticker with Id " + stickerCategory.localUri);
                            }
                        });
            } else {
                Log.d(TAG, "addTextTab: " + stickerCategory.title + " " + stickerCategory.localUri);
            }
        }
        tabsContainer.addView(view);
    }

    private void changeTabsColor(int position) {
        for (int i = 0; i < tabsContainer.getChildCount(); i++) {
            if (i == position)
                tabsContainer.getChildAt(i).setAlpha(1);
            else
                tabsContainer.getChildAt(i).setAlpha(0.3f);
        }
    }

    final int NUMBER_OF_TILES_IN_SCREEN = 5;

    private void updateTabStyles() {

        int width = 50;
        if (tabCount > 0) {//this was earlier two instead of 0; this gives each tab 1/3rd of the screen width.
            width = context.getResources().getConfiguration().screenWidthDp / NUMBER_OF_TILES_IN_SCREEN;
            defaultTabLayoutParams = new LinearLayout.LayoutParams(getDIP(context, width), LayoutParams.MATCH_PARENT);
        } else {
            defaultTabLayoutParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        }
        for (int i = 0; i < tabCount; i++) {
            View v = tabsContainer.getChildAt(i);
            v.setLayoutParams(defaultTabLayoutParams);
            v.setBackgroundResource(tabBackgroundResId);
            if (shouldExpand) {
//                v.setPadding(0, 15, 0, 5);
                v.setPadding(0, 0, 0, 0);
            } else {
                v.setPadding(tabPadding, 0, tabPadding, 0);
            }

            if (v instanceof TextView) {
                TextView tab = (TextView) v;
                tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
                tab.setTextColor(tabTextColor);
            }
        }

    }

    private void scrollToChild(int position, int offset) {
        int newScrollX = tabsContainer.getChildAt(position).getLeft() + offset;
        if (position > 0 || offset > 0) {
            newScrollX -= scrollOffset;
        }
        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX;
            //scrollTo(newScrollX, 0);
            smoothScrollTo(newScrollX, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode() || tabCount == 0) {
            return;
        }

        final int height = getHeight();
        rectPaint.setColor(indicatorColor);
        View currentTab = tabsContainer.getChildAt(currentPosition);
        float lineLeft = currentTab.getLeft();
        float lineRight = currentTab.getRight();
        if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {

            View nextTab = tabsContainer.getChildAt(currentPosition + 1);
            final float nextTabLeft = nextTab.getLeft();
            final float nextTabRight = nextTab.getRight();

            lineLeft = (currentPositionOffset * nextTabLeft + (1f - currentPositionOffset) * lineLeft);
            lineRight = (currentPositionOffset * nextTabRight + (1f - currentPositionOffset) * lineRight);
        }

        canvas.drawRect(lineLeft, height - indicatorHeight, lineRight, height, rectPaint);
        rectPaint.setColor(underlineColor);
        dividerPaint.setColor(dividerColor);
        for (int i = 0; i < tabCount - 1; i++) {
            View tab = tabsContainer.getChildAt(i);
            canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(), height - dividerPadding, dividerPaint);
        }
    }


    private class PageListener implements OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            currentPosition = position;
            currentPositionOffset = positionOffset;
            scrollToChild(position, (int) (positionOffset * tabsContainer.getChildAt(position).getWidth()));//// FIXME: 6/10/16
            invalidate();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageSelected(int position) {
            changeTabsColor(position);
        }
    }

    public static int getDIP(Context context, int value) {
        Resources r = context.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
        return px;
    }
}

