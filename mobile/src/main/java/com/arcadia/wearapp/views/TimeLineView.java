package com.arcadia.wearapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.arcadia.wearapp.R;

import java.util.HashMap;
import java.util.Map;

public class TimeLineView extends View {

    int height;
    int width;
    private Map<Rect, Integer> rectangles = new HashMap<>();
    private Paint paint;
    private Paint textPaint;

    public TimeLineView(Context context) {
        super(context);
        initPaints();
    }

    public TimeLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.height = h;
        this.width = w;
    }

    private void initPaints() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(0xFF);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(4);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStrokeWidth(2);
        textPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size_secondary_material));
    }

    public void setRectangles(Map<Rect, Integer> rectangles) {
        this.rectangles = rectangles;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Map.Entry<Rect, Integer> rectangle : rectangles.entrySet()) {
            paint.setColor(rectangle.getValue());
            canvas.drawRect(rectangle.getKey(), paint);
        }

        paint.setColor(Color.BLACK);
        int lineHorizontalSize = width / 24;
        int lineVerticalSize = height * 2 / 3;

        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawLine(0, height, 0, lineVerticalSize, paint);
        canvas.drawText("0", 0, lineVerticalSize - 4, textPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 1; i < 25; i++) {
            int x = i * lineHorizontalSize - 2;
            canvas.drawLine(x, height, x, lineVerticalSize, paint);
            if (i % 3 == 0 && i != 24) {
                canvas.drawText(String.valueOf(i), x, lineVerticalSize - 4, textPaint);
            }
        }

        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("24", width, lineVerticalSize - 4, textPaint);
    }
}