/*Author: Abdukl Ghani
 * Website: https://abdulghani.tech
 * Github Repo: https://github.com/abdulghanitech/rpad-ml
 * */
package com.techglows.rpadml;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public boolean enabled;
    private static final String TAG = "RPAD_MainActivity";
    public Button btn;
    public TextView status;
    public final static int REQUEST_CODE = 676;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.isEnabled);
        status = findViewById(R.id.ProtectionStatus);
        accessibilityEnabledStatusCheck();
        //findViewById(R.id.buttonCreateWidget).setOnClickListener(this);


    }


    public void requestPermission(View v) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startService(new Intent(MainActivity.this, FloatingViewService.class));
            finish();
        } else if (Settings.canDrawOverlays(this)) {
            startService(new Intent(MainActivity.this, FloatingViewService.class));
            finish();
        } else {
            askPermission();
            Toast.makeText(this, "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
        }
    }

    public void accessibilityEnabledStatusCheck(){
        if(enabled = isAccessibilityServiceEnabled(getApplicationContext(), MyAccessibilityService.class)){
            //Enabled
            btn.setText("Disable");
            btn.setBackground(getResources().getDrawable(R.drawable.redgradient));
            status.setText("You're Protected!");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                askPermission();
            }

        }else{
            //disabled
            //show Enable btn
            btn.setText("Enable");
            btn.setBackground(getResources().getDrawable(R.drawable.gradient1));
            status.setText("You're NOT Protected!");
        }
    }

    public void checkEnabled(View view){
        if(enabled){
            //permission given
            Log.d(TAG,"accessibility permission given");
            getAccessibilityPermissions();
        }else{
            //not given show UI to give permission
            Log.d(TAG,"accessibility permission NOT given");
            getAccessibilityPermissions();
        }
    }

    public void getAccessibilityPermissions(){
        Log.d(TAG,"getting accessibility permissions!");
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, 0);
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }

    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        // after resuming the activity
        accessibilityEnabledStatusCheck();

    }
}
