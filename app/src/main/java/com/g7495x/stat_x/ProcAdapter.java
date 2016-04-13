package com.g7495x.stat_x;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ProcAdapter extends ArrayAdapter{
Context context;
int layoutResourceId;
Proc data[]=null;
View row;
ProcHolder holder;
LayoutInflater inflater;
Proc proc;

public ProcAdapter(Context context,int layoutResourceId,Proc[] data){
	super(context,layoutResourceId,data);

	this.layoutResourceId=layoutResourceId;
	this.context=context;
	this.data=data;
	holder=new ProcHolder();
}

@Override
public View getView(int position, View convertView, ViewGroup parent) {
	row=convertView;

	if(row==null){
		inflater=((Activity)context).getLayoutInflater();
		row=inflater.inflate(layoutResourceId,parent,false);

		holder.get(row,context);

		row.setTag(holder);
	}
	else{
		holder=(ProcHolder)row.getTag();
	}

	holder.set(data[position]);

	return row;
}
}
