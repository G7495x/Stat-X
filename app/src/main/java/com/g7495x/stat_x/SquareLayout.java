package com.g7495x.stat_x;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLayout extends LinearLayout{
public SquareLayout(Context context){
	super(context);
}

public SquareLayout(Context context,AttributeSet attrs){
	super(context,attrs);
}

public SquareLayout(Context context,AttributeSet attrs,int defStyleAttr){
	super(context,attrs,defStyleAttr);
}

public SquareLayout(Context context,AttributeSet attrs,int defStyleAttr,int defStyleRes){
	super(context,attrs,defStyleAttr,defStyleRes);
}

@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	super.onMeasure(widthMeasureSpec, widthMeasureSpec);
}
}