package com.g7495x.stat_x;
//LogLength=33

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.view.Display;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.QuadEase;
import com.db.chart.view.animation.easing.SineEase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity{
String	    op;
String      op2;

String[]	timeLabels;
String[]    percentLabels;

int         old;
int         now;
int         interval;
int         intervalMilli;          //delay(in milli sec.)
long        count;
long        temp;

boolean     isActive;

//CPU Block
int         n;                      //no. of CPUs
Entity[]    cpu;

String[]    load;
String[]	toks;

long[][]    totalTime;              //current total uptime
long[][]    busyTime;               //current busy uptime
float[]     cpu_usage;              //current cpu usage of each core

long[]	    timestampTotalTime;     //total uptime since timestamp(per core)
long[]	    timestampBusyTime;      //busy uptime since timestamp(per core)
float[]     cpu_avg;                //avg cpu usage of each core since timestamp
String	    cpuTimestamp;

int         cpusBusy;

//Mem Block
ActivityManager.MemoryInfo mi;
Entity      mem;

int         totalMem;		        //total memory(in MBytes)
int         usedMem;		        //used memory(in MBytes)
int         availableMem;	        //available memory(in MBytes)
int         memPercent;		        //used memory percent

//Net Block
Entity      netD;
Entity      netU;

Entity      mobD;
Entity      mobU;

Entity      wifiD;
Entity      wifiU;

long[]	    netDByte=new long[2];   //bytes downloaded
long[]	    netUByte=new long[2];   //bytes uploaded

long[]	    mobDByte=new long[2];   //mobile data bytes downloaded
long[]	    mobUByte=new long[2];   //mobile data bytes uploaded

long[]      wifiDByte=new long[2];  //wifi bytes downloaded
long[]      wifiUByte=new long[2];  //wifi bytes uploaded

float       netMax;

long        timestampNetDByte;
long        timestampNetUByte;

long        timestampMobDByte;
long        timestampMobUByte;

String              netTimestamp;
SpannableString     netPeakText;
AbsoluteSizeSpan    smallSize;

//Process Block
PackageManager  packageMan;
boolean         procMemMode;
String          topCommand;
Process         p;
BufferedReader  buffReader;
String          line;
String[]        attrs;
int             noOfProcs;
Debug.MemoryInfo[] pidPss;
ProcAdapter     topAppsList;
Proc[]          topApps;
ListView        procs;
int[]           pids;
boolean         busy;
AsyncTaskRunner updateProcInfo;

//unknown block
ActivityManager activityManager;

Handler handler;

Typeface font;
//TextView    t1;
TextView    tstamp1;
TextView    tstamp2;
//TextView    t2;

BlendedLineChartView    cpugraph;
TextView                cores;
BlendedLineChartView    memgraph;
TextView                memview;
LineChartView           netgraph;
TextView                peak;
TextView                dRate;
TextView                uRate;

LineSet     dataset;
Animation   anim;
Paint       gridPaint;

Context     currentContext;
Display     display;
Point       size;

@Override
protected void onCreate(Bundle savedInstanceState){
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	currentContext=this;
	//FontsOverride.overrideFont(this,"DEFAULT","font.ttf");
	//FontsOverride.overrideFont(this,"SANS_SERIF","font.ttf");

	backend();
	frontend();

	cpuTimestamp(tstamp1);
	netTimestamp(tstamp2);

	handler=new Handler();
	//t2.start();
	refreshStats.run();
}

@Override
protected void onResume(){
	super.onResume();
	isActive=true;
}

@Override
protected void onPause(){
	super.onPause();
	isActive=false;
}

void backend(){
	int i;

	old=0;
	now=0;
	interval=1;
	intervalMilli=interval*1000;
	count=0;
	isActive=true;

	activityManager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);

	//CPU Block
	n=getNumOfCpus();

	cpu=new Entity[n+1];
	for(i=0;i<=n;++i)
		cpu[i]=new Entity(4);

	load=new String[n+1];

	totalTime=	new long[2][n+1];
	busyTime=	new long[2][n+1];

	cpu_usage=	new float[n+1];
	cpu_avg=	new float[n+1];

	timestampTotalTime=new long[n+1];
	timestampBusyTime=new long[n+1];

	//Mem Block
	mem=new Entity(4);

	mi=new ActivityManager.MemoryInfo();
	activityManager.getMemoryInfo(mi);
	totalMem=(int)(mi.totalMem/1000000);

	//Net Block
	netMax=1;

	netD=   new Entity();
	netU=   new Entity();

	mobD=   new Entity();
	mobU=   new Entity();

	wifiD=  new Entity();
	wifiU=  new Entity();

	//Process Block
	packageMan=this.getPackageManager();
	procMemMode=false;
	topCommand="top -n 1 -d "+(interval);
	noOfProcs=4;
	topApps=new Proc[noOfProcs];
	for(i=0;i<noOfProcs;++i)
		topApps[i]=new Proc();
	busy=false;
}

