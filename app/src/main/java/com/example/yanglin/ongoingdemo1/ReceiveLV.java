
package com.example.yanglin.ongoingdemo1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yanglin.ongoingdemo1.Generic.GenericGF;
import com.example.yanglin.ongoingdemo1.Generic.GenericGFPoly;
import com.example.yanglin.ongoingdemo1.Generic.ReedSolomonDecoder;
import com.example.yanglin.ongoingdemo1.Generic.SensorDataProcess;
import com.example.yanglin.ongoingdemo1.Utils.MD5;
import com.example.yanglin.ongoingdemo1.Utils.Utils2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ReceiveLV extends AppCompatActivity  {

    private static final String TAG = "sensor";
    private ReceiveHandler receiveHandler = new ReceiveHandler();

    private int pite=55555;
    File sdcard= Environment.getExternalStorageDirectory();
    DatagramSocket ds=null;

    DatagramPacket dp=null,messagepkg=null;

    FileOutputStream fos=null;

    File f1=new File(sdcard,"onefile.txt");
    File f2=new File(sdcard,"twofile.txt");

    Button gpsData,gpsCmp ;
    Button start ,AccSyd,AccKey,GyrSyd,GyrKey;
    EditText ip;
    TextView txt;

    SensorManager sensorManager;
    private android.hardware.Sensor accelerometer; // 加速度传感器
    private android.hardware.Sensor magnetic; // 地磁场传感器
    private android.hardware.Sensor gyroscope;//线性加速度传感器

    boolean tag_acc;   //标志位 tag_acc标志产生了加速度
    boolean tag_g;  // tag_g标志产生了新的磁场数据
    boolean tag_Gry; // tag_lineAcc标志产生了新的线性加速度数据

    StringBuilder sensorBuilderAcc = new StringBuilder("");
    StringBuilder sensorBuilderGry = new StringBuilder("");
    int count=0;

    private float[] accValues = new float[3];//用于之后计算方向
    private float[] magFieldValues = new float[3];
    private float[] gryFieldValues = new float[3];

    private float[] rotationMatrix = new float[9];  //存放旋转矩阵


    class ReceiveHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            if(msg.what==1) {
                txt.setText("收到文件");
                Toast.makeText(ReceiveLV.this, "我是LV，我接收到文件", Toast.LENGTH_SHORT).show();
            }
            else
                if(msg.what==2){
                    txt.setText("发送反馈");
                    Toast.makeText(ReceiveLV.this, "我是LV，发送反馈", Toast.LENGTH_SHORT).show();
                }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate ( savedInstanceState );
        setContentView ( R.layout.activity_udp);

        gpsData = (Button)findViewById(R.id.gpsData);
         gpsData.setText("接受文件");  //接收端显示   接受文件
        gpsCmp = (Button)findViewById(R.id.gpsCmp);

        start=(Button)findViewById(R.id.start);

        AccSyd=(Button)findViewById(R.id.AccSyd);
        AccKey=(Button)findViewById(R.id.AccKey);

        GyrSyd=(Button)findViewById(R.id.GyrSyd);
        GyrKey=(Button)findViewById(R.id.GyrKey);

        ip=(EditText)findViewById(R.id.ip);
        ip.setVisibility(View.GONE);
        AccSyd.setText("接受Acc校正子");
        AccKey.setText("接受Acc加密数据");

        GyrSyd.setText("接受Gyr校正子");
        GyrKey.setText("接受Gyr加密数据");

        txt = (TextView)findViewById(R.id.txt);//用来显示消息

        gpsData.setOnClickListener ( new View.OnClickListener ( )
        {@Override
            public void onClick(View v)
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        receive(f2);
                    }
                }).start();
            }
        } );



        gpsCmp.setOnClickListener(new View.OnClickListener() {   //比较GPS数据相似性
            @Override
            public void onClick(View v) {
               new Thread(new Runnable() {
                   @Override
                   public void run() {
                       CompareFile(f1,f2);
                   }
               }).start();

            }
        });


        //开始记录传感器数据  并进行 0,1 处理
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Toast.makeText(ReceiveLV.this,"开始记录传感器数据",Toast.LENGTH_LONG).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        begin();
                    }
                }).start();
            }
        });

        //接受  校正子
        AccSyd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String str= receiveData(8888);
                        //将接收到的校正子 存储
                        Utils2.setSyndromes(str);

                        Log.d("输出","接收到的校正子"+ Utils2.getSyndromes());
                    }
                }).start();


            }
        });
        //接受加密后的数据
        AccKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String str = receiveData(6666);


                        Log.d("输出","接收到的加密数据"+ str);
                        try {
                            String newKey=  infoReconciliation(Utils2.getSyndromes(),Utils2.getRawKey());
                            Log.d("输出newKey","newKey的数值是"+newKey);
                            MD5 md = new MD5();
                            String  MD5LV = md.print(newKey);

                            if(MD5LV.equals(str)){
                                Log.d("输出比较结果：","两组数据是一致的");
                            }
                            else{
                                Log.d("输出比较结果：","两组数据是bu一致的");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                }).start();

            }
        });

