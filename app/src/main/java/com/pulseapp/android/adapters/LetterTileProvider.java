package com.pulseapp.android.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Xfermode;
import android.text.TextPaint;

import com.pulseapp.android.R;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;

/**
 * Created by Karthik on 9/8/15.
 */
//public class LetterAvatar extends ColorDrawable {
//    Paint paint   = new Paint();
//    Rect bounds  = new Rect();
//
//    String              pLetters;
//    private float       ONE_DP  = 0.0f;
//    private Resources pResources;
//    private int         pPadding;
//    int                 pSize   = 0;
//    float               pMesuredTextWidth;
//
//    int                 pBoundsTextwidth;
//    int                 pBoundsTextHeight;
//
//    public LetterAvatar (Context context, int color, String letter, int paddingInDp) {
//        super(color);
//        this.pLetters = letter;
//        this.pResources = context.getResources();
//        ONE_DP = 1 * pResources.getDisplayMetrics().density;
//        this.pPadding = Math.round(paddingInDp * ONE_DP);
//    }
//
//    @Override
//    public void draw(Canvas canvas) {
//        super.draw(canvas);
//        paint.setAntiAlias(true);
//
//        do {
//            paint.setTextSize(++pSize);
//            paint.getTextBounds(pLetters, 0, pLetters.length(), bounds);
//
//        } while ((bounds.height() < (canvas.getHeight() - pPadding)) && (paint.measureText(pLetters) < (canvas.getWidth() - pPadding)));
//
//        paint.setTextSize(pSize);
//        pMesuredTextWidth = paint.measureText(pLetters);
//        pBoundsTextHeight = bounds.height();
//
//        float xOffset = ((canvas.getWidth() - pMesuredTextWidth) / 2);
//        float yOffset = (int) (pBoundsTextHeight + (canvas.getHeight() - pBoundsTextHeight) / 2);
//        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
//        paint.setColor(0xffffffff);
//        canvas.drawText(pLetters, xOffset, yOffset, paint);
//    }
//}


public class LetterTileProvider {

    /**
     * The number of available tile colors (see R.array.letter_tile_colors)
     */
    private static final int NUM_OF_TILE_COLORS = 8;

    /**
     * The {@link TextPaint} used to draw the letter onto the tile
     */
    private final TextPaint mPaint = new TextPaint();
    /**
     * The bounds that enclose the letter
     */
    private final Rect mBounds = new Rect();
    /**
     * The {@link Canvas} to draw on
     */
    private final Canvas mCanvas = new Canvas();
    /**
     * The first char of the name being displayed
     */
    private final char[] mFirstChar = new char[1];

    /**
     * The background colors of the tile
     */
    private final TypedArray mColors;
    /**
     * The font size used to display the letter
     */
    private final int mTileLetterFontSize;
    /**
     * The default image to display
     */
//    private final Bitmap mDefaultBitmap;

    /**
     * Constructor for <code>LetterTileProvider</code>
     *
     * @param context The {@link Context} to use
     */
    Context context;
    public LetterTileProvider(Context context) {
        final Resources res = context.getResources();
        this.context=context;

        mPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mPaint.setColor(Color.WHITE);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);

        mColors = res.obtainTypedArray(R.array.letter_tile_colors);
        mTileLetterFontSize = res.getDimensionPixelSize(R.dimen.tile_letter_font_size);

//        mDefaultBitmap = BitmapFactory.decodeResource(res, R.drawable.profile_thumbnail);
    }

    /**
     * @param displayname The name used to create the letter for the tile
     * @param key         The key used to generate the background color for the tile
     * @param width       The desired width of the tile
     * @param height      The desired height of the tile
     * @return A {@link Bitmap} that contains a letter used in the English
     * alphabet or digit, if there is no letter or digit available, a
     * default image is shown instead
     */
    public Bitmap getLetterTile(String displayname, String key, int width, int height) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final char firstChar;
        String displayName = displayname.trim();
        if (AppLibrary.checkStringObject(displayName) != null && displayName.length() > 0)
            firstChar = displayName.charAt(0);
        else
            firstChar = 'Y';

        if (AppLibrary.checkStringObject(key) == null)
            key = "Name";

        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        final Canvas c = mCanvas;
        c.setBitmap(bitmap);
        c.drawARGB(0, 0, 0, 0);
        mPaint.setColor(pickColor(key));
        c.drawOval(rectF, mPaint);
        Xfermode mode = mPaint.getXfermode();
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        if (isEnglishLetterOrDigit(firstChar)) {
            mPaint.setColor(Color.WHITE);
            mFirstChar[0] = Character.toUpperCase(firstChar);
            mPaint.setTextSize(mTileLetterFontSize);
            mPaint.setTypeface(FontPicker.getInstance(context).getMontserratRegular());
            mPaint.getTextBounds(mFirstChar, 0, 1, mBounds);
            c.drawText(mFirstChar, 0, 1, 0 + width / 2, 0 + height / 2
                    + (mBounds.bottom - mBounds.top) / 2, mPaint);
        } else {
//            c.drawBitmap(mDefaultBitmap, null, rectF, null);
        }
        mPaint.setXfermode(mode);
        return bitmap;
    }

    /**
     * @param c The char to check
     * @return True if <code>c</code> is in the English alphabet or is a digit,
     * false otherwise
     */
    private static boolean isEnglishLetterOrDigit(char c) {
        return 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || '0' <= c && c <= '9';
    }

    /**
     * @param key The key used to generate the tile color
     * @return A new or previously chosen color for <code>key</code> used as the
     * tile background color
     */
    private int pickColor(String key) {
        // String.hashCode() is not supposed to change across java versions, so
        // this should guarantee the same key always maps to the same color
        final int color = Math.abs(key.hashCode()) % NUM_OF_TILE_COLORS;
        try {
            return mColors.getColor(color, Color.BLACK);
        } catch (Exception e) {
            AppLibrary.log_d("Letter Tile", "Exception in mColours");
            return 0;
        }
//        } finally {
//            mColors.recycle();
//        }
    }
}
