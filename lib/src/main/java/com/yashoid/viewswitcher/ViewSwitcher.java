package com.yashoid.viewswitcher;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.WeakHashMap;

/**
 * Please do not remove the following lines!
 *
 * Created by Yashar on 3/26/2018.
 *
 * https://github.com/yasharpm
 */

public class ViewSwitcher extends ViewGroup {

    private static final long SWITCH_DURATION = 500;
    private static final float SWITCH_SLIDE_TIME_PORTION = 0.5f;
    private static final float SWITCH_SLIDE_AMOUNT_PORTION = 0.75f;

    private float mChildVerticalPositionRatio = 0.4f;
    private float mChildHorizontalPaddingRatio = 0.1f;
    private float mStackPlacementAreaPortion = 0.66f;
    private float mStackSmallestSizeRatio = 0.75f;
    private float mStackAlpha = 1f;

    private int mChildWidth;
    private int mStackPlacementOffset;

    private int mFrontChildIndex = 0;

    private float mProgressRatio = 0;

    private ValueAnimator mAnimator;

    private WeakHashMap<View, ChildDrawInfo> mDrawInfos = new WeakHashMap<>();

    public ViewSwitcher(@NonNull Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public ViewSwitcher(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public ViewSwitcher(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        setChildrenDrawingOrderEnabled(true);
    }

    public void setChildVerticalPositionRatio(float ratio) {
        mChildVerticalPositionRatio = ratio;

        requestLayout();
    }

    public void setChildHorizontalPaddingRatio(float ratio) {
        mChildHorizontalPaddingRatio = ratio;

        requestLayout();
    }

    public void setStackPlacementAreaPortion(float portion) {
        mStackPlacementAreaPortion = portion;

        invalidate();
    }

    public void setStackSmallestSizeRatio(float ratio) {
        mStackSmallestSizeRatio = ratio;

        invalidate();
    }

    public void setStackAlpha(float ratio) {
        mStackAlpha = ratio;

        invalidate();
    }

    public int getFrontChildIndex() {
        return mFrontChildIndex;
    }

    public void setFrontChildIndex(int index) {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }

        mFrontChildIndex = index;
        mProgressRatio = 0;

        invalidate();
    }

    public boolean switchForward() {
        if (mAnimator != null) {
            return false;
        }

        if (mFrontChildIndex == getChildCount() - 1) {
            return false;
        }

        mAnimator = new ValueAnimator();
        mAnimator.setDuration(SWITCH_DURATION);
        mAnimator.setFloatValues(0, 1);
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        mAnimator.addListener(new AnimatorListener(true));
        mAnimator.start();

        return true;
    }

    public boolean switchBack() {
        if (mAnimator != null) {
            return false;
        }

        if (mFrontChildIndex == 0) {
            return false;
        }

        mFrontChildIndex--;
        mProgressRatio = 1;

        invalidate();

        mAnimator = new ValueAnimator();
        mAnimator.setDuration(SWITCH_DURATION);
        mAnimator.setFloatValues(1, 0);
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        mAnimator.addListener(new AnimatorListener(false));
        mAnimator.start();

        return true;
    }

    private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mProgressRatio = (float) animation.getAnimatedValue();

            invalidate();
        }

    };

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        mDrawInfos.put(child, new ChildDrawInfo(getChildCount() - 1, child));
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);

        mDrawInfos.remove(child);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        mChildWidth = (int) (width * (1 - 2 * mChildHorizontalPaddingRatio));

        final int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            child.measure(MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!changed) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        int childLeft = (width - mChildWidth) / 2;

        final int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            int childHeight = child.getMeasuredHeight();
            int childTop = (int) ((height - childHeight) * mChildVerticalPositionRatio);

            child.layout(childLeft, childTop, childLeft + mChildWidth, childTop + childHeight);
        }

        if (childCount > 1) {
            mStackPlacementOffset = (int) (childLeft * mStackPlacementAreaPortion / (childCount - 1));
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int order;

        if (mProgressRatio > 0.5f) {
            order = getOrder(childCount, i, mFrontChildIndex + 1);
        }
        else {
            order = getOrder(childCount, i);
        }

        return childCount - order - 1;
    }

    private int getOrder(int childCount, int i) {
        return getOrder(childCount, i, mFrontChildIndex);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        child.setAnimation(mDrawInfos.get(child));

        return super.drawChild(canvas, child, drawingTime);
    }

    private class ChildDrawInfo extends Animation {

        private int mIndex;
        private View mView;

        protected ChildDrawInfo(int index, View view) {
            mIndex = index;
            mView = view;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final int childCount = getChildCount();

            int order = getOrder(childCount, mIndex);

            if (mProgressRatio < SWITCH_SLIDE_TIME_PORTION) {
                t.setAlpha(mStackAlpha);

                t.getMatrix().postTranslate(order * mStackPlacementOffset, 0);

                if (childCount > 1) {
                    float ratio = (float) order / (childCount - 1);
                    float scale = getProgressiveValue(1, mStackSmallestSizeRatio, ratio);

                    t.getMatrix().postScale(scale, scale, mView.getWidth(), mView.getHeight() / 2);
                }
            }
            else if (order != 0) {
                float progress = (mProgressRatio - SWITCH_SLIDE_TIME_PORTION) / (1 - SWITCH_SLIDE_TIME_PORTION);

                if (order - 1 > 0) {
                    t.setAlpha(mStackAlpha);
                }
                else {
                    t.setAlpha(getProgressiveValue(mStackAlpha, 1, progress));
                }

                t.getMatrix().postTranslate((order - progress) * mStackPlacementOffset, 0);

                if (childCount > 1) {
                    float ratio = (order - progress) / (childCount - 1);
                    float scale = getProgressiveValue(1, mStackSmallestSizeRatio, ratio);

                    t.getMatrix().postScale(scale, scale, mView.getWidth(), mView.getHeight() / 2);
                }
            }

            if (order == 0) {
                if (mProgressRatio > 0) {
                    if (mProgressRatio <= SWITCH_SLIDE_TIME_PORTION) {
                        t.setAlpha(1);

                        float slideProgress = mProgressRatio / SWITCH_SLIDE_TIME_PORTION;

                        t.getMatrix().postTranslate(- slideProgress * mChildWidth * SWITCH_SLIDE_AMOUNT_PORTION, 0);
                    }
                    else {
                        float slideProgress = (mProgressRatio - SWITCH_SLIDE_TIME_PORTION) / (1 - SWITCH_SLIDE_TIME_PORTION);

                        t.setAlpha(getProgressiveValue(1, mStackAlpha, slideProgress));

                        t.getMatrix().postTranslate(getProgressiveValue(- mChildWidth * SWITCH_SLIDE_AMOUNT_PORTION, (childCount -1) * mStackPlacementOffset, slideProgress), 0);

                        float scale = getProgressiveValue(1, mStackSmallestSizeRatio, slideProgress);
                        t.getMatrix().postScale(scale, scale, mView.getWidth(), mView.getHeight() / 2);
                    }
                }
                else {
                    t.setAlpha(1);
                }
            }
        }
    }

    private class AnimatorListener implements Animator.AnimatorListener {

        private boolean mShouldAddToFrontChildIndex;

        protected AnimatorListener(boolean shouldAddToFrontChildIndex) {
            mShouldAddToFrontChildIndex = shouldAddToFrontChildIndex;
        }

        @Override
        public void onAnimationStart(Animator animation) { }

        @Override
        public void onAnimationEnd(Animator animation) {
            mAnimator = null;

            mProgressRatio = 0;

            if (mShouldAddToFrontChildIndex) {
                mFrontChildIndex++;
            }

            invalidate();
        }

        @Override
        public void onAnimationCancel(Animator animation) { }

        @Override
        public void onAnimationRepeat(Animator animation) { }

    }

    private static int getOrder(int childCount, int index, int frontChildIndex) {
        int order = index - frontChildIndex;

        if (order < 0) {
            order += childCount;
        }

        return order;
    }

    private static float getProgressiveValue(float from, float to, float progress) {
        return from * (1 - progress) + to * progress;
    }

}
