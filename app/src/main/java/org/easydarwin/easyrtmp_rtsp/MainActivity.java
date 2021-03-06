/*
	Copyright (c) 2012-2018 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/
package org.easydarwin.easyrtmp_rtsp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.easydarwin.easyrtmp.push.EasyRTMP;
import org.easydarwin.easyrtmp.push.InitCallback;
import org.easydarwin.easyrtmp.push.Pusher;
import org.easydarwin.video.EasyRTSPClient;
import org.easydarwin.video.RTSPClient;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    /*
    *本Key为3个月临时授权License，如需商业使用，请邮件至support@easydarwin.org申请此产品的授权。
    */
    public static final String EasyRTSPClient_KEY = "79393674363536526D3432416F75746170525263792B4E76636D63755A57467A65575268636E64706269356C59584E35636E52746346397964484E773456634D5671442F532B424859585A7062695A4359574A76633246414D6A41784E6B566863336C4559584A33615735555A5746745A57467A65513D3D";
    public static final String EasyRTMP_KEY = "79393674363536526D3432416F757461705252644A654E76636D63755A57467A65575268636E64706269356C59584E35636E52746346397964484E776246634D5671442F7065424859585A7062695A4359574A76633246414D6A41784E6B566863336C4559584A33615735555A5746745A57467A65513D3D";

    public EditText etRtspUrl;
    public EditText etRtmpUrl;
    public Button   btStartPush;
    public StatusInfoView mDbgInfoPrint;
    public LinearLayout mViewContainer;
    public TextView tvVideoInfo;

    protected EasyRTSPClient mStreamHamal;
    protected ResultReceiver mResultReceiver;
    protected Pusher mPusher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar tlToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(tlToolbar);

        mViewContainer = (LinearLayout)findViewById(R.id.option_bar_container);

        tvVideoInfo = (TextView)findViewById(R.id.tvVideoInfo);

        etRtspUrl = (EditText)findViewById(R.id.rtsp_url);
        etRtmpUrl = (EditText)findViewById(R.id.rtmp_url);
        String rtsp = EasyApplication.getEasyApplication().getRTSPUrl();
        String rtmp = EasyApplication.getEasyApplication().getRTMPUrl();
        etRtspUrl.setText(rtsp);
        etRtmpUrl.setText(rtmp);

        btStartPush = (Button)findViewById(R.id.btnStartPush);
        btStartPush.setOnClickListener(this);

        if(EasyApplication.getPushState()) {
            btStartPush.setText("停止推送");
        } else {
            btStartPush.setText("开始推送");
        }

        mDbgInfoPrint = (StatusInfoView)findViewById(R.id.tvEventMsg);
        initDbgInfoView();

        mResultReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                switch (resultCode){
                    case EasyRTSPClient.RESULT_VIDEO_SIZE:
                        int width = resultData.getInt(EasyRTSPClient.EXTRA_VIDEO_WIDTH);
                        int height = resultData.getInt(EasyRTSPClient.EXTRA_VIDEO_HEIGHT);
                        MainActivity.this.onEvent(String.format("Video Size: %d x %d", width, height));
                        break;
                    case EasyRTSPClient.RESULT_UNSUPPORTED_AUDIO:
                        new AlertDialog.Builder(MainActivity.this).setMessage("音频格式不支持").setTitle("SORRY").setPositiveButton(android.R.string.ok, null).show();
                        break;
                    case EasyRTSPClient.RESULT_UNSUPPORTED_VIDEO:
                        new AlertDialog.Builder(MainActivity.this).setMessage("视频格式不支持").setTitle("SORRY").setPositiveButton(android.R.string.ok, null).show();
                        break;
                    case EasyRTSPClient.RESULT_EVENT:
                        int errorcode = resultData.getInt("errorcode");
//                        if (errorcode != 0){
//                            StopPushing();
//                        }
                        MainActivity.this.onEvent(resultData.getString("event-msg"));
                        break;
                    case EasyRTMP.MSG.EasyRTMP_VideoInfo:
                        tvVideoInfo.setText(resultData.getString("event-msg"));
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnStartPush:
                if(EasyApplication.getPushState()) {
                    EasyApplication.setPushState(false);
                    StopPushing();
                    btStartPush.setText("开始推送");
                }else{
                    EasyApplication.setPushState(true);
                    String rtspValue = etRtspUrl.getText().toString();
                    String rtmpValue = etRtmpUrl.getText().toString();
                    if (TextUtils.isEmpty(rtspValue)) {
                        rtspValue = Config.DEFAULT_RTSP_URL;
                    }
                    if (TextUtils.isEmpty(rtmpValue)) {
                        rtmpValue = Config.DEFAULT_SERVER_URL;
                    }
                    EasyApplication.getEasyApplication().saveStringIntoPref(Config.RTSP_URL, rtspValue);
                    EasyApplication.getEasyApplication().saveStringIntoPref(Config.SERVER_URL, rtmpValue);

                    btStartPush.setText("停止推送");

                    StartPushing();
                }
                break;
        }
    }

    private void initDbgInfoView() {
        if (mDbgInfoPrint == null)
            return;
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        mViewContainer.measure(w, h);
        int height = mViewContainer.getMeasuredHeight();
        int width = mViewContainer.getMeasuredWidth();

        int[] location = new int[2];
        mViewContainer.getLocationOnScreen(location);

        ViewGroup.LayoutParams lp = mDbgInfoPrint.getLayoutParams();
        lp.height = getResources().getDisplayMetrics().heightPixels - height - location[1] - 300;
        mDbgInfoPrint.setLayoutParams(lp);
        mDbgInfoPrint.requestLayout();
        mDbgInfoPrint.setInstence(mDbgInfoPrint);
    }

    public void onEvent(String msg) {
        Intent intent = new Intent(StatusInfoView.DBG_MSG);
        intent.putExtra(StatusInfoView.DBG_DATA, String.format("[%s]\t%s\n",new SimpleDateFormat("HH:mm:ss").format(new Date()),msg));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void StartPushing(){
        mStreamHamal = new EasyRTSPClient(this, EasyRTSPClient_KEY, null, mResultReceiver);
        String rtsp = EasyApplication.getEasyApplication().getRTSPUrl();
        String rtmp = EasyApplication.getEasyApplication().getRTMPUrl();
        mPusher = new EasyRTMP(mResultReceiver);
        mStreamHamal.setRTMPInfo(mPusher, rtmp, EasyRTMP_KEY, new InitCallback(){

            @Override
            public void onCallback(int code) {
                Bundle resultData = new Bundle();
                switch (code) {
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_INVALID_KEY:
                        resultData.putString("event-msg", "EasyRTMP 无效Key");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_SUCCESS:
                        resultData.putString("event-msg", "EasyRTMP 激活成功");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTING:
                        resultData.putString("event-msg", "EasyRTMP 连接中");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTED:
                        resultData.putString("event-msg", "EasyRTMP 连接成功");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_FAILED:
                        resultData.putString("event-msg", "EasyRTMP 连接失败");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_ABORT:
                        resultData.putString("event-msg", "EasyRTMP 连接异常中断");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_PUSHING:
                        resultData.putString("event-msg", "EasyRTMP 推流中");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_DISCONNECTED:
                        resultData.putString("event-msg", "EasyRTMP 断开连接");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PLATFORM_ERR:
                        resultData.putString("event-msg", "EasyRTMP 平台不匹配");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_COMPANY_ID_LEN_ERR:
                        resultData.putString("event-msg", "EasyRTMP 断授权使用商不匹配");
                        break;
                    case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PROCESS_NAME_LEN_ERR:
                        resultData.putString("event-msg", "EasyRTMP 进程名称长度不匹配");
                        break;
                }
                mResultReceiver.send(EasyRTSPClient.RESULT_EVENT, resultData);
            }
        });
        mStreamHamal.start(rtsp, RTSPClient.TRANSTYPE_TCP, RTSPClient.EASY_SDK_VIDEO_FRAME_FLAG | RTSPClient.EASY_SDK_AUDIO_FRAME_FLAG, "", "");
    }

    void StopPushing(){
        if(mStreamHamal != null) {
            mStreamHamal.stop();
            mStreamHamal = null;
        }
    }
}
