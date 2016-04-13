package com.g7495x.stat_x;

public class Entity{
public boolean stagnent;
public float[] log,percent;
public float max;
public long val;
public int logLen;

int i;

Entity(){
	stagnent=true;
	max=0;
	val=0;
	logLen=33;
	log=new float[logLen];
	percent=new float[logLen];
	for(i=0;i<logLen;++i){
		log[i]=0;
		percent[i]=0;
	}
}

Entity(int size){
	stagnent=true;
	max=0;
	val=0;
	logLen=size;
	log=new float[size];
	percent=new float[size];
	for(i=0;i<size;++i){
		log[i]=0;
		percent[i]=0;
	}
}

public void push(float val){
	stagnent=true;
	max=0;
	for(i=0;i<logLen-1;++i){
		if(log[i]!=log[i+1]){
			stagnent=false;
			log[i]=log[i+1];
		}
		if(log[i]>max)
			max=log[i];
	}
	if(log[logLen-1]!=val)
		stagnent=false;
	log[logLen-1]=val;
}

public void push2(float val){
	stagnent=true;
	max=0;
	for(i=0;i<logLen-2;++i){
		if(log[i]!=log[i+2]){
			stagnent=false;
			log[i]=log[i+2];
		}
		if(log[i]>max)
			max=log[i];
	}
	if(log[logLen-2]!=val)
		stagnent=false;
	log[logLen-2]=val;
	if(log[logLen-1]!=val)
		stagnent=false;
	log[logLen-1]=val;
}

public float top(){
	return log[logLen-1];
}

public void calcPercent(float Max){
	for(i=0;i<logLen;++i)
		percent[i]=(int)(log[i]*100/Max);
}
}