void frontend(){
	int i,len;

	font=Typeface.createFromAsset(currentContext.getAssets(),"font.ttf");
	smallSize=new AbsoluteSizeSpan(8,true);

	display=getWindowManager().getDefaultDisplay();
	size=new Point();
	display.getSize(size);

	//CPU Block
	cores=(TextView)findViewById(R.id.cores);
	cores.setText("CPU\n"+n+" Cores");

	cpugraph=(BlendedLineChartView)findViewById(R.id.cpugraph);
	cpugraph.setOverlayResource(R.drawable.diamondblue,size.x*16/27,size.x*16/27);  //Dimensions of the diamond(px)
	cpugraph.setTextSize(48);

	len=cpu[0].logLen;
	percentLabels=new String[len];
	for(i=0;i<len;++i)
		percentLabels[i]="";

	dataset=new LineSet(percentLabels,cpu[0].log);
	dataset .setFill(Color.parseColor("#000000"))
			.setThickness(1)
			.setSmooth(true);
	cpugraph.addData(dataset);
	dataset=null;

	cpugraph.setBorderSpacing(Tools.fromDpToPx((float)0.5))
			.setLabelsColor(Color.parseColor("#7f7f7f"))
			.setXAxis(false)
			.setYAxis(false)
			.setXLabels(AxisController.LabelPosition.NONE)
			.setYLabels(AxisController.LabelPosition.NONE)
			.setAxisBorderValues(0,100,25);

	anim=null;
	anim=new Animation(intervalMilli);
	anim.setEasing(new SineEase())
			.setAlpha(1);
	cpugraph.show(anim);

	//Mem Block
	memview=(TextView)findViewById(R.id.mem);

	memgraph=(BlendedLineChartView)findViewById(R.id.memgraph);
	memgraph.setOverlayResource(R.drawable.diamondgreen,size.x*8/27,size.x*8/27);   //Dimensions of the diamond(px)
	memgraph.setTextSize(24);

	dataset=new LineSet(percentLabels,mem.percent);
	dataset.setFill(Color.parseColor("#000000"))
			.setThickness(1)
			.setSmooth(true);
	memgraph.addData(dataset);
	dataset=null;

	memgraph.setBorderSpacing(Tools.fromDpToPx((float)0.5))
			.setLabelsColor(Color.parseColor("#7f7f7f"))
			.setXAxis(false)
			.setYAxis(false)
			.setXLabels(AxisController.LabelPosition.NONE)
			.setYLabels(AxisController.LabelPosition.NONE)
			.setAxisBorderValues(0,100,25);

	anim=null;
	anim=new Animation(intervalMilli);
	anim.setEasing(new SineEase())
			.setAlpha(1);
	memgraph.show(anim);

	//Net Block
	peak=(TextView)findViewById(R.id.peak);
	dRate=(TextView)findViewById(R.id.dRate);
	uRate=(TextView)findViewById(R.id.uRate);

	gridPaint=new Paint();
	gridPaint.setColor(Color.parseColor("#7f7f7f7f"));
	gridPaint.setStyle(Paint.Style.STROKE);
	gridPaint.setAntiAlias(true);
	gridPaint.setStrokeWidth(Tools.fromDpToPx((float)0.25));

	netgraph=(LineChartView)findViewById(R.id.netgraph);

	len=netD.logLen;
	timeLabels=new String[len];
	for(i=0;i<len;++i)
		timeLabels[i]="";

	int color[][]=new int[2][2];
	float pos[]=new float[2];

	pos[0]=0;
	pos[1]=1;

	color[0][0]=Color.parseColor("#2f3f3f3f");
	color[0][1]=Color.parseColor("#1f3f3f3f");

	dataset=new LineSet(timeLabels,netU.percent);
	dataset.setColor(Color.parseColor("#3f3f3f"))
			.setGradientFill(color[0],pos)
			.setThickness(Tools.fromDpToPx(1))
			.setSmooth(true);
	netgraph.addData(dataset);
	dataset=null;

	color[1][0]=Color.parseColor("#2f0096fa");
	color[1][1]=Color.parseColor("#170096fa");

	dataset=new LineSet(timeLabels,netD.percent);
	dataset.setColor(Color.parseColor("#0096fa"))
			.setGradientFill(color[1],pos)
			.setThickness(Tools.fromDpToPx(1))
			.setSmooth(true);
	netgraph.addData(dataset);
	dataset=null;

	netgraph.setBorderSpacing(Tools.fromDpToPx((float)0.5))
			.setLabelsColor(Color.parseColor("#7f7f7f"))
			.setGrid(ChartView.GridType.HORIZONTAL,2,1,gridPaint)
			.setXAxis(true)
			.setYAxis(false)
			.setAxisColor(Color.parseColor("#E1F0FF"))
			.setAxisThickness(Tools.fromDpToPx((float)0.5))
			.setXLabels(AxisController.LabelPosition.NONE)
			.setYLabels(AxisController.LabelPosition.NONE)
			.setAxisBorderValues(0,120,60);

	anim=null;
	anim=new Animation(500);
	anim.setEasing(new QuadEase())
			.setAlpha(1);
	netgraph.show(anim);

	//Process Block
	topAppsList=new ProcAdapter(this,R.layout.proclistitem,topApps);
	procs=(ListView)findViewById(R.id.procs);
	procs.setAdapter(topAppsList);

	cores.setTypeface(font);
	memview.setTypeface(font);
	peak.setTypeface(font);
	dRate.setTypeface(font);
	uRate.setTypeface(font);
}

