package net.maxsmr.commonutils.gui.drawables;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import net.maxsmr.commonutils.gui.fonts.FontsHolder;

import static net.maxsmr.commonutils.AppUtilsKt.convertAnyToPx;
import static net.maxsmr.commonutils.text.TextUtilsKt.isEmpty;
import static net.maxsmr.commonutils.graphic.GraphicUtilsKt.copyBitmap;
import static net.maxsmr.commonutils.graphic.GraphicUtilsKt.createBitmapFromResource;
import static net.maxsmr.commonutils.graphic.GraphicUtilsKt.fixFontSize;
import static net.maxsmr.commonutils.graphic.GraphicUtilsKt.isBitmapValid;

public class NumberDrawable extends BitmapDrawable {

    private final Context mContext;

    private NumberSetter mSetter;

    private boolean mNeedInvalidate = true;

    private Bitmap mSource = null;

    private Typeface mFont = Defaults.DEFAULT_FONT;

    private int mTextColor = Defaults.DEFAULT_TEXT_COLOR;

    private int mTextAlpha = Defaults.DEFAULT_TEXT_ALPHA;

    private int mNumber = 0;

    private int mTextSizeDP = Defaults.DEFAULT_TEXT_SIZE_DP;

    private final Paint mPaint = new Paint();

    public NumberDrawable setNumberSetter(NumberSetter setter) {
        mSetter = setter;
        return this;
    }

    public NumberDrawable(Context context, @DrawableRes int sourceDrawableResId, @ColorInt int textColor, int textAlpha, String fontAlias) {
        this(context, copyBitmap(createBitmapFromResource(context.getResources(), sourceDrawableResId)), textColor, textAlpha, fontAlias);
    }

    public NumberDrawable(Context context, Bitmap sourceBitmap, @ColorInt int textColor, int textAlpha, String fontAlias) {
        super(context.getResources(), sourceBitmap);
        mContext = context;
        mNeedInvalidate = false;
        setSource(sourceBitmap);
        setTextColor(textColor);
        setTextAlpha(textAlpha);
        setFontByAlias(fontAlias);
        mNeedInvalidate = true;
    }
    /**
     * @param source may be immutable
     */
    private void setSource(Bitmap source) {
        if (mSource != source) {
            if (isBitmapValid(source)) {
                mSource = source;
                if (mNeedInvalidate)
                    invalidateSelf();
            }
        }
    }

    public void setFontByAlias(String alias) {
        if (!isEmpty(alias)) {
            Typeface font = FontsHolder.getInstance().getFont(alias);
            if (!mFont.equals(font)) {
                if (font != null) {
                    mFont = font;
                    if (mNeedInvalidate)
                        invalidateSelf();
                }
            }
        }
    }

    public void setFont(Typeface font) {
        if (mFont != font) {
            if (font != null) {
                mFont = font;
                if (mNeedInvalidate)
                    invalidateSelf();
            }
        }
    }

    private void setTextColor(@ColorInt int textColor) {
        if (mTextColor != textColor) {
            mTextColor = textColor;
            if (mNeedInvalidate)
                invalidateSelf();
        }
    }

    private void setTextAlpha(int textAlpha) {
        if (mTextAlpha != textAlpha) {
            if (textAlpha >= 0 && textAlpha <= 0xFF) {
                mTextAlpha = textAlpha;
                if (mNeedInvalidate)
                    invalidateSelf();
            }
        }
    }

    public void setNumber(int number) {
        if (mNumber != number) {
            mNumber = number;
            if (mNeedInvalidate)
                invalidateSelf();
        }
    }

    public void setTextSizeDp(int textSizeDP) {
        if (mTextSizeDP != textSizeDP) {
            if (textSizeDP > 0) {
                mTextSizeDP = textSizeDP;
                invalidateSelf();
            }
        }
    }

    private int getTextSizeByDensity() {
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
//        int dpi = (int) (displayMetrics.density * DisplayMetrics.DENSITY_MEDIUM);
        return (int) convertAnyToPx(mTextSizeDP, mContext);
    }

    {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public int getAlpha() {
        return super.getAlpha();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (isBitmapValid(mSource)) {
//            canvas.drawBitmap(mSource, 0, 0, mPaint);
            mPaint.setColor(mTextColor);
            mPaint.setAlpha(mTextAlpha);
            mPaint.setTypeface(mFont);
            mPaint.setTextSize(fixFontSize(mSource, getTextSizeByDensity(), String.valueOf(mNumber), mPaint)); // getBitmap()
            mNumber = mSetter != null ? mSetter.getNumber() : mNumber;
//            logger.d("drawing text " + String.valueOf(mNumber) + "...");
            canvas.drawText(String.valueOf(mNumber), 0, 0, mPaint);
        } else {
            throw new RuntimeException("can't draw: source bitmap is incorrect");
        }
    }

//    @Override
//    public void setColorFilter(ColorFilter colorFilter) {
//        mPaint.setColorFilter(colorFilter);
//        invalidateSelf();
//    }

    public interface Defaults {
        Typeface DEFAULT_FONT = Typeface.create((String) null, Typeface.BOLD);
        int DEFAULT_TEXT_COLOR = Color.BLACK;
        int DEFAULT_TEXT_ALPHA = 0xBB;
        int DEFAULT_TEXT_SIZE_DP = 18;
    }

    public interface NumberSetter {
        int getNumber();
    }


}
