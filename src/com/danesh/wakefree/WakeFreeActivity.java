package com.danesh.wakefree;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class WakeFreeActivity extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        if (!canSu()){
            showToast("No root found");
        }
        List<String> PARTIAL_WAKE_LOCK=new ArrayList<String>();
        String dumpsys = runCommand(new String[] {"su","-c","dumpsys power"});
        String filter = "";
        int numOfWakeLocks = 0;
        for (int iter = dumpsys.indexOf("mLocks.size");iter<=dumpsys.length();iter++){
            if (dumpsys.charAt(iter)!='\n'){
                filter+=dumpsys.charAt(iter);
            }else {
                if (!filter.contains(":"))
                    PARTIAL_WAKE_LOCK.add(filter);
                filter = "";
            }
            if (filter.contains("mPokeLocks.size")){
                filter = filter.substring(0,filter.length()-15);
                break;
            }else if (filter.contains("mLocks.size=")){
                numOfWakeLocks = Integer.parseInt(String.valueOf(dumpsys.charAt(iter+1)));
                filter="";
            }
        }
        List<String> PARTIAL_WAKE_LOCKA=new ArrayList<String>();
        for (String content : PARTIAL_WAKE_LOCK){
            int pid = Integer.parseInt(content.substring(content.indexOf("pid=")+4,content.length()-1));
            String abc =runCommand(new String[] {"su","-c","ps | grep "+pid}).split(" ")[8];
            PARTIAL_WAKE_LOCK.
        }
        
        this.setListAdapter(new ArrayAdapter<String>(this,    android.R.layout.simple_list_item_1, PARTIAL_WAKE_LOCK));
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

    public static boolean canSu() {
        String[] places = {"/sbin/","/system/bin/","/system/xbin/","/data/local/xbin/","/data/local/bin/","/system/sd/xbin/"};
        for (String where : places) {
            File file = new File(where + "su");
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }

    protected void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Object o = this.getListAdapter().getItem(position);
        String keyword = o.toString();
        Toast.makeText(this, "You selected: " + keyword, Toast.LENGTH_LONG).show();
    }

    String runCommand (String[] strings){
        try {

            Process process = Runtime.getRuntime().exec(strings);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } 
    }
}