package com.shenkangyun.healthcenter.MainPage.Activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jaeger.library.StatusBarUtil;
import com.shenkangyun.healthcenter.BaseFolder.AppConst;
import com.shenkangyun.healthcenter.BaseFolder.Base;
import com.shenkangyun.healthcenter.BeanFolder.ApkUpBean;
import com.shenkangyun.healthcenter.DBFolder.User;
import com.shenkangyun.healthcenter.LoginPage.LoginActivity;
import com.shenkangyun.healthcenter.R;
import com.shenkangyun.healthcenter.UtilFolder.APKVersionCodeUtils;
import com.shenkangyun.healthcenter.UtilFolder.GsonCallBack;
import com.zhy.http.okhttp.OkHttpUtils;

import org.json.JSONException;
import org.litepal.LitePal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PersonalActivity extends AppCompatActivity {

    @BindView(R.id.toolBar_title)
    TextView toolBarTitle;
    @BindView(R.id.toolBar)
    Toolbar toolBar;
    @BindView(R.id.tv_word)
    TextView tvWord;
    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.tv_version)
    TextView tvVersion;

    private String name;
    private String loginName;

    private SharedPreferences sp;
    private ProgressDialog pd;

    private String versionName;
    private String appVersion;
    private String appUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);
        ButterKnife.bind(this);
        StatusBarUtil.setColor(this, getResources().getColor(R.color.login_red));
        setSupportActionBar(toolBar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            toolBarTitle.setText("个人中心");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        initView();
    }

    private void initView() {
        sp = getSharedPreferences("config", MODE_PRIVATE);
        versionName = APKVersionCodeUtils.getVerName(this);
        List<User> all = LitePal.findAll(User.class);
        for (int i = 0; i < all.size(); i++) {
            name = all.get(i).getName();
            loginName = all.get(i).getLoginName();
        }

        if (!TextUtils.isEmpty(name)) {
            String firstWord = name.substring(0, 1);
            tvWord.setText(firstWord);
        } else {
            String firstWord = loginName.substring(0, 1);
            tvWord.setText(firstWord);
        }
        tvVersion.setText(versionName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @OnClick({R.id.tv_change, R.id.tv_logout, R.id.tv_update, R.id.tv_info})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_change:
                Intent intent = new Intent(this, ChangeWordActivity.class);
                startActivityForResult(intent, 0);
                break;
            case R.id.tv_logout:
                SharedPreferences.Editor edit = sp.edit();
                edit.putString(AppConst.LOGING_PASSWORD, "");
                edit.commit();
                Intent intentLog = new Intent();
                setResult(2, intentLog);
                Intent intentExit = new Intent(PersonalActivity.this, LoginActivity.class);
                startActivity(intentExit);
                finish();
                break;
            case R.id.tv_update:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 没有获得授权，申请授权
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, "请授权！", Toast.LENGTH_LONG).show();

                        // 帮跳转到该应用的设置界面，让用户手动授权
                        Intent intentSet = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                        intentSet.setData(uri);
                        intentSet.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intentSet);
                    } else {
                        // 不需要解释为何需要该权限，直接请求授权
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10000);
                    }
                } else {
                    // 已经获得授权，可以打电话
                    initUpdate();
                }
                break;
            case R.id.tv_info:
                Intent intentConsummate = new Intent(this, ChangeInfoActivity.class);
                startActivity(intentConsummate);
                break;
        }
    }

    private void initUpdate() {
        OkHttpUtils.post()
                .url(Base.URL)
                .addParams("act", "appVesionCompare")
                .addParams("data", new appVesionCompare(Base.appKey, Base.timeSpan, 1, "1", versionName).toJson())
                .build()
                .execute(new GsonCallBack<ApkUpBean>() {
                    @Override
                    public void onSuccess(ApkUpBean response) throws JSONException {
                        appVersion = response.getData().getData().getApp_parent_version();
                        appUrl = response.getData().getData().getApp_parent_version_url();
                        if (appVersion.equals(versionName)) {
                            Toast.makeText(PersonalActivity.this, "已是最新版本", Toast.LENGTH_SHORT).show();
                        } else {
                            showDialogUpdate();//弹出提示版本更新的对话框
                        }
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        // TODO Auto-generated method stub
        switch (requestCode) {
            case 10000: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 授权成功，继续打电话
                    initUpdate();
                } else {
                    // 授权失败！
                    Toast.makeText(this, "授权失败！", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showDialogUpdate() {
        // 这里的属性可以一直设置，因为每次设置后返回的是一个builder对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 设置提示框的标题
        builder.setTitle("版本升级").
                // 设置要显示的信息
                        setMessage("发现新版本！请及时更新").
                // 设置确定按钮
                        setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadNewVersionProgress();//下载最新的版本程序
                    }
                }).setNegativeButton("取消", null);

        // 生产对话框
        AlertDialog alertDialog = builder.create();
        // 显示对话框
        alertDialog.show();

    }

    private void loadNewVersionProgress() {
        //进度条对话框
        pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage("正在下载更新");
        pd.show();
        //启动子线程下载任务
        new Thread() {
            @Override
            public void run() {
                try {
                    File file = getFileFromServer(appUrl, pd);
                    sleep(3000);
                    installApk(file);
                    pd.dismiss(); //结束掉进度条对话框
                } catch (Exception e) {
                    //下载apk失败
                    Message message = new Message();
                    message.arg1 = 1;
                    handler.sendMessage(message);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1) {
                pd.dismiss();
                Toast.makeText(PersonalActivity.this, "下载新版本失败", Toast.LENGTH_LONG).show();
            }
        }
    };

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data;
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            data = FileProvider.getUriForFile(this, "com.shenkangyun.healthcenter.fileprovider", file);
            // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            data = Uri.fromFile(file);
        }
        intent.setDataAndType(data, "application/vnd.android.package-archive");
        startActivity(intent);
    }

    private File getFileFromServer(String appUrl, ProgressDialog pd) throws IOException {
        //如果相等的话表示当前的sdcard挂载在手机上并且是可用的
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            URL url = new URL(appUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            //获取到文件的大小
            pd.setMax(conn.getContentLength());
            InputStream is = conn.getInputStream();
            File file = new File(Environment.getExternalStorageDirectory(), "updata.apk");
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024];
            int len;
            int total = 0;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                total += len;
                //获取当前下载量
                pd.setProgress(total);
            }
            fos.close();
            bis.close();
            is.close();
            return file;
        } else {
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == 2) {
            Intent intent = new Intent();
            setResult(1, intent);
            finish();
        }
    }

    static class appVesionCompare {
        private String appKey;
        private String timeSpan;
        private int appType;
        private String mobileType;
        private String version;

        public appVesionCompare(String appKey, String timeSpan, int appType, String mobileType, String version) {
            this.appKey = appKey;
            this.timeSpan = timeSpan;
            this.appType = appType;
            this.mobileType = mobileType;
            this.version = version;
        }

        public String toJson() {
            return new Gson().toJson(this);
        }
    }
}
