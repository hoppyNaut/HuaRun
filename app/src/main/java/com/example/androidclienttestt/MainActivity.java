package com.example.androidclienttestt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.DiscretePathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rubensousa.previewseekbar.PreviewLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    private Theme theme = Theme.BLACK;
    private VideoState videoState = VideoState.STOP;
    private VideoType videoType = VideoType.None;

    private static String ServerIP = "";
    private static int ServerPort = 1234;

    private SharedPreferences mSharedPreferences;

    private Socket socket = null;
    private String strMessage;
    private boolean isConnect = false;
    private boolean isReceive = false;
    private boolean isVideoProgress = false;
    private OutputStream outStream;

    private ReceiveThread receiveThread = null;
    private VideoProgressThread videoProgressThread = null;

    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            //显示接收到的数据
            super.handleMessage(msg);

            switch (msg.what)
            {
                case MSG_InitId:
                    ChangeVolumn((String)msg.obj);
                    break;
                case MSG_TimeId:
                    InitTime();
                    break;
                case MSG_VolumnId:
                    ChangeVolumn((String) msg.obj);
                    break;
                case MSG_UpdateProgressId:
                    String[] info = ((String) msg.obj).split("/");
                    System.out.println(info[0] + ":" + info[1]);
                    if(info.length > 1 && info[0] != null && info[1] != null)
                    {
                        float curTime = Float.parseFloat(info[0]);
                        float totalTime = Float.parseFloat(info[1]);
                        SetVideoCurLength(curTime);
                        SetVideoTotalLength(totalTime);
                    }
                    break;
                case MSG_SucceedConnectId:
                    Toast.makeText(MainActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                    break;
                case MSG_FailConnectId:
                    Toast.makeText(MainActivity.this,"连接失败,请确认服务器已连接或IP是否正确",Toast.LENGTH_LONG).show();
                    ShowIpDialog();
                    break;
                case MSG_DisConnectId:
                    System.out.println("Disconnect");
                    //Toast.makeText(MainActivity.this,"服务端关闭，连接断开",Toast.LENGTH_LONG).show();
                    break;
                case MSG_VideoIndexId:
                    int videoIndex = Integer.parseInt((String) msg.obj) ;
                    switch (VideoType.values()[videoIndex + 1])
                    {
                        case None:
                            videoType = VideoType.None;
                            image_Picture.setImageResource(R.drawable.picturenormal);
                            break;
                        case First:
                            videoType = VideoType.First;
                            image_Picture.setImageResource(R.drawable.picturescene1);
                            break;
                        case Second:
                            videoType = VideoType.Second;
                            image_Picture.setImageResource(R.drawable.picturescene2);
                            break;
                    }
                    break;
                //ase MSG_PlayOrPauseId:
                //   int isPlaying = Integer.parseInt((String) msg.obj);
                //   if(isPlaying == 1)
                //   {
                //       switch (theme)
                //       {
                //           case BLACK:
                //               btn_PauseOrPlay.setBackgroundResource(R.drawable.pausewhite);
                //               break;
                //           case WHITE:
                //               btn_PauseOrPlay.setBackgroundResource(R.drawable.pauseblack);
                //               break;
                //       }
                //   }
                //   else if(isPlaying == 0)
                //   {
                //       switch (theme)
                //       {
                //           case BLACK:
                //               btn_PauseOrPlay.setBackgroundResource(R.drawable.playwhite2);
                //               break;
                //           case WHITE:
                //               btn_PauseOrPlay.setBackgroundResource(R.drawable.playblack);
                //               break;
                //       }
                //   }
                //   break;
                default:
                    break;
            }



        }
    };


    public static final int MSG_InitId = 0;
    public static final int MSG_TimeId = 1;
    public static final int MSG_VolumnId = 2;
    public static final int MSG_UpdateProgressId = 3;
    public static final int MSG_SucceedConnectId = 4;
    public static final int MSG_FailConnectId = 5;
    public static final int MSG_DisConnectId = 6;
    public static final int MSG_VideoIndexId = 7;
    public static final int MSG_PlayOrPauseId = 8;

    //消息格式：消息类型|传递的数据（没有数据传递即为空）
    public static final String MSG_Init = "Init|";
    public static final String MSG_Scene1 = "Scene1|";
    public static final String MSG_Scene2 = "Scene2|";
    public static final String MSG_SceneMode1 = "SceneMode1|";
    public static final String MSG_SceneMode2 = "SceneMode2|";
    public static final String MSG_SceneMode3 = "SceneMode3|";
    public static final String MSG_SceneMode4 = "SceneMode4|";
    public static final String MSG_PlayOrPause = "Play Or Pause|";
    public static final String MSG_Stop = "Stop|";
    public static final String MSG_PrevVideo = "Prev Video|";
    public static final String MSG_NextVideo = "Next Video|";
    public static final String MSG_TurnUp = "Turn Up|";
    public static final String MSG_TurnDown = "Turn Down|";
    public static final String MSG_GetVideoProgress = "Get Video Progress|";
    public static final String MSG_ChangeVideoProgress = "Change Video Progress|";
    public static final String MSG_DisConnect = "DisConnect|";

    private TextView txt_Date;
    private TextView txt_Time;
    private TextView txt_VideoTotalTime;
    private TextView txt_VideoCurTime;

    private RelativeLayout layout_Left;
    private LinearLayout layout_Bg;

    private ImageView image_Volumn;
    private ImageView image_VideoController;
    private ImageView image_Picture;
    private ImageView image_PauseOrPlay;

    private ImageButton btn_Scene1;
    private ImageButton btn_Scene2;
    private ImageButton btn_CustomMode;
    private ImageButton btn_SceneMode1;
    private ImageButton btn_SceneMode2;
    private ImageButton btn_SceneMode3;
    private ImageButton btn_SceneMode4;
    private ImageButton btn_Light1;
    private ImageButton btn_Light2;
    private ImageButton btn_PhotoMode;
    private ImageButton btn_Increase;
    private ImageButton btn_Decrease;
    private ImageButton btn_PauseOrPlay;
    private ImageButton btn_PreVideo;
    private ImageButton btn_NextVideo;
    private ImageButton btn_Item;
    private ImageButton btn_Stop;

    private SeekBar videoBar;
    private com.littlejie.circleprogress.DialProgress dialProgress;

    private PowerManager pm;
    private PowerManager.WakeLock wl;
    Runnable connectThread = new Runnable() {
        @Override
        public void run() {
            try{
                MainActivity.this.socket  =  new Socket(ServerIP,ServerPort);
                MainActivity.this.isConnect = true;
                MainActivity.this.isReceive = true;
                MainActivity.this.isVideoProgress = true;
                //初始化并开启接受线程
                MainActivity.this.receiveThread = new ReceiveThread(socket);
                MainActivity.this.receiveThread.start();
                System.out.println("----Connect Succeed-----");
                //获取当前音量
                new Thread(new SendThread(MSG_Init)).start();
                //开启更新视频进度线程
                MainActivity.this.videoProgressThread =  new VideoProgressThread();
                MainActivity.this.videoProgressThread.start();
                ////存储ip和port
                //SharedPreferences.Editor edit = mSharedPreferences.edit();
                //edit.putString("ip",ServerIP);
                //edit.putString("port",Integer.toString(ServerPort));
                //edit.commit();
                //发送消息更新UI
                Message msg = new Message();
                msg.what = MSG_SucceedConnectId;
                myHandler.sendMessage(msg);
            }catch (UnknownHostException var2)
            {
                Looper.prepare();
                var2.printStackTrace();
                System.out.println("UnknownHostException-->" + var2.toString());
                Message msg = new Message();
                msg.what = MSG_FailConnectId;
                myHandler.sendMessage(msg);

                Looper.loop();
            } catch (IOException var3) {
                Looper.prepare();
                var3.printStackTrace();
                System.out.println("IOException" + var3.toString());
                Message msg = new Message();
                msg.what = MSG_FailConnectId;
                myHandler.sendMessage(msg);
                Looper.loop();
            }

            //TODO
            //do{
            //    try{
            //            //发送心跳包判断连接
            //            socket.sendUrgentData(0xFF);
            //            Thread.sleep(5000);
            //            System.out.println("send heart");
            //    } catch (IOException e) {
            //        DisConnect();
            //        e.printStackTrace();
            //    } catch (InterruptedException e) {
            //        DisConnect();
            //        e.printStackTrace();
            //    }
            //}while(isConnect);
        }
    };


    Runnable sendThread = new Runnable() {
        @Override
        public void run() {
            byte[] sendBuffer= null;
            try{
                sendBuffer = MainActivity.this.strMessage.getBytes("UTF-8");
            }
            catch (UnsupportedEncodingException var5)
            {
                var5.printStackTrace();
            }

            try{
                MainActivity.this.outStream = MainActivity.this.socket.getOutputStream();
            }
            catch (IOException var4)
            {
                var4.printStackTrace();
            }

            try{
                MainActivity.this.outStream.write(sendBuffer);
            }
            catch (IOException var3)
            {
                var3.printStackTrace();
            }
        }
    };

    public MainActivity(){}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"myapp:mywakelocktag");
        wl.acquire();
        //System.out.println(1);

        //应用不锁屏
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSharedPreferences = getSharedPreferences("ipInfo",MODE_PRIVATE);
        //ShowIpDialog();
        InitComponent();
        InitTime();
        if(theme == Theme.BLACK)
        {
            SetBlackTheme();
        }
        else if(theme == Theme.WHITE)
        {
            SetWhiteTheme();
        }
        //开启计时线程
        new TimeThread().start();

        videoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int curTime = seekBar.getProgress();
                String msg = MSG_ChangeVideoProgress + curTime;
                new Thread(new SendThread(msg)).start();
            }
        });


        btn_Scene1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                switch (theme)
                {
                    case BLACK:
                        btn_PauseOrPlay.setBackgroundResource(R.drawable.playwhite2);
                        break;
                    case WHITE:
                        btn_PauseOrPlay.setBackgroundResource(R.drawable.playblack);
                        break;
                }
                videoState = VideoState.STOP;
                videoType = VideoType.First;
                image_Picture.setImageResource(R.drawable.picturescene1);
                //new Thread(new SendThread(MSG_Scene1)).start();
            }
        });

        btn_Scene2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                switch (theme)
                {
                    case BLACK:
                        btn_PauseOrPlay.setBackgroundResource(R.drawable.playwhite2);
                        break;
                    case WHITE:
                        btn_PauseOrPlay.setBackgroundResource(R.drawable.playblack);
                        break;
                }
                videoState = VideoState.STOP;
                videoType = VideoType.Second;
                image_Picture.setImageResource(R.drawable.picturescene2);
                //new Thread(new SendThread(MSG_Scene2)).start();
            }
        });

        //切换交互场景一按钮事件
        btn_SceneMode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                videoState = VideoState.STOP;
                videoType = VideoType.None;
                image_Picture.setImageResource(R.drawable.pictureinteraction1);
                new Thread(new SendThread(MSG_SceneMode1)).start();
            }
        });

        btn_SceneMode2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                videoState = VideoState.STOP;
                videoType = VideoType.None;
                image_Picture.setImageResource(R.drawable.pictureinteraction2);
                new Thread(new SendThread(MSG_SceneMode2)).start();
            }
        });

        btn_SceneMode3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                videoState = VideoState.STOP;
                videoType = VideoType.None;
                image_Picture.setImageResource(R.drawable.pictureinteraction3);
                new Thread(new SendThread(MSG_SceneMode3)).start();
            }
        });

        btn_SceneMode4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                videoState = VideoState.STOP;
                videoType = VideoType.None;
                image_Picture.setImageResource(R.drawable.picturenormal);
                new Thread(new SendThread(MSG_SceneMode4)).start();
            }
        });

        btn_CustomMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowIpDialog();

            }
        });

        //播放/暂停按钮事件
        btn_PauseOrPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                if(videoState == VideoState.STOP)
                {
                    videoState = VideoState.PLAY;
                    //TODO 更换图标
                    switch (theme)
                    {
                        case BLACK:
                            btn_PauseOrPlay.setBackgroundResource(R.drawable.pausewhite);
                            break;
                        case WHITE:
                            btn_PauseOrPlay.setBackgroundResource(R.drawable.pauseblack);
                            break;
                    }

                    switch (videoType)
                    {
                        case None:
                            videoType = VideoType.First;
                            image_Picture.setImageResource(R.drawable.picturescene1);
                            new Thread(new SendThread(MSG_Scene1)).start();
                            break;
                        case First:
                            videoType = VideoType.First;
                            new Thread(new SendThread(MSG_Scene1)).start();
                            break;
                        case Second:
                            videoType = VideoType.Second;
                            new Thread(new SendThread(MSG_Scene2)).start();
                            break;
                    }
                }
                else if(videoState == VideoState.PLAY)
                {
                    switch (theme)
                    {
                        case BLACK:
                            btn_PauseOrPlay.setBackgroundResource(R.drawable.playwhite2);
                            break;
                        case WHITE:
                            btn_PauseOrPlay.setBackgroundResource(R.drawable.playblack);
                            break;
                    }
                    videoState = VideoState.PAUSE;
                    new Thread(new SendThread(MSG_PlayOrPause)).start();
                }
                else if(videoState == VideoState.PAUSE)
                {
                    switch (theme)
                    {
                        case BLACK:
                            btn_PauseOrPlay.setBackgroundResource(R.drawable.pausewhite);
                            break;
                        case WHITE:
                            btn_PauseOrPlay.setBackgroundResource(R.drawable.pauseblack);
                            break;
                    }
                    videoState = VideoState.PLAY;
                    new Thread(new SendThread(MSG_PlayOrPause)).start();
                }
            }
        });

        //停止播放事件
        btn_Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                switch (theme)
                {
                    case BLACK:
                        btn_PauseOrPlay.setBackgroundResource(R.drawable.playwhite2);
                        break;
                    case WHITE:
                        btn_PauseOrPlay.setBackgroundResource(R.drawable.playblack);
                        break;
                }
                videoType = VideoType.None;
                videoState = VideoState.STOP;
                image_Picture.setImageResource(R.drawable.picturenormal);
                new Thread(new SendThread(MSG_Stop)).start();
            }
        });

        //切换上个视频事件
        btn_PreVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                videoState = VideoState.PLAY;
                //switch (videoType)
                //{
                //    case None:
                //        videoType = VideoType.Second;
                //        image_Picture.setImageResource(R.drawable.picturescene2);
                //        break;
                //    case First:
                //        videoType = VideoType.Second;
                //        image_Picture.setImageResource(R.drawable.picturescene2);
                //        break;
                //    case Second:
                //        videoType = VideoType.First;
                //        image_Picture.setImageResource(R.drawable.picturescene1);
                //        break;
                //}
                //image_Picture.setImageResource(videoType == VideoType.First ? R.drawable.picturescene2:R.drawable.picturescene1);
                new Thread(new SendThread(MSG_PrevVideo)).start();
            }
        });
