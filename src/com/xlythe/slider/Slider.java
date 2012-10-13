package com.xlythe.slider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

@SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class Slider extends LinearLayout implements OnClickListener, OnTouchListener, AnimationListener {
    private OnSlideListener slideListener;
    private OnSlideListener beforeSlideListener;
    private OnSlideListener afterSlideListener;
    private ImageButton slider;
    private LinearLayout body;
    private boolean sliderOpen;
    private int distance;
    private int offset;
    private int height;
    private int multiplier = 1;
    private int barHeight = 62;
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
        }
        if(android.os.Build.VERSION.SDK_INT < 8) {
            slider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        else {
            slider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        slider.setOnTouchListener(this);
        body = new LinearLayout(context);
        if(android.os.Build.VERSION.SDK_INT < 16) {
            body.setBackgroundDrawable(getBackground());
        }
        else {
            body.setBackground(getBackground());
        }
        if(android.os.Build.VERSION.SDK_INT < 8) {
            body.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        }
        else {
            body.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
        body.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        setBackgroundResource(android.R.color.transparent);
        
        addView(slider);
        addView(body);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout(){
            	if(android.os.Build.VERSION.SDK_INT < 16) {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                else {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                height = getHeight();
                if(android.os.Build.VERSION.SDK_INT < 8) {
                    body.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, getHeight()-barHeight));
                }
                else {
                    body.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getHeight()-barHeight));
                }
                minimizeSlider();
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            offset = (int) event.getY();
            if(slideListener != null && isSliderOpen()) slideListener.onSlide(Direction.DOWN);
            else if(slideListener != null && !isSliderOpen()) slideListener.onSlide(Direction.UP);
            break;
        case MotionEvent.ACTION_UP:
            if(sliderOpen) {
                if(distance*multiplier < (height-barHeight)/6) {
                    animateSliderOpen();
                }
                else{
                    animateSliderClosed();
                }
            }
            else{
                if(distance*multiplier < 5*(height-barHeight)/6) {
                    animateSliderOpen();
                }
                else{
                    animateSliderClosed();
                }
            }
            break;
        case MotionEvent.ACTION_MOVE:
            distance += event.getY()-offset;
            if(distance*multiplier < 0) distance = 0;
            if(distance*multiplier > height - barHeight) distance = (height - barHeight)*multiplier;
            translate();
            break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if(sliderOpen) {
            minimizeSlider();
        } else {
            maximizeSlider();
        }
    }

    private void translate() {
        if(android.os.Build.VERSION.SDK_INT < 11) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(getWidth(), getHeight());
            params.topMargin = distance;
            setLayoutParams(params);
        } else{
            setTranslationY(distance);
        }
    }

    private void minimizeSlider() {
        distance = (height - barHeight)*multiplier;
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
            public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(afterSlideListener != null) afterSlideListener.onSlide(Direction.UP);
            }
        });
        animationSet.setFillAfter(false);
        animationSet.setFillEnabled(true);

        TranslateAnimation r = new TranslateAnimation(0, 0, distance, 0); 
        r.setDuration(500);
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

            TranslateAnimation r = new TranslateAnimation(0, 0, distance, (height-barHeight)*multiplier); 
            r.setDuration(500);
            r.setFillAfter(false);
            animationSet.addAnimation(r);

            maximizeSlider();
            startAnimation(animationSet);
        } 
        else{
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setInterpolator(new DecelerateInterpolator());
            animationSet.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if(beforeSlideListener != null) beforeSlideListener.onSlide(Direction.DOWN);
                }

                @Override
                public void onAnimationRepeat(Animation animation) { }

                @Override
                public void onAnimationEnd(Animation animation) {
                    minimizeSlider();
                    if(afterSlideListener != null) afterSlideListener.onSlide(Direction.DOWN);
                }
            });
            animationSet.setFillAfter(true);
            animationSet.setFillEnabled(true);

            TranslateAnimation r = new TranslateAnimation(0, 0, distance-((height-barHeight)*multiplier), 0); 
            r.setDuration(500);
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
    public void onAnimationRepeat(Animation animation) {
    }

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

    public enum Direction{
        UP, DOWN
    }

    public static interface OnSlideListener{
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
        } else{
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
}
