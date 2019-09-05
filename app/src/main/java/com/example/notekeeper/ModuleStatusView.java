package com.example.notekeeper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class ModuleStatusView extends View {
    public static final int INVALID_MODULE_INDEX = -1;
    public static final int DEFAULT_CIRCLE_VALUE = 0;
    public static final float DEFAULT_OUTLINE_WIDTH = 2f;
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private static  final int EDIT_MODE_MODULE_COUNT = 7;

    private TextPaint mTextPaint;
    private  float mRadius;
    private float mTextWidth;
    private float mTextHeight;
    private float mOutLineWidth;
    private float mShapeSize;
    private float mSpacing;
    private Rect[] mModuleRectangles;
    private int mOutlineColor;
    private Paint mPaintOutline;
    private int mFillColor;
    private Paint mPaintFill;
    private int mMaxHorizontalModule;
    private int mShape;
    private ModuleStatusAccessibilityHelper mAccessibilityHelper;

    public boolean[] getModuleStatus() {
        return mModuleStatus;
    }

    public void setModuleStatus(boolean[] mModuleStatus) {
        this.mModuleStatus = mModuleStatus;
    }

    private boolean[] mModuleStatus;

    public ModuleStatusView(Context context) {
        super(context);
        init(null, 0);
    }

    public ModuleStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ModuleStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        if(isInEditMode())
            setupEditModeValues();

        setFocusable(true);
        mAccessibilityHelper = new ModuleStatusAccessibilityHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mAccessibilityHelper);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float displayDensity = displayMetrics.density;
        float defaultOutlineWidthPixels = displayDensity * DEFAULT_OUTLINE_WIDTH;

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ModuleStatusView, defStyle, 0);
        mOutlineColor = a.getColor(R.styleable.ModuleStatusView_outlineColor, Color.BLACK);
        mShape = a.getInt(R.styleable.ModuleStatusView_shape, DEFAULT_CIRCLE_VALUE);
        mOutLineWidth = a.getDimension(R.styleable.ModuleStatusView_outlineWidth, defaultOutlineWidthPixels);
        a.recycle();

        mShapeSize = 144f;
        mSpacing = 20f;
        mRadius = (mShapeSize - mOutLineWidth)/2;
        //setupModuleRectangles();

        mPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintOutline.setStyle(Paint.Style.STROKE);
        mPaintOutline.setStrokeWidth(mOutLineWidth);
        mPaintOutline.setColor(mOutlineColor);

        mFillColor = getContext().getResources().getColor(R.color.pluralsight_orange);
        mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintFill.setStyle(Paint.Style.FILL);
        mPaintFill.setColor(mFillColor);

    }

    private void setupEditModeValues() {
        boolean[] exampleModuleValues = new boolean[EDIT_MODE_MODULE_COUNT];
        int middle = EDIT_MODE_MODULE_COUNT / 2;
        for(int i=0; i < middle; i++)
            exampleModuleValues[i] = true;

        setModuleStatus(exampleModuleValues);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 0;
        int desiredHeight = 0;

        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = specWidth - getPaddingRight() - getPaddingLeft();
        int horizontalModulesThatCanFit = (int) (availableWidth /(mShapeSize + mSpacing));
        mMaxHorizontalModule = Math.min(horizontalModulesThatCanFit, mModuleStatus.length);

        desiredWidth = (int)(mMaxHorizontalModule * (mShapeSize + mSpacing)- mSpacing);
        desiredWidth += getPaddingLeft() + getPaddingRight();

        int rows = ((mModuleStatus.length - 1)/mMaxHorizontalModule)+1;
        desiredHeight = (int) ((rows*(mShapeSize + mSpacing)) - mSpacing);
        desiredHeight += getPaddingTop() + getPaddingBottom();

        int width = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0);
        int Height = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0);

        setMeasuredDimension(width, Height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                int moduleIndex = findItemAtPoint(event.getX(), event.getY());
                onModuleSelected(moduleIndex);
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void onModuleSelected(int moduleIndex) {
        if(moduleIndex == INVALID_MODULE_INDEX){
            return;
        }

        mModuleStatus[moduleIndex] =! mModuleStatus[moduleIndex];
        invalidate();

        mAccessibilityHelper.invalidateVirtualView(moduleIndex);
        mAccessibilityHelper.sendEventForVirtualView(moduleIndex,
                AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    private int findItemAtPoint(float x, float y) {
        int moduleIndex = INVALID_MODULE_INDEX;

        for(int i =0 ; i< mModuleRectangles.length; i++){
            if(mModuleRectangles[i].contains((int)x, (int)y)){
                moduleIndex = i;
                break;
            }
        }

        return moduleIndex;
    }

    private void setupModuleRectangles(int width) {
        int availableWidth = width - getPaddingLeft()- getPaddingRight();
        int horizontalModulesThatCanFit= (int)(availableWidth /(mShapeSize + mSpacing));
        int maxHorizontalModules = Math.min(horizontalModulesThatCanFit, mModuleStatus.length);
        mModuleRectangles = new Rect[mModuleStatus.length];
        for(int moduleIndex = 0; moduleIndex< mModuleRectangles.length; moduleIndex++){
           int row = moduleIndex / maxHorizontalModules;
           int column = moduleIndex % maxHorizontalModules;

            int x = getPaddingLeft() + (int) (column *(mShapeSize + mSpacing));
            int y =  getPaddingTop() + (int)(row*(mSpacing + mShapeSize));
            mModuleRectangles[moduleIndex] = new Rect(x, y, x + (int)mShapeSize,y +  (int)mShapeSize);
        }
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setupModuleRectangles(w);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for(int moduleIndex=0; moduleIndex < mModuleRectangles.length; moduleIndex++) {
            if(mShape== DEFAULT_CIRCLE_VALUE) {
                float x = mModuleRectangles[moduleIndex].centerX();
                float y = mModuleRectangles[moduleIndex].centerY();

                if (mModuleStatus[moduleIndex])
                    canvas.drawCircle(x, y, mRadius, mPaintFill);
                canvas.drawCircle(x, y, mRadius, mPaintOutline);
            }else {
                drawSquare(canvas, moduleIndex);
            }
        }

    }

    private void drawSquare(Canvas canvas, int moduleIndex){
        Rect moduleRectangle = mModuleRectangles[moduleIndex];

        if(mModuleStatus[moduleIndex])
            canvas.drawRect(moduleRectangle, mPaintFill);

        canvas.drawRect(moduleRectangle.left+ (mOutLineWidth/2),
                moduleRectangle.top +(mOutLineWidth/2),
                moduleRectangle.right - (mOutLineWidth/2),
                moduleRectangle.bottom - (mOutLineWidth/2),
                mPaintOutline);
    }

    private class ModuleStatusAccessibilityHelper extends ExploreByTouchHelper{

        public ModuleStatusAccessibilityHelper(View host) {
            super(host);
        }

        @Override
        protected int getVirtualViewAt(float v, float v1) {
           int moduleIndex = findItemAtPoint(v, v1);
            return moduleIndex == INVALID_MODULE_INDEX ?
                    ExploreByTouchHelper.INVALID_ID : moduleIndex;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> list) {
            if(mModuleRectangles == null)
                return;
            for(int moduleIndex = 0 ; moduleIndex < mModuleRectangles.length; moduleIndex++){
                list.add(moduleIndex);
            }
        }

        @Override
        protected void onPopulateNodeForVirtualView(int i, AccessibilityNodeInfoCompat mCompat) {
            mCompat.setFocusable(true);
            mCompat.setBoundsInParent(mModuleRectangles[i]);
            mCompat.setContentDescription("Module number "+ i);

            mCompat.setCheckable(true);
            mCompat.setCheckable(mModuleStatus[i]);
            mCompat.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
        }

        @Override
        protected boolean onPerformActionForVirtualView(int i, int i1, Bundle bundle) {
            switch (i1){
                case AccessibilityNodeInfoCompat.ACTION_CLICK:
                    onModuleSelected(i);
                    return true;
            }

            return false;
        }
    }




}
