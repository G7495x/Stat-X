package com.g7495x.stat_x;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

public class ProcHolder{
TextView    name;
TextView    pname;
TextView    cpu;
TextView    mem;

Typeface	font;

void set(Proc proc){
	name.setText(proc.name);
	pname.setText(proc.pname);
	cpu.setText(proc.cpu+"%");
	mem.setText(proc.mem+"M");
}

void get(View row,Context currentContext){
	font=Typeface.createFromAsset(currentContext.getAssets(),"font.ttf");

	name=(TextView)row.findViewById(R.id.name);
	pname=(TextView)row.findViewById(R.id.pName);
	cpu=(TextView)row.findViewById(R.id.proccpu);
	mem=(TextView)row.findViewById(R.id.procmem);

	name.setTypeface(font);
	pname.setTypeface(font);
	cpu.setTypeface(font);
	mem.setTypeface(font);
}
}
