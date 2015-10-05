package com.arcadia.wearapp;

import android.content.Context;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WearableListItemLayout extends LinearLayout
        implements WearableListView.OnCenterProximityListener {

    private final float mFadedTextAlpha;
    private final int mFadedCircleColor;
    private final int mChosenCircleColor;
    private final float mNonFadedTextAlpha;
    private CircledImageView mCircle;
    private TextView mName;
    private TextView mSecond;

    public WearableListItemLayout(Context context) {
        this(context, null);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);

        mFadedTextAlpha = getResources()
                .getInteger(R.integer.action_text_faded_alpha) / 100f;
        mNonFadedTextAlpha = 1f;
        mFadedCircleColor = getResources().getColor(R.color.gray_material);
        mChosenCircleColor = getResources().getColor(R.color.blue_material);
    }

    // Get references to the icon and text in the item layout definition
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // These are defined in the layout file for list items
        // (see next section)
        mCircle = (CircledImageView) findViewById(R.id.circle);
        mName = (TextView) findViewById(R.id.title_text);
        mSecond = (TextView) findViewById(R.id.secondary_text);
    }

    @Override
    public void onCenterPosition(boolean animate) {
        mName.setAlpha(mNonFadedTextAlpha);
        mSecond.setAlpha(mNonFadedTextAlpha);

        mCircle.setCircleColor(mChosenCircleColor);
        mCircle.setCircleRadius(getResources().getDimension(R.dimen.radius_center_value));
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        mCircle.setCircleColor(mFadedCircleColor);
        mCircle.setCircleRadius(getResources().getDimension(R.dimen.radius_noncenter_value));
        mName.setAlpha(mFadedTextAlpha);
        mSecond.setAlpha(mFadedTextAlpha);
    }
}