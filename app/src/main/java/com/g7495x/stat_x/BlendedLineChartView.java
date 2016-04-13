package com.g7495x.stat_x;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;

import com.db.chart.Tools;
import com.db.chart.view.LineChartView;

public class BlendedLineChartView extends LineChartView{
Context     currentContext;
Typeface    font;

float   width;
float   height;

Paint   overlayPaint;

Paint   t1Paint;
float   w1;
float   h1;
int     percent;

Paint   t2Paint;
float   w2;

public BlendedLineChartView(Context context){
	super(context);

	currentContext=context;
	init();
}

public BlendedLineChartView(Context context,AttributeSet attrs){
	super(context,attrs);

	currentContext=context;
	init();
}

private void init(){
	this.setLayerType(LAYER_TYPE_HARDWARE,null);
	font=Typeface.createFromAsset(currentContext.getAssets(),"font.ttf");

	overlayPaint=new Paint();
	overlayPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

	t1Paint=new Paint();
	t1Paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
	t1Paint.setColor(Color.parseColor("#E1F0FF"));
	t1Paint.setAntiAlias(true);
	t1Paint.setTextAlign(Paint.Align.RIGHT);
	t1Paint.setTypeface(font);

	t2Paint=new Paint();
	t2Paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
	t2Paint.setColor(Color.parseColor("#E1F0FF"));
	t2Paint.setAntiAlias(true);
	t2Paint.setTypeface(font);

	percent=0;
}

public void setOverlayResource(int id,int w,int h){
	width=w;
	height=h;

	Bitmap overlay=Bitmap.createScaledBitmap(BitmapFactory.decodeResource(currentContext.getResources(),id),(int)width,(int)height,false);
	overlayPaint.setShader(new BitmapShader(overlay,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP));
}

public void setTextSize(int sizeDP){
	t1Paint.setTextSize(Tools.fromDpToPx(sizeDP));
	w1=t1Paint.measureText("25");

	t2Paint.setTextSize(Tools.fromDpToPx(sizeDP>>1));
	w2=t2Paint.measureText("%");

	h1=(sizeDP+height)/2;
	w1=(width+w1-w2)/2;
}

public void setPercent(int percentage){
	percent=percentage;
}

@Override
protected void onDraw(Canvas canvas){
	super.onDraw(canvas);

	canvas.drawRect(0,0,width,height,overlayPaint);
	canvas.drawText(String.format("%02d",percent),w1,h1,t1Paint);
	canvas.drawText("%",w1,h1,t2Paint);
}
}