//
        //切换下个视频按钮事件
        btn_NextVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                videoState = VideoState.PLAY;
                //switch (videoType)
                //{
                //    case None:
                //        videoType = VideoType.First;
                //        image_Picture.setImageResource(R.drawable.picturescene1);
                //        break;
                //    case First:
                //        videoType = VideoType.Second;
                //        image_Picture.setImageResource(R.drawable.picturescene2);
                //        break;
                //    case Second:
                //        videoType = VideoType.First;
                //        image_Picture.setImageResource(R.drawable.picturescene1);
                //        break;
                //}
                //image_Picture.setImageResource(videoType == VideoType.Second ? R.drawable.picturescene2:R.drawable.picturescene1);
                new Thread(new SendThread(MSG_NextVideo)).start();
            }
        });
//
        //调大音量事件
        btn_Increase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                new Thread(new SendThread(MSG_TurnUp)).start();
            }
        });
//
        //调小音量事件
        btn_Decrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnect)
                    return;
                new Thread(new SendThread(MSG_TurnDown)).start();
            }
        });

        btn_PhotoMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(theme == Theme.BLACK)
                {
                    SetWhiteTheme();
                }
                else if(theme == Theme.WHITE)
                {
                    SetBlackTheme();
                }
            }
        });

    }

    protected void onDestroy() {
        super.onDestroy();
        if(isConnect)
        {
            DisConnect();
        }
        wl.release();

        //if (this.receiveThread != null) {
        //    this.isReceive = false;
        //    this.receiveThread.interrupt();
        //}

    }

    public void DisConnect()
    {
        try{
            Message msg = new Message();
            msg.what = MSG_DisConnectId;
            myHandler.sendMessage(msg);
            isConnect = false;
            if (this.receiveThread != null) {
                this.isReceive = false;
                this.receiveThread.interrupt();
                this.receiveThread = null;
            }
            if(this.videoProgressThread != null)
            {
                this.isVideoProgress = false;
                this.videoProgressThread.interrupt();
                this.videoProgressThread = null;
            }
            socket.close();
            System.out.println("DisConnect");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ShowIpDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("请输入Ip和端口地址");
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog,null);
        builder.setView(view);

        final EditText ip = view.findViewById(R.id.Txt_Ip);
        final EditText port = view.findViewById(R.id.Txt_Port);

        //获取数据
        ip.setText(mSharedPreferences.getString("ip",""));
        port.setText(mSharedPreferences.getString("port",""));

        builder.setPositiveButton("连接", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ServerIP = ip.getText().toString().trim();
                ServerPort = Integer.parseInt(port.getText().toString().trim());
                //存储ip和port
                SharedPreferences.Editor edit = mSharedPreferences.edit();
                edit.putString("ip",ServerIP);
                edit.putString("port",port.getText().toString().trim());
                edit.commit();
                //开启线程连接服务器
                if (!MainActivity.this.isConnect)
                {
                    (new Thread(MainActivity.this.connectThread)).start();
                }
                else{
                    //Toast.makeText(MainActivity.this, "已有连接,请先断开当前连接后重连", Toast.LENGTH_SHORT).show();
                    //断开当前连接
                    DisConnect();
                    (new Thread(MainActivity.this.connectThread)).start();
                }
            }
        });
        builder.show();
    }


    private void GetIpAndPort() throws IOException {
        String ipInfo =  ReadIpConfigFile(this,"ipconfig");
        System.out.println(ipInfo);
        String[] infoArray = ipInfo.split(":");
        ServerIP = infoArray[0];
        ServerPort = Integer.parseInt(infoArray[1]);
    }

    private void InitComponent()
    {
        txt_Date = findViewById(R.id.Text_Date);
        txt_Time = findViewById(R.id.Text_Time);
        txt_VideoCurTime = findViewById(R.id.Text_VideoCurTime);
        txt_VideoTotalTime = findViewById(R.id.Text_VideoTotalTime);
        layout_Left = findViewById(R.id.Image_Left);
        layout_Bg = findViewById(R.id.Layout_Bg);

        image_Volumn = findViewById(R.id.Image_Volume);
        image_VideoController = findViewById(R.id.Image_VideoController);
        image_Picture = findViewById(R.id.Image_Picture);

        btn_Scene1 = findViewById(R.id.Btn_Scene1);
        btn_Scene2 = findViewById(R.id.Btn_Scene2);
        btn_CustomMode = findViewById(R.id.Btn_CustomMode);
        btn_SceneMode1 = findViewById(R.id.Btn_SceneMode1);
        btn_SceneMode2 = findViewById(R.id.Btn_SceneMode2);
        btn_SceneMode3 = findViewById(R.id.Btn_SceneMode3);
        btn_SceneMode4 = findViewById(R.id.Btn_SceneMode4);
        btn_Light1 = findViewById(R.id.Btn_Light1);
        btn_Light2 = findViewById(R.id.Btn_Light2);
        btn_PhotoMode = findViewById(R.id.Btn_PhotoMode);
        btn_Increase = findViewById(R.id.Btn_Increase);
        btn_Decrease = findViewById(R.id.Btn_Decrease);

        dialProgress = findViewById(R.id.dial_progress_bar);

        btn_PauseOrPlay = findViewById(R.id.Btn_PauseOrPlay);
        btn_PreVideo = findViewById(R.id.Btn_PreVideo);
        btn_NextVideo = findViewById(R.id.Btn_NextVideo);
        btn_Stop = findViewById(R.id.Btn_Stop);
        btn_Item = findViewById(R.id.Btn_Item);

        videoBar = findViewById(R.id.SeekBar);

    }

    private void SetWhiteTheme()
    {
        theme = Theme.WHITE;
        //layout_Bg.setBackgroundResource(R.drawable.blackbg);
        layout_Bg.setBackgroundColor(Color.parseColor("#FFFFFF"));
        layout_Left.setBackgroundResource(R.drawable.leftwhite);

        txt_Time.setTextColor(this.getResources().getColor(R.color.colorGray));
        txt_Date.setTextColor(this.getResources().getColor(R.color.colorGray));
        txt_VideoTotalTime.setTextColor(this.getResources().getColor(R.color.colorGray));
        txt_VideoCurTime.setTextColor(this.getResources().getColor(R.color.colorGray));

        btn_Scene1.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_Scene1.setImageResource(R.drawable.scene1black);
        btn_Scene2.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_Scene2.setImageResource(R.drawable.scene2black);
        btn_CustomMode.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_CustomMode.setImageResource(R.drawable.custommodeblack);
        btn_SceneMode1.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_SceneMode1.setImageResource(R.drawable.scenemode1black);
        btn_SceneMode2.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_SceneMode2.setImageResource(R.drawable.scenemode2black);
        btn_SceneMode3.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_SceneMode3.setImageResource(R.drawable.scenemode3black);
        btn_SceneMode4.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_SceneMode4.setImageResource(R.drawable.scenemode4black);
        btn_Light1.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_Light1.setImageResource(R.drawable.light1black);
        btn_Light2.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_Light2.setImageResource(R.drawable.light2black);
        btn_PhotoMode.setBackgroundResource(R.drawable.whitescenebtnpress);
        btn_PhotoMode.setImageResource(R.drawable.photomodeblack);
        if(videoState == VideoState.PLAY)
        {
            btn_PauseOrPlay.setBackgroundResource(R.drawable.pauseblack);
        }
        else
        {
            btn_PauseOrPlay.setBackgroundResource(R.drawable.playblack);
        }
        btn_Increase.setBackgroundResource(R.drawable.blackincreasebtnpress);
        btn_Decrease.setBackgroundResource(R.drawable.blackdecreasebtnpress);
        image_Volumn.setImageResource(R.drawable.volumeblack);
        image_VideoController.setBackgroundResource(R.drawable.videocontrollerblack2);

        //dialProgress.setValueColor(R.color.colorWhite);
    }

    private void SetBlackTheme()
    {
        theme = Theme.BLACK;
        //layout_Bg.setBackgroundResource(R.drawable.blackbg);
        layout_Bg.setBackgroundColor(Color.parseColor("#000000"));
        layout_Left.setBackgroundResource(R.drawable.left3);

        txt_Time.setTextColor(this.getResources().getColor(R.color.colorBlack));
        txt_Date.setTextColor(this.getResources().getColor(R.color.colorBlack));
        txt_VideoTotalTime.setTextColor(this.getResources().getColor(R.color.colorBlack));
        txt_VideoCurTime.setTextColor(this.getResources().getColor(R.color.colorBlack));

        btn_Scene1.setBackgroundResource(R.drawable.scenebtnpress);
        btn_Scene1.setImageResource(R.drawable.scene1white);
        btn_Scene2.setBackgroundResource(R.drawable.scenebtnpress);
        btn_Scene2.setImageResource(R.drawable.scene2white);
        btn_CustomMode.setBackgroundResource(R.drawable.scenebtnpress);
        btn_CustomMode.setImageResource(R.drawable.custommodewhite);
        btn_SceneMode1.setBackgroundResource(R.drawable.scenebtnpress);
        btn_SceneMode1.setImageResource(R.drawable.scenemode1white);
        btn_SceneMode2.setBackgroundResource(R.drawable.scenebtnpress);
        btn_SceneMode2.setImageResource(R.drawable.scenemode2white);
        btn_SceneMode3.setBackgroundResource(R.drawable.scenebtnpress);
        btn_SceneMode3.setImageResource(R.drawable.scenemode3white);
        btn_SceneMode4.setBackgroundResource(R.drawable.scenebtnpress);
        btn_SceneMode4.setImageResource(R.drawable.scenemode4white);
        btn_Light1.setBackgroundResource(R.drawable.scenebtnpress);
        btn_Light1.setImageResource(R.drawable.light1white);
        btn_Light2.setBackgroundResource(R.drawable.scenebtnpress);
        btn_Light2.setImageResource(R.drawable.light2white);
        btn_PhotoMode.setBackgroundResource(R.drawable.scenebtnpress);
        btn_PhotoMode.setImageResource(R.drawable.photomodewhite);
        if(videoState == VideoState.PLAY)
        {
            btn_PauseOrPlay.setBackgroundResource(R.drawable.pausewhite);
        }
        else
        {
            btn_PauseOrPlay.setBackgroundResource(R.drawable.playwhite2);
        }

        btn_Increase.setBackgroundResource(R.drawable.increasebtnpress);
        btn_Decrease.setBackgroundResource(R.drawable.decreasebtnpress);
        image_Volumn.setImageResource(R.drawable.volume);
        image_VideoController.setBackgroundResource(R.drawable.videocontrollerwhite);

        //dialProgress.setValueColor(R.color.colorBlack);
    }

    private void InitTime()
    {
        //获取日期与时间
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        String timeInfo = new String();
        if(hour < 10)
        {
            timeInfo += "0";
        }
        timeInfo += hour + ":";
        if(minute < 10)
        {
            timeInfo += "0";
        }
        timeInfo += minute;

        String dateInfo = new String();
        if(month < 10)
        {
            dateInfo += "0";
        }
        dateInfo += month + "/";
        if(day < 10)
        {
            dateInfo += "0";
        }
        dateInfo += day;
        txt_Date.setText(dateInfo);
        txt_Time.setText(timeInfo);
    }

    private void ChangeVolumn(String volumn)
    {
        dialProgress.setValue(Float.parseFloat(volumn));
    }

    private void SetVideoTotalLength(float ms)
    {
        System.out.println("VideoTotalLength:" + ms);
        int second = (int) (ms / 1000);
        videoBar.setMax(second);
        int minute = second / 60;
        int restSecond = second % 60;
        txt_VideoTotalTime.setText(ConvertIntToDateTime(minute,restSecond));
    }

    private void SetVideoCurLength(float ms)
    {
        System.out.println("VideoCurLength:" + ms);
        int second = (int) (ms / 1000);
        videoBar.setProgress(second);
        int minute = second / 60;
        int restSecond = second % 60;
        txt_VideoCurTime.setText(ConvertIntToDateTime(minute,restSecond));
    }

    private String ConvertIntToDateTime(int minute,int restSecond)
    {
        String msg = new String();
        if(minute < 10)
        {
            msg += "0";
        }
        msg += (minute + ":");
        if(restSecond < 10)
        {
            msg += "0";
        }
        msg += restSecond;
        return msg;
    }

    public String ReadIpConfigFile(Context context, String filename) throws IOException {
        StringBuilder sb = new StringBuilder("");

        //获取文件在内存卡中files目录下的路径
        File file = context.getFilesDir();
        filename = file.getAbsolutePath() + File.separator + filename;
        System.out.println(filename);

        File targetFile = new File(filename);
        if (!targetFile.exists()) {
            try {
                FileOutputStream fos = openFileOutput(filename, Activity.MODE_PRIVATE);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
                osw.write(new String("172.20.10.4:1234"));
                fos.flush();
                osw.flush();
                osw.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //打开文件输入流
        try {
            FileInputStream inputStream = openFileInput(filename);
            byte[] buffer = new byte[1024];
            int len = inputStream.read(buffer);
            //读取文件内容
            while (len > 0) {
                sb.append(new String(buffer, 0, len));
                //继续将数据放到buffer中
                len = inputStream.read(buffer);

            }
            //关闭输入流
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    //接收线程
    private class ReceiveThread extends Thread{
        private InputStream inStream = null;
        private byte[] buffer;
        private String str = null;

        ReceiveThread(Socket socket)
        {
            try{
                this.inStream = socket.getInputStream();
            }
            catch (IOException var4)
            {
                var4.printStackTrace();
            }
        }

        @Override
        public void run() {
            while(MainActivity.this.isReceive)
            {
                this.buffer = new byte[512];

                try{
                    this.inStream.read(this.buffer);
                }
                catch (IOException var3)
                {
                    var3.printStackTrace();
                }

                try{
                    this.str = new String(this.buffer,"UTF-8").trim();
                }
                catch (UnsupportedEncodingException var2)
                {
                    var2.printStackTrace();
                }

                if(str != null)
                {
                    String[] infoArray = str.split(":");
                    Message msg = new Message();
                    if(infoArray.length > 1 && infoArray[0] != null && infoArray[1] != null)
                    {
                        msg.what = Integer.parseInt(infoArray[0]);
                        msg.obj = infoArray[1];
                        MainActivity.this.myHandler.sendMessage(msg);
                    }
                }

            }
        }
    }

    //发送线程
    public class SendThread implements Runnable{

        private String Message;

        public SendThread(String msg)
        {
            this.Message = msg;
        }

        @Override
        public void run() {
            byte[] sendBuffer = null;

            try {
                sendBuffer = this.Message.getBytes("UTF-8");
            } catch (UnsupportedEncodingException var5) {
                var5.printStackTrace();
            }

            try {
                MainActivity.this.outStream = MainActivity.this.socket.getOutputStream();
            } catch (IOException var4) {
                var4.printStackTrace();
            }

            try {
                MainActivity.this.outStream.write(sendBuffer);
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }
    }

    //更新时间线程
    public class TimeThread extends Thread{
        @Override
        public void run() {
            do{
                try
                {
                    //每隔1s发送一次消息
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = MSG_TimeId;
                    myHandler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while(true);
        }
    }

    //更新视频进度线程
    public class VideoProgressThread extends Thread{
        @Override
        public void run() {
            while(MainActivity.this.isVideoProgress)
            {
                try {
                    Thread.sleep(1000);
                    if(isConnect)
                    {
                        new Thread(new SendThread(MSG_GetVideoProgress)).start();
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