/*
  陀螺仪 对应的校正子  加密数据
 */
        //校正子
        GyrSyd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        String str=  receiveData(5555);
                        //将接收到的校正子 存储
                        Utils2.setSyndromesGyr(str);
                        Log.d("输出","接收到的校正子"+ Utils2.getSyndromesGyr());


                    }
                }).start();


            }
        });

        //加密数据
        GyrKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        String str =   receiveData(3333);

                        Log.d("输出","接收到的加密数据"+ str);
                        try {
                            String newKey=  infoReconciliation(Utils2.getSyndromesGyr(),Utils2.getRawKeyGyr());
                            Log.d("输出newKey","newKey的数值是"+newKey);
                            MD5 md = new MD5();
                            String  MD5LV = md.print(newKey);
                            if(MD5LV.equals(str)){
                                Log.d("输出比较结果：","两组数据是一致的");
                            }
                            else{
                                Log.d("输出比较结果：","两组数据是bu一致的");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                }).start();

            }
        });





    }


    public void receive(File f2){

        try {
              ds= new DatagramSocket(pite);

            byte[] by = new byte[1024];
            fos = new FileOutputStream(f2);
             dp = new DatagramPacket(by,by.length);
            System.out.println("等待接受数据");

            while(true) {
                ds.receive(dp);
                if(new String(dp.getData(),0,dp.getLength()).equals("end")) {
                    receiveHandler.sendEmptyMessage(1); //通知本机接受完毕
                    break;
                }
                fos.write(dp.getData(), 0, dp.getLength());
                fos.flush();
                String str= new String(dp.getData(),0,dp.getLength());
                System.out.println("接收到的数据是：");
                System.out.println(str+"--->"+dp.getAddress().getHostAddress()
                        +":"+dp.getPort());
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try{
                fos.close();
                ds.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }


    public void CompareFile(File f1,File f2){

    byte[] messagebuf = new byte[1024];  //反馈信息
   //使用FrechetDistane方法进行比较
    DiscreteFrechetDistance dt = new DiscreteFrechetDistance();
    String s1 = dt.createStr(f1);
    String s2 = dt.createStr(f2);

    String d = dt.test(s1,s2);
    System.out.println("*******CompareFile中的d******"+d);

if(d.equals("00.0000")){
    messagebuf = "ok".getBytes();
}else {
    messagebuf="disable".getBytes();
}
    try {
        ds= new DatagramSocket(pite);
        messagepkg = new DatagramPacket(messagebuf, messagebuf.length, InetAddress.getByName(dp.getAddress().getHostAddress()), 8090);
        System.out.println("------打印信息-------" + new String(messagepkg.getData()));
        ds.send(messagepkg);
    }catch (IOException e){
        e.printStackTrace();
    }
    receiveHandler.sendEmptyMessage(2);
}


    public String  receiveData(int port) {
        String str="";
        try {
            DatagramSocket ds = new DatagramSocket(port);
            byte[] bytes = new byte[12800];
            DatagramPacket dp = new DatagramPacket(bytes,bytes.length);
            ds.receive(dp);
            str= new String(dp.getData(),0,dp.getLength());
            return str;

        }catch (Exception e){
            e.printStackTrace();
        }
        return str;
    }

    //开始记录传感器数据
    public void begin(){
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //初始化加速度传感器
        accelerometer=sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        // 初始化地磁场传感器
        magnetic = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD);
        //初始化线性加速度传感器
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        //初始化标志位
        tag_acc = false;
        tag_g = false;
        tag_Gry = false;
        //注册加速度传感器
        sensorManager.registerListener(sensorListener,accelerometer,10000);
        //注册磁场传感器
        sensorManager.registerListener(sensorListener,magnetic,10000);
        //注册磁场传感器
        sensorManager.registerListener(sensorListener,gyroscope,10000);

    }
    final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            if(tag_acc&&tag_g&&tag_Gry){
                calculateRotationMatrix();
                double f[] ={gryFieldValues[0],gryFieldValues[1],gryFieldValues[2]};
                double fUpdate[] =  UpdateRealDate(f);
                sensorBuilderGry.append(fUpdate[2]).append("\r\n");
                Log.d("陀螺仪", "数据开始记录 event.values[2]"+event.values[2]);
                Log.d("陀螺仪", "数据开始记录  fUpdate[2]  "+fUpdate[2]);

                sensorBuilderAcc.append(accValues[2]).append("\r\n");
                Log.d("加速度", "数据开始记录"+event.values[2]);

                count++;
                if(count==12800){
                  Toast.makeText(ReceiveLV.this,"已采样12800次",Toast.LENGTH_LONG).show();
                    sensorManager.unregisterListener(sensorListener);
                    tag_Gry = false;
                    tag_acc = false;
                    tag_g = false;

                    String sensorSendDataBuilder =sensorBuilderAcc.toString();
                    SensorDataProcess s = new SensorDataProcess();
                    float[] signal = s.load(sensorSendDataBuilder);  //将文件中的数据放到数组[12800]
                    String rawKeySend = s.startKeyGen(signal);
                    Utils2.setRawKey(rawKeySend); //将原始数据（0,1处理后）进行保存  原始数据：计算校正子，加密后发送



                    String sensorSendDataBuilderGry =sensorBuilderGry.toString();
                    float[] signalGyr = s.load(sensorSendDataBuilderGry);  //将文件中的数据放到数组[12800]
                    String rawKeySendGyr = s.startKeyGen(signalGyr,0); //重载函数
                    Utils2.setRawKeyGyr(rawKeySendGyr); //将原始数据（0,1处理后）进行保存  原始数据：计算校正子，加密后发送



                }
                tag_Gry = false;
                tag_acc = false;
                tag_g = false;
            }




            if(event.sensor.getType()==android.hardware.Sensor.TYPE_ACCELEROMETER){

                accValues=event.values; //计算选择矩阵用得着
                tag_acc = true;
            }
            if(event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE)//陀螺仪
            {
                Log.i(TAG, "陀螺仪传感器 的值改变");
                gryFieldValues =event.values;
                tag_Gry = true;
            }
            if(event.sensor.getType() == android.hardware.Sensor.TYPE_MAGNETIC_FIELD) //磁场
            {
                Log.i(TAG, "磁场传感器 的值改变");
                magFieldValues = event.values;  //计算旋转矩阵用得着
                tag_g = true;
            }

        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private double[] UpdateRealDate(double [] f) {

        f[0] = rotationMatrix[0]*f[0]+rotationMatrix[1]*f[1]+rotationMatrix[2]*f[2];
        f[1] = rotationMatrix[3]*f[0]+rotationMatrix[4]*f[1]+rotationMatrix[5]*f[2];
        f[2] = rotationMatrix[6]*f[0]+rotationMatrix[7]*f[1]+rotationMatrix[8]*f[2];
        return f;
    }

    //计算旋转矩阵
    private void calculateRotationMatrix() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accValues,
                magFieldValues);
    }
    //开始信息协调
    public String infoReconciliation(String syn,String key) throws Exception{
        StringBuilder newKey = new StringBuilder();

        int[] syndromeInts = Utils2.string2Ints(  syn);  //

        int len = syndromeInts.length/3;

        int[] aliceSyndromeInt1 = new int[len];
        int[] aliceSyndromeInt2 = new int[len];
        int[] aliceSyndromeInt3 = new int[len];

        for(int i=0;i<len;i++){
            aliceSyndromeInt1[i] = syndromeInts[i];
        }
        for(int i=0;i<len;i++){
            aliceSyndromeInt2[i] = syndromeInts[len+i];
        }
        for(int i=0;i<len;i++){
            aliceSyndromeInt3[i] = syndromeInts[len+len+i];
        }
        GenericGFPoly[] aliceSyndromes = new GenericGFPoly[3];
        aliceSyndromes[0] = new GenericGFPoly(GenericGF.AZTEC_PARAM,aliceSyndromeInt1);
        aliceSyndromes[1] = new GenericGFPoly(GenericGF.AZTEC_PARAM,aliceSyndromeInt2);
        aliceSyndromes[2] = new GenericGFPoly(GenericGF.AZTEC_PARAM,aliceSyndromeInt3);

        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_PARAM);
        //将lv的rawkey 分为3组 int数组
        int[] slaveKey = new int[45];
        for(int i=0;i<32;i++){
            String tmpStr= key.substring(i*4,(i+1)*4);
            slaveKey[i]=Integer.parseInt(tmpStr,2);
        }
        for(int i=32;i<45;i++)
        {
            slaveKey[i] = 0;
        }
        int[] rawKey1 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey1[i] = slaveKey[i];
        }
        int[] rawKey2 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey2[i] = slaveKey[15+i];
        }
        int[] rawKey3 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey3[i] = slaveKey[30+i];
        }

        String result1 = Utils2.int2String(decoder.myDecode(aliceSyndromes[0], rawKey1, 12));
        String result2 = Utils2.int2String(decoder.myDecode(aliceSyndromes[1], rawKey2, 12));
        String result3 = Utils2.int2String(decoder.myDecode(aliceSyndromes[2], rawKey3, 12));

        newKey.append(result1);
        newKey.append(result2);
        newKey.append(result3);

        return newKey.substring(0,128);
    }








}