public int getNumOfCpus(){
	class CPUFilter implements FileFilter{
		@Override
		public boolean accept(File pathname){
			//Check if filename is "cpu0","cpu1",...
			return Pattern.matches("cpu[0-9]+",pathname.getName());
		}
	}

	try{
		//Get directory containing CPU info
		File dir=new File("/sys/devices/system/cpu/");
		File[] files=dir.listFiles(new CPUFilter());
		// Return the number of cores
		return files.length;
	}catch(Exception e){
		return 1;
	}
}

public void getCpuStats(){
	try{
		int i;
		RandomAccessFile reader=new RandomAccessFile("/proc/stat","r");
		reader.readLine();

		for(i=1;i<=n;++i){
			load[i]=reader.readLine();
		}

		reader.close();
	}catch(Exception e){}
}

public void update(){
	int i,j;

	getCpuStats();
	op="CPU\n";

	cpu_usage[0]=0;
	cpu_avg[0]=0;
	cpusBusy=0;
	for(i=1;i<=n;++i){
		toks=load[i].split(" +");

		totalTime[now][i]=0;
		for(j=1;j<toks.length;++j)
			totalTime[now][i]+=Long.parseLong(toks[j]);
		busyTime[now][i]=totalTime[now][i]-Long.parseLong(toks[4])-Long.parseLong(toks[5]);

		temp=totalTime[now][i]-totalTime[old][i];
		if(temp!=0)
			cpu_usage[i]=((busyTime[now][i]-busyTime[old][i])*100)/temp;
		else
			cpu_usage[i]=0;
		if(cpu_usage[i]>100)
			cpu_usage[i]=100;

		temp=totalTime[now][i]-timestampTotalTime[i];
		if(temp!=0)
			cpu_avg[i]=((busyTime[now][i]-timestampBusyTime[i])*(float)100)/temp;
		else
			cpu_avg[i]=0;
		if(cpu_avg[i]>100)
			cpu_avg[i]=100;

		//log cpu usage
		cpu[i].push(cpu_usage[i]);

		cpu_usage[0]+=cpu_usage[i];
		cpu_avg[0]+=cpu_avg[i];

		if(cpu_usage[i]!=0)
			++cpusBusy;
	}
	cpu_usage[0]/=n;
	cpu_usage[0]=(int)cpu_usage[0];
	cpu_avg[0]/=n;
	cpu[0].push(cpu_usage[0]);

	//memory usage
	activityManager.getMemoryInfo(mi);
	availableMem=(int)(mi.availMem/1000000);
	usedMem=totalMem-availableMem;
	memPercent=availableMem*100/totalMem;

	//log memory usage
	mem.push(usedMem);
	mem.calcPercent(totalMem);

	//network stats
	netDByte[now]=TrafficStats.getTotalRxBytes();
	netUByte[now]=TrafficStats.getTotalTxBytes();
	mobDByte[now]=TrafficStats.getMobileRxBytes();
	mobUByte[now]=TrafficStats.getMobileTxBytes();
	wifiDByte[now]=netDByte[now]-mobDByte[now];
	wifiUByte[now]=netUByte[now]-mobUByte[now];

	netD.push2((netDByte[now]-netDByte[old])/(float)intervalMilli);
	netU.push2((netUByte[now]-netUByte[old])/(float)intervalMilli);
	mobD.push2((mobDByte[now]-mobDByte[old])/(float)intervalMilli);
	mobU.push2((mobUByte[now]-mobUByte[old])/(float)intervalMilli);
	wifiD.push2((wifiDByte[now]-wifiDByte[old])/(float)intervalMilli);
	wifiU.push2((wifiUByte[now]-wifiUByte[old])/(float)intervalMilli);

	//For net graph
	netMax=netD.max;
	if(netU.max>netMax)
		netMax=netU.max;
	if(netMax<=0)
		netMax=1;
	netD.calcPercent(netMax);
	netU.calcPercent(netMax);

	/*
	op+="\nDownload rate:"+String.format("%.2f",netD.top())+"kB/s\n";
	op+="Upload rate:"+String.format("%.2f",netU.top())+"kB/s\n";

	op+="Total data downloaded on wifi:"+String.format("%.2f",(wifiDByte[now])/1000000.0)+"MB\n";
	op+="Total data uploaded on wifi:"+String.format("%.2f",(wifiUByte[now])/1000000.0)+"MB\n";
	op+="Total data downloaded on mobile data:"+String.format("%.2f",mobDByte[now]/1000000.0)+"MB\n";
	op+="Total data uploaded on mobile data:"+String.format("%.2f",mobUByte[now]/1000000.0)+"MB\n";
	*/

	old=now;
	now=++now%2;
	++count;
}

