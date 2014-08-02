package com.coco67.pntmonitor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.coco67.pntmonitor.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class TaskManagerActivity extends Activity {
	protected static final int LOAD_TASK_FINISHED = 60;
	protected static final String TAG = "TaskManagerActivity";
	int memory;
	TextView tv_task_manager_avail_memory;
	ActivityManager am;
	ProgressDialog pd;
	PackageManager pm;
	ListView lv_task_manager;
	List<TaskInfo> taskinfos;
	TaskManagerAdapter adapter;
	Map<String, Boolean> map;
	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case LOAD_TASK_FINISHED:
				pd.dismiss();
				setTitle();
				if (adapter == null) {
					adapter = new TaskManagerAdapter(TaskManagerActivity.this,
							taskinfos);
					lv_task_manager.setAdapter(adapter);
				} else {

					map.clear();
					adapter.setChecked(map);
					adapter.setTaskInfos(taskinfos);
					adapter.notifyDataSetChanged();
				}

				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		map = new HashMap<String, Boolean>();
		am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		setContentView(R.layout.task_manager);
		lv_task_manager = (ListView) this.findViewById(R.id.lv_task_manager);
		pd = new ProgressDialog(this);
		pm = getPackageManager();
		tv_task_manager_avail_memory = (TextView) this
				.findViewById(R.id.tv_task_manager_avail_memory);
		fillData();
		setTitle();
		// 设置点击事件
		lv_task_manager.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Object item = lv_task_manager.getItemAtPosition(position);
				if (item instanceof TaskInfo) {
					TaskInfo info = (TaskInfo) item;
					if ("system".equals(info.getPackname())
							|| "com.coco67.pntmonitor".equals(info.getPackname())) {
						return;
					}
					CheckBox cb = (CheckBox) view
							.findViewById(R.id.cb_task_checked);
					if (cb.isChecked()) {
						cb.setChecked(false);
						info.setIschecked(false);
						map.put(info.getPackname(), false);
						adapter.setChecked(map);
						adapter.notifyDataSetChanged();
					} else {
						cb.setChecked(true);
						info.setIschecked(true);
						map.put(info.getPackname(), true);
						adapter.setChecked(map);
						adapter.notifyDataSetChanged();
					}

				}
			}
		});

	}

	private void setTitle() {
		MemoryInfo outInfo = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(outInfo);
		float availMem = outInfo.availMem / 1024f / 1024f;
		DecimalFormat format = new DecimalFormat("###.00");
		String availMemStr = format.format(availMem);
		tv_task_manager_avail_memory.setText("可用内存 "+availMemStr + "MB");
	}

	private void fillData() {
		pd.setMessage("获取进程信息");
		pd.show();
		new Thread() {
			@Override
			public void run() {
				taskinfos = getRunningTaskInfo();
				Message msg = new Message();
				msg.what = LOAD_TASK_FINISHED;
				handler.sendMessage(msg);
			}
		}.start();

	}

	/**
	 * 获取当期系统正在运行的任务的列表
	 * 
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private List<TaskInfo> getRunningTaskInfo() {
		List<TaskInfo> taskinfos = new ArrayList<TaskInfo>();
		List<RunningAppProcessInfo> infos = am.getRunningAppProcesses();
		for (RunningAppProcessInfo info : infos) {
			TaskInfo taskinfo = new TaskInfo();
			try {
				int pid = info.pid;
				taskinfo.setPid(pid);
				int[] pids = { pid };
				android.os.Debug.MemoryInfo[] memoryinfos = am
						.getProcessMemoryInfo(pids);
				int kbmemory = memoryinfos[0].getTotalPrivateDirty();
				String memory = getMemoryString(kbmemory);
				taskinfo.setMemory(memory);
				String packname = info.processName;
				taskinfo.setPackname(packname);

				ApplicationInfo appinfo = pm.getApplicationInfo(packname, 0);
				boolean isuserapp = filterApp(appinfo);
				taskinfo.setIsuserapp(isuserapp);
				Drawable icon = appinfo.loadIcon(pm);
				taskinfo.setTaskicon(icon);
				String taskname = appinfo.loadLabel(pm).toString();
				taskinfo.setTaskname(taskname);
				taskinfos.add(taskinfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return taskinfos;
	}

	/**
	 * 获取内存信息对应的文本值
	 */

	private String getMemoryString(int kbmemory) {
		if (kbmemory < 1024) {
			return kbmemory + "KB";
		} else if (kbmemory < 1024 * 1024) { // MB
			float result = kbmemory / 1024f;
			DecimalFormat format = new DecimalFormat("###.00");
			return format.format(result);
		} else {
			return "内存信息错误";
		}
	}

	private boolean filterApp(ApplicationInfo info) {
		if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
			return true;
		} else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public void killTask(View view) {
		Set<Entry<String, Boolean>> set = map.entrySet();
		int count = 0;
		for (Entry<String, Boolean> entry : set) {
			if (entry.getValue()) {
				String packname = entry.getKey();
				am.restartPackage(packname);
				count++;
			}
		}
		MyToast.showMyToast(this, 0, "清理了"+count+"个进程");
		fillData();
	}
}
