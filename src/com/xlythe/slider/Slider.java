package com.xlythe.slider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

@SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class Slider extends LinearLayout implements OnClickListener, OnTouchListener, AnimationListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;
    private VelocityTracker mVelocityTracker;

    private float mPreviousEvent;

    private OnSlideListener slideListener;
    private OnSlideListener beforeSlideListener;
    private OnSlideListener afterSlideListener;
    private ImageButton slider;
    private LinearLayout body;
    private boolean sliderOpen = false;
    private int distance;
    private int offset;
    private int height = -1;
    private int multiplier = 1;
    private int barHeight = 62;
    private boolean vibrate = false;
    private int vibrationStrength = 10;

    public Slider(Context context) {
        super(context);
        setupView(context, null);
    }

    public Slider(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView(context, attrs);
    }

    public Slider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupView(context, attrs);
    }

    private void setupView(Context context, AttributeSet attrs) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);

        setOrientation(LinearLayout.VERTICAL);
        setScrollContainer(false);
        setHorizontalScrollBarEnabled(false);
        slider = new ImageButton(context);
        if(attrs != null) {
            int[] attrsArray = new int[] { android.R.attr.scrollbarThumbHorizontal };
            TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
            Drawable background = ta.getDrawable(0);
            if(android.os.Build.VERSION.SDK_INT < 16) {
                slider.setBackgroundDrawable(background);
            }
            else {
                slider.setBackground(background);
            }
            ta.recycle();
        }
        slider.setOnTouchListener(this);
        body = new LinearLayout(context);
        if(android.os.Build.VERSION.SDK_INT < 16) {
            body.setBackgroundDrawable(getBackground());
        }
        else {
            body.setBackground(getBackground());
        }
        body.setOnTouchListener(this);
        setBackgroundResource(android.R.color.transparent);

        addView(slider);
        addView(body);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean minimize = height != bottom;
        height = bottom;
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(right, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height - barHeight, MeasureSpec.EXACTLY);
        body.measure(widthMeasureSpec, heightMeasureSpec);
        if(minimize && sliderOpen) maximizeSlider();
        else if(minimize && !sliderOpen) minimizeSlider();
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if(vibrate) vibrate();

            mVelocityTracker = VelocityTracker.obtain();
            mVelocityTracker.addMovement(event);

            offset = (int) event.getY();

            if(slideListener != null && isSliderOpen()) slideListener.onSlide(Direction.DOWN);
            else if(slideListener != null && !isSliderOpen()) slideListener.onSlide(Direction.UP);
            break;
        case MotionEvent.ACTION_UP:
            if(mVelocityTracker == null) {
                break;
            }

            mVelocityTracker.addMovement(event);
            mVelocityTracker.computeCurrentVelocity(1000);
            float velocityY = Math.abs(mVelocityTracker.getYVelocity());
            boolean open = (isSliderOpen() && distance * multiplier < height / 8) || (!isSliderOpen() && distance * multiplier > height / 8);
            if(mMinFlingVelocity <= velocityY && velocityY <= mMaxFlingVelocity) {
                open = mPreviousEvent > event.getY();
            }
            if(open) {
                animateSliderOpen();
            }
            else {
                animateSliderClosed();
            }
            mVelocityTracker.recycle();
            mVelocityTracker = null;
            break;
        case MotionEvent.ACTION_MOVE:
            if(mVelocityTracker == null) {
                break;
            }

            mPreviousEvent = event.getY();

            mVelocityTracker.addMovement(event);
            distance += event.getY() - offset;
            if(distance * multiplier < 0) distance = 0;
            if(distance * multiplier > height - barHeight) distance = (height - barHeight) * multiplier;
            if(Math.abs(distance) > mSlop) {
                translate();
                return true;
            }
            break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if(vibrate) vibrate();
        if(sliderOpen) {
            animateSliderClosed();
        }
        else {
            animateSliderOpen();
        }
    }

    private void translate() {
        if(android.os.Build.VERSION.SDK_INT < 11) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
            params.topMargin = distance;
            setLayoutParams(params);
        }
        else {
            setTranslationY(distance);
        }
    }

    private void minimizeSlider() {
        distance = (height - barHeight) * multiplier;
        translate();
        if(slideListener != null) slideListener.onSlide(Direction.DOWN);
        sliderOpen = false;
    }

    private void maximizeSlider() {
        distance = 0;
        translate();
        bringToFront();
        if(slideListener != null) slideListener.onSlide(Direction.UP);
        sliderOpen = true;
    }

    public void animateSliderOpen() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setInterpolator(new DecelerateInterpolator());
        animationSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if(beforeSlideListener != null) beforeSlideListener.onSlide(Direction.UP);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if(afterSlideListener != null) afterSlideListener.onSlide(Direction.UP);
            }
        });
        animationSet.setFillAfter(false);
        animationSet.setFillEnabled(true);

        TranslateAnimation r = new TranslateAnimation(0, 0, distance, 0);
        r.setDuration(mAnimationTime);
        r.setFillAfter(false);
        animationSet.addAnimation(r);

        maximizeSlider();
        startAnimation(animationSet);
    }

    public void animateSliderClosed() {
        if(android.os.Build.VERSION.SDK_INT < 11) {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setAnimationListener(this);
            animationSet.setInterpolator(new DecelerateInterpolator());
            animationSet.setFillAfter(false);
            animationSet.setFillEnabled(true);

            TranslateAnimation r = new TranslateAnimation(0, 0, distance, (height - barHeight) * multiplier);
            r.setDuration(mAnimationTime);
            r.setFillAfter(false);
            animationSet.addAnimation(r);

            maximizeSlider();
            startAnimation(animationSet);
        }
        else {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setInterpolator(new DecelerateInterpolator());
            animationSet.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if(beforeSlideListener != null) beforeSlideListener.onSlide(Direction.DOWN);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    minimizeSlider();
                    if(afterSlideListener != null) afterSlideListener.onSlide(Direction.DOWN);
                }
            });
            animationSet.setFillAfter(true);
            animationSet.setFillEnabled(true);

            TranslateAnimation r = new TranslateAnimation(0, 0, distance - ((height - barHeight) * multiplier), 0);
            r.setDuration(mAnimationTime);
            r.setFillAfter(true);
            animationSet.addAnimation(r);

            minimizeSlider();
            startAnimation(animationSet);
        }
    }

    public void addViewToBody(View v) {
        body.addView(v);
    }

    @Override
    public void onAnimationEnd(Animation a) {
        minimizeSlider();
        if(afterSlideListener != null) afterSlideListener.onSlide(Direction.DOWN);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {}

    @Override
    public void onAnimationStart(Animation animation) {
        if(beforeSlideListener != null) beforeSlideListener.onSlide(Direction.DOWN);
    }

    public boolean isSliderOpen() {
        return sliderOpen;
    }

    public void setOnSlideListener(OnSlideListener slideListener) {
        this.slideListener = slideListener;
    }

    public enum Direction {
        UP,
        DOWN
    }

    public static interface OnSlideListener {
        public void onSlide(Direction d);
    }

    public void setBeforeSlideListener(OnSlideListener beforeSlideListener) {
        this.beforeSlideListener = beforeSlideListener;
    }

    public void setAfterSlideListener(OnSlideListener afterSlideListener) {
        this.afterSlideListener = afterSlideListener;
    }

    @Override
    public void addView(View v) {
        if(v == slider || v == body) {
            super.addView(v);
        }
        else {
            body.addView(v);
        }
    }

    public void setSlideDirection(Direction d) {
        switch(d) {
        case UP:
            removeAllViews();
            addView(slider);
            addView(body);
            multiplier = 1;
            break;
        case DOWN:
            removeAllViews();
            addView(body);
            addView(slider);
            multiplier = -1;
            break;
        }
    }

    public void setClickToOpen(boolean clickToOpen) {
        if(clickToOpen) slider.setOnClickListener(this);
        else slider.setOnClickListener(null);
    }

    public void setBarHeight(int height) {
        barHeight = height;
        slider.getLayoutParams().height = barHeight;
    }

    public void setBarBackground(Drawable background) {
        if(android.os.Build.VERSION.SDK_INT < 16) {
            slider.setBackgroundDrawable(background);
        }
        else {
            slider.setBackground(background);
        }
    }

    public void setBarBackgroundResource(int background) {
        slider.setBackgroundResource(background);
    }

    public void setBodyBackground(Drawable background) {
        if(android.os.Build.VERSION.SDK_INT < 16) {
            body.setBackgroundDrawable(background);
        }
        else {
            body.setBackground(background);
        }
    }

    public void setBodyBackgroundResource(int background) {
        body.setBackgroundResource(background);
    }

    public View getBar() {
        return slider;
    }

    public LinearLayout getBody() {
        return body;
    }

    private void vibrate() {
        Vibrator vi = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if(!vi.hasVibrator()) return;
        vi.vibrate(vibrationStrength);
    }

    public void enableClick(boolean enabled) {
        OnClickListener listener = (enabled) ? this : null;
        slider.setOnClickListener(listener);
    }

    public void enableTouch(boolean enabled) {
        OnTouchListener listener = (enabled) ? this : null;
        slider.setOnTouchListener(listener);
    }

    public void enableVibration(boolean enabled, int strength) {
        vibrate = enabled;
        vibrationStrength = strength;
    }
}