Runnable refreshStats=new Runnable(){
	@Override
	public void run(){
		update();

		if(isActive){
			if(!busy){
				updateProcInfo=new AsyncTaskRunner();
				updateProcInfo.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}

			if(!cpu[0].stagnent){
				cpugraph.updateValues(0,cpu[0].log);
				try{
					cpugraph.notifyDataUpdate();
				}catch(Exception e){}
				cpugraph.setPercent((int)cpu[0].top());
			}

			if((!mem.stagnent)&&(count>1)){
				memgraph.updateValues(0,mem.percent);
				try{
					memgraph.notifyDataUpdate();
				}catch(Exception e){}
				memgraph.setPercent((int)mem.percent[mem.logLen-1]);
				memview.setText("Mem\n"+availableMem+"M free");
			}

			if(!netU.stagnent)
				netgraph.updateValues(0,netU.percent);

			if(!netD.stagnent)
				netgraph.updateValues(1,netD.percent);

			if((!netU.stagnent)||(!netD.stagnent)){
				try{
					netgraph.notifyDataUpdate();
				}catch(Exception e){}
				netPeakText=new SpannableString("Peak "+format(netMax));
				netPeakText.setSpan(smallSize,0,4,0);
				peak.setText(netPeakText);
				dRate.setText(format(netD.top()));
				uRate.setText(format(netU.top()));
			}
		}

		handler.postDelayed(refreshStats,intervalMilli);
	}
};

