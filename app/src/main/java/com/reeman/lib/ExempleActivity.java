package com.reeman.lib;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.reeman.serialport.controller.RobotActionController;
import com.reeman.serialport.controller.RosCallbackParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class ExempleActivity extends Activity implements RosCallbackParser.RosCallback {

    private TextView tvROSData, tvRefreshHostname, tvRefreshIP, tvCoreData;
    private RobotActionController controller;
    private String coreData = "";

    private Handler handler = new Handler();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exemple);
        tvROSData = findViewById(R.id.tv_ros_data);
        tvROSData.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvRefreshHostname = findViewById(R.id.tv_refresh_hostname);
        tvRefreshIP = findViewById(R.id.tv_refresh_ip);
        tvCoreData = findViewById(R.id.tv_core_data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller == null) {
            controller = RobotActionController.getInstance();
            try {
                controller.init(
                        115200,
                        ofChassis(Build.PRODUCT),
                        this,
                        com.reeman.serialport.BuildConfig.LOG_ROS,
                        BuildConfig.APP_LOG_DIR

                );
            } catch (Exception e) {
                e.printStackTrace();
                Timber.tag(BuildConfig.APP_LOG_DIR).e(e, "serial port open failed");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private String ofChassis(String product) {
        if ("YF3568_XXXE".equals(product)) {
            return "/dev/ttyS4";
        } else if ("rk3399_all".equals(product)) {
            return "/dev/ttyXRUSB0";
        } else {
            return "/dev/ttyS1";
        }
    }

    public void refreshHostname(View view) {
        tvRefreshHostname.setText("");
        tvRefreshHostname.postDelayed(() -> controller.getHostName(), 500);
    }

    public void refreshIP(View view) {
        tvRefreshIP.setText("");
        tvRefreshIP.postDelayed(() -> controller.getHostIp(), 500);
    }


    public void canNavigation(View view) {
        controller.sendCommand("footprint[0.65,0.85]");
        controller.sendCommand("set_tolerance[0]");
        tvROSData.postDelayed(() -> controller.sendCommand("get_plan_name[A]"), 1500);
    }

    public void navigationTOPointA(View view) {
        controller.navigationByPoint("A");
    }


    public void cancelNavigation(View view) {
        controller.cancelNavigation();
    }

    public void exit(View view) {
        controller.stopListen();
        finishAndRemoveTask();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void clean(View view) {
        tvROSData.setText("");
        tvROSData.scrollTo(0, 0);
    }

    @Override
    public void onResult(String result) {
        runOnUiThread(() -> {
            if (result.startsWith("sys:boot")) {
                tvRefreshHostname.setText(getString(R.string.text_ros_hostname, result.replace("sys:boot:", "")));
            } else if (result.startsWith("ip") || result.startsWith("wlan")) {
                if (result.startsWith("wlan")) {
                    tvRefreshIP.setText(getString(R.string.text_ros_ip, getString(R.string.text_not_connect), "127.0.0.1"));
                    return;
                }
                String[] split = result.split(":");
                if (split.length != 3 || result.contains("connecting")) {
                    tvRefreshIP.setText(getString(R.string.text_ros_ip, getString(R.string.text_not_connect), "127.0.0.1"));
                } else {
                    tvRefreshIP.setText(getString(R.string.text_ros_ip, split[1], split[2]));
                    //本地日志上传到ROS Upload local logs to ROS
                    controller.setIpAddress(split[2]);
                }
            } else if (result.startsWith("nav_result{")) {
                String replace = result.replace("nav_result{", "").replace("}", "");
                String[] split = replace.split(" ");
                if (split.length == 5) {
                    int state = Integer.parseInt(split[0]);
                    int code = Integer.parseInt(split[1]);
                    String name = split[2];
                    String distToGoal = split[3];
                    String mileage = split[4];
                    if (state == 6) {
                        if (code == 0) {
                            Timber.i("导航指令发送成功,机器状态正常,可以导航");
                        } else {
                            String startNavFailedReason = getStartNavFailedReason(code);
                            Timber.w(startNavFailedReason);
                            Toast.makeText(this, startNavFailedReason, Toast.LENGTH_SHORT).show();
                        }
                    } else if (state == 1) {
                        Toast.makeText(this, getString(R.string.text_start_navigation_success, distToGoal), Toast.LENGTH_SHORT).show();
                    } else if (state == 3) {
                        if (code == 0) {
                            String text = getString(R.string.text_navigation_success, name, mileage);
                            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "导航失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else if (result.startsWith("core_data")) {
                if (coreData.equals(result)) return;
                coreData = result;
                String[] split = result.replace("core_data{", "").replace("}", "").split(" ");
                if (split.length == 5) {
                    int bumper = Integer.parseInt(split[0]);
                    int cliff = Integer.parseInt(split[1]);
                    int button = Integer.parseInt(split[2]);
                    int battery = Integer.parseInt(split[3]);
                    int charger = Integer.parseInt(split[4]);
                    tvCoreData.setText(getString(R.string.text_core_data, battery, button, charger));
                }
            } else if (result.startsWith("get_plan:")) {
                if (result.equals("get_plan:error")) {
                    Toast.makeText(this, "生成路线失败", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "生成路线成功", Toast.LENGTH_SHORT).show();
                }
            } else if (result.startsWith("get_flag_point")) {
                if (result.equals("get_flag_point:-1")) {
                    if (sb.length() == 0) {
                        Toast.makeText(this, "找不到路线点", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String path = sb.deleteCharAt(sb.length() - 1).toString();
                    Toast.makeText(this, "path :" + path, Toast.LENGTH_SHORT).show();
                    controller.sendCommand("list_point[" + path + "]");
                } else {
                    String replace = result.replace("get_flag_point[", "").replace("]", "");
                    String[] split = replace.split(",");
                    sb.append(split[0]).append(",")
                            .append(split[1]).append(",")
                            .append(split[2]).append(",");
                    controller.sendCommand("nav:get_flag_point[" + (++point) + "]");
                }
            }else if (result.startsWith("robot_type:")){
                    Toast.makeText(this, getRobotType(result), Toast.LENGTH_SHORT).show();
            }
            String data = tvROSData.getText().toString();
            if (data.length() > 500) {
                clean(null);
            }
            tvROSData.setText(String.format("%s\n%s %s", data, formatDay(new Date()), result));
            int offset = tvROSData.getLineCount() * tvROSData.getLineHeight();
            if (offset > tvROSData.getHeight()) {
                tvROSData.scrollTo(0, offset - tvROSData.getHeight() + 20);
            }
        });
    }

    public String formatDay(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return formatter.format(date);
    }


    /**
     * @param code -1,-2,-3,-4,-9:所有导航模式都有可能出现以上原因;
     *             -5:只有AGV导航会出现该原因;
     *             -6,-7,-8:固定路线模式可能会出现以上原因;
     * @return
     */
    public String getStartNavFailedReason(int code) {
        String reason = "";
        switch (code) {
            case -1:
                reason = getString(R.string.text_start_navigation_failed_1);
                break;
            case -2:
                reason = getString(R.string.text_start_navigation_failed_2);
                break;
            case -3:
                reason = getString(R.string.text_start_navigation_failed_3);
                break;
            case -4:
                reason = getString(R.string.text_start_navigation_failed_4);
                break;
            case -5:
                reason = getString(R.string.text_start_navigation_failed_5);
                break;
            case -6:
                reason = getString(R.string.text_start_navigation_failed_6);
                break;
            case -7:
                reason = getString(R.string.text_start_navigation_failed_7);
                break;
            case -8:
                reason = getString(R.string.text_start_navigation_failed_8);
                break;
            case -9:
                reason = getString(R.string.text_start_navigation_failed_9);
                break;
        }
        return reason;
    }

    double speed = 0.1;
    int count = 0;

    public void speedControl(View view) {
        count = 0;
        speed = 0.1;
        speedControlRunnable.run();
    }

    Runnable speedControlRunnable = new Runnable() {
        @Override
        public void run() {
            if (++count > 20) {
                count = 0;
                speed = 0.1;
                return;
            }
            if (speed < 0.5) {
                speed = speed + 0.1;
            }
            controller.sendCommand("app_vel[" + speed + ",0.2]");
            Log.w("=====", "app_vel[" + speed + ",0.2]");
            handler.postDelayed(this, 100);
        }
    };

    StringBuilder sb;

    int point = 1;

    public void fixPointNavigation(View view) {
        sb = new StringBuilder();
        point = 1;
        controller.sendCommand("nav:get_flag_point[" + point + "]");
    }

    public void getRobotType(View view) {
        controller.sendCommand("robot_type");
    }

    private String getRobotType(String result){
        if (result.endsWith("1")){
            return "方形低盘";
        } else if (result.endsWith("2")) {
            return "送餐低盘";
        } else if (result.endsWith("3")) {
            return "消毒低盘";
        } else if (result.endsWith("4")) {
            return "AGV低盘";
        } else if (result.endsWith("5")) {
return "大狗低盘";
        }else {
            return "agv no qr底盘 "+result;
        }
    }

}
