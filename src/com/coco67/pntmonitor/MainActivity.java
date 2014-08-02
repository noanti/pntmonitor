package com.coco67.pntmonitor;

import com.coco67.pntmonitor.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button bt = (Button)findViewById(R.id.button1);
		OnClickListener l = new OnClickListener() {		
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,TaskManagerActivity.class);
				startActivity(intent);
			}
		};
		bt.setOnClickListener(l);	
		l=new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(MainActivity.this,TrafficManagerActivity.class);
				startActivity(intent);
			}
		};
		bt = (Button)findViewById(R.id.button2);
		bt.setOnClickListener(l);
	}
}