public void cpuTimestamp(View view){
	int i,j;

	getCpuStats();

	//for all cores(with avg core)
	for(i=1;i<=n;++i){
		//Get CPU Times
		toks=load[i].split(" +");

		//this block gets the cpu times from load[],(Total & Busy times for all cores)
		timestampTotalTime[i]=0;
		for(j=1;j<toks.length;++j)
			timestampTotalTime[i]+=Long.parseLong(toks[j]);
		timestampBusyTime[i]=timestampTotalTime[i]-Long.parseLong(toks[4])-Long.parseLong(toks[5]);
	}
	cpuTimestamp=null;
	cpuTimestamp=new SimpleDateFormat("h:mm:ss a").format(Calendar.getInstance().getTime());
	//tstamp1.setText("Avg. CPU usage from "+cpuTimestamp+" is "+String.format("%.2f",cpu_avg[0])+"%");
}

public void netTimestamp(View view){
	timestampNetDByte=TrafficStats.getTotalRxBytes();
	timestampNetUByte=TrafficStats.getTotalTxBytes();
	timestampMobDByte=TrafficStats.getMobileRxBytes();
	timestampMobUByte=TrafficStats.getMobileTxBytes();

	netTimestamp=null;
	netTimestamp=new SimpleDateFormat("h:mm:ss a").format(Calendar.getInstance().getTime());

	//tstamp2.setText("0B downloaded,0B uploaded since "+netTimestamp);
}

public String format(float f){
	int i;
	String s;
	s="";
	for(i=0;f>=1000;++i)
		f/=1000;
	if(f>100)
		s+=(int)f;
	else if(f>10)
		s+=String.format("%.1f",f);
	else
		s+=String.format("%.2f",f);
	switch(i){
		case 0: s+="kB/s";
			break;
		case 1: s+="MB/s";
			break;
		case 2: s+="GB/s";
			break;
	}
	return s;
}

ProcHolder holder;
View row;
public class AsyncTaskRunner extends AsyncTask<Void,Void,Void>{
	int i;
	@Override
	protected Void doInBackground(Void... params){

		pids=new int[noOfProcs];
		if(!busy){
			try{
				busy=true;

				p=Runtime.getRuntime().exec(topCommand);
				buffReader=new BufferedReader(new InputStreamReader(p.getInputStream()));

				buffReader.readLine();
				buffReader.readLine();
				buffReader.readLine();
				buffReader.readLine();
				buffReader.readLine();
				buffReader.readLine();
				buffReader.readLine();

				line=buffReader.readLine().trim();
				for(i=0;i<noOfProcs;line=buffReader.readLine().trim()){
					attrs=line.split(" +");
					try{
						topApps[i].pname=attrs[9];
						topApps[i].name=(String)packageMan.getApplicationLabel(packageMan.getApplicationInfo(topApps[i].pname,PackageManager.GET_META_DATA));
						pids[i]=topApps[i].pid=Integer.parseInt(attrs[0]);
						topApps[i].cpu=Integer.parseInt(attrs[2].substring(0,attrs[2].length()-1))*cpusBusy/n;
						++i;
					}catch(Exception e){}
				}

				pidPss=activityManager.getProcessMemoryInfo(pids);
				for(i=0;i<noOfProcs;++i){
					topApps[i].mem=pidPss[i].getTotalPss()/1000;
					op2+=topApps[i].name+"\t"+topApps[i].cpu+"\t"+topApps[i].mem+"M\n";
				}
				busy=false;
			}catch(Exception e){
				busy=false;
			}
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result){
		if(!busy){
			try{
				procs.setAdapter(procs.getAdapter());
				topAppsList.notifyDataSetChanged();
			}catch(Exception e){}
		}
	}
}
}
