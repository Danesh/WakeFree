package com.danesh.wakefree;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class WakeFreeActivity extends ListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);
		ShellCommand cmd = new ShellCommand();
		cmd.sh.runWaitFor("ps");
		cmd.su.runWaitFor("dumpsys power");
		this.setListAdapter(new ArrayAdapter<String>(this,	android.R.layout.simple_list_item_1, cmd.PARTIAL_WAKE_LOCK));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Object o = this.getListAdapter().getItem(position);
		String keyword = o.toString();
		Toast.makeText(this, "You selected: " + keyword, Toast.LENGTH_LONG)
		.show();
	}

	class ShellCommand {
		private static final String TAG = "ShellCommand.java";
		private Boolean can_su;    
		List<String> PARTIAL_WAKE_LOCK=new ArrayList<String>();
		List<String> SCREEN_DIM_WAKE_LOCK=new ArrayList<String>();
		List<String> SCREEN_BRIGHT_WAKE_LOCK=new ArrayList<String>();
		List<String> FULL_WAKE_LOCK=new ArrayList<String>();
		List<String> psLog = new ArrayList<String>();
		Boolean logSet = false;
		String ps = "";
		public SH sh;
		public SH su;

		public ShellCommand() {
			sh = new SH("sh");
			su = new SH("su");
		}

		public boolean canSU() {
			return canSU(false);
		}

		public boolean canSU(boolean force_check) {
			if (can_su == null || force_check) {
				CommandResult r = su.runWaitFor("id");
				StringBuilder out = new StringBuilder();
				if (r.stdout != null)
					out.append(r.stdout).append(" ; ");
				if (r.stderr != null)
					out.append(r.stderr);
				Log.v(TAG, "canSU() su[" + r.exit_value + "]: " + out);
				can_su = r.success();
			}
			return can_su;
		}

		public SH suOrSH() {
			return canSU() ? su : sh;
		}

		public class CommandResult {
			public final String stdout;
			public final String stderr;
			public final Integer exit_value;

			CommandResult(Integer exit_value_in, String stdout_in, String stderr_in)
			{
				exit_value = exit_value_in;
				stdout = stdout_in;
				stderr = stderr_in;
			}

			CommandResult(Integer exit_value_in) {
				this(exit_value_in, null, null);
			}

			public boolean success() {
				return exit_value != null && exit_value == 0;
			}
		}

		public class SH {
			private String SHELL = "sh";

			public SH(String SHELL_in) {
				SHELL = SHELL_in;
			}

			public Process run(String s) {
				Process process = null;
				try {
					process = Runtime.getRuntime().exec(SHELL);
					DataOutputStream toProcess = new DataOutputStream(process.getOutputStream());
					toProcess.writeBytes("exec " + s + "\n");
					toProcess.flush();
				} catch(Exception e) {

					process = null;
				}
				return process;
			}

			private String getPid(String out, int pos){
				return out.subSequence(pos, out.length()).toString().split("pid=")[1].replace(")", "");
			}

			private String getPackageName(String pid){
				int pos = -1;
				for (String a : psLog){
					if ((pos = a.indexOf(pid))!=-1){
						return a.substring(pos).toString().split("\\s+")[7];
					}
				}
				return "";
			}

			private String getAppName(String packageName){
				try {
					return  getPackageManager().getApplicationLabel(getPackageManager().getPackageInfo(packageName, 
							PackageManager.GET_ACTIVITIES).applicationInfo).toString();
				} catch (NameNotFoundException e) {
					return packageName;
				}
			}

			private String getStreamLines(InputStream is) {
				String out = null;
				int pos = -1;
				StringBuffer buffer = null;
				DataInputStream dis = new DataInputStream(is);
				try {
					if (dis.available() > 0) { 
						buffer = new StringBuffer(dis.readLine());
						while(dis.available() > 0){
							out = dis.readLine();
							if ((pos = out.indexOf("PARTIAL_WAKE_LOCK")) != -1){
								PARTIAL_WAKE_LOCK.add(getPid(out, pos));
								String packageName = getPackageName(getPid(out,pos));
								PARTIAL_WAKE_LOCK.add(getAppName(packageName));
							}else if ((pos = out.indexOf("SCREEN_DIM_WAKE_LOCK")) != -1){
								SCREEN_DIM_WAKE_LOCK.add(getPid(out, pos));
								SCREEN_DIM_WAKE_LOCK.add(getPackageName(getPid(out,pos)));
							}else if ((pos = out.indexOf("SCREEN_BRIGHT_WAKE_LOCK")) != -1){
								SCREEN_BRIGHT_WAKE_LOCK.add(getPid(out, pos));
								SCREEN_BRIGHT_WAKE_LOCK.add(getPackageName(getPid(out,pos)));
							}else if ((pos = out.indexOf("FULL_WAKE_LOCK")) != -1){
								FULL_WAKE_LOCK.add(getPid(out, pos));
								FULL_WAKE_LOCK.add(getPackageName(getPid(out,pos)));
							}
							if (!logSet)
								psLog.add(out);
							buffer.append("\n").append(out);
						}
					}
					dis.close();
				} catch (Exception ex) {
					Log.e(TAG, ex.getMessage());
				}
				if (buffer != null)
					out = buffer.toString();
				logSet = true;
				return out;
			}

			public CommandResult runWaitFor(String s) {
				Process process = run(s);
				Integer exit_value = null;
				String stdout = null;
				String stderr = null;
				if (process != null) {
					try {
						exit_value = process.waitFor();

						stdout = getStreamLines(process.getInputStream());
						stderr = getStreamLines(process.getErrorStream());

					} catch(InterruptedException e) {
						Log.e(TAG, "runWaitFor " + e.toString());
					} catch(NullPointerException e) {
						Log.e(TAG, "runWaitFor " + e.toString());
					}
				}
				return new CommandResult(exit_value, stdout, stderr);
			}
		}
	}
}