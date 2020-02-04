package com.example.bluetoothpiano;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button gunban[] = new Button[8];
    Integer gunbanID[]={R.id.btnDo1,R.id.btnRe, R.id.btnMe,R.id.btnFa,R.id.btnSol,R.id.btnLa,R.id.btnSi,R.id.btnDo2};
    BluetoothAdapter bluetoothAdapter;
    static final int REQUEST_ENABLE_BT=10;
    int mPairedDeviceCount=0;
    Set<BluetoothDevice> mDevices;
    BluetoothDevice mRemoteDevice;//  BluetoothDevice 로 기기의 장치정보를 알아낼 수 있는 자세한 메소드 및 상태값을 알아낼 수 있다.
    // 연결하고자 하는 다른 블루투스 기기의 이름, 주소, 연결 상태 등의 정보를 조회할 수 있는 클래스.
    //현재 기기가 아닌 다른 블루투스 기기와의 연결 및 정보를 알아낼 때 사용.
    BluetoothSocket mSocket=null;// 통신하려면 소켓이 필요함
    OutputStream moutputStream= null;
    InputStream minputStream= null;
    Thread mWorkerThread=null; //여러개 장치를 동시에 처리하기위해
    String mStrDelimiter="\n";
    char mCharDelimiter='\n';
    byte readBuffer[];
    int readBufferPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        for (int i = 0 ; i<gunbanID.length; i++) {
            gunban[i]=(Button)findViewById(gunbanID[i]);
        }
        for(int i=0; i<gunbanID.length; i++){
            final int index;
            index=i;
            gunban[index].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendData(Integer.toString(index+1));
                }
            });
        }
        checkBluetooth();

    }
    void checkBluetooth(){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();//겟 디폴트 어댑터 어댑터를 반환
        if(bluetoothAdapter==null){
            showToast("블루투스를 지원하지 않습니다.");

        }else {
            if(!bluetoothAdapter.isEnabled()){
                Intent enableBTIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE) ;
                startActivityForResult(enableBTIntent,REQUEST_ENABLE_BT);
            }else{
                selectDevice();
            }
        }
    }
    void selectDevice() {
        mDevices=bluetoothAdapter.getBondedDevices();
        mPairedDeviceCount=mDevices.size();
        if(mPairedDeviceCount==0){
            showToast("연결할 블루투스 장치가 하나도 없습니다.");

        }else {
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("블루투스 장치 선택");

            List<String> listItems=new ArrayList<String>();
            for(BluetoothDevice device:mDevices){
                listItems.add(device.getName());
            }
            listItems.add("취소");
            final CharSequence items[]=listItems.toArray(new CharSequence[listItems.size()]);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==mPairedDeviceCount){
                        showToast("취소를 선택했습니다.");

                    }else{
                        connectToSelectDevice(items[which].toString());
                    }

                }
            });
            builder.setCancelable(false);
            builder.show();
        }
    }
    //선택된 장치 연결
    void connectToSelectDevice(String selectedDeviceName){
        mRemoteDevice=getDeviceFromBondedList(selectedDeviceName);
        UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//유니버셜 유티크 아이덴 티티 /소켓을 생성할때 생성하는 기능
        try{
            mSocket=mRemoteDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            moutputStream=mSocket.getOutputStream();
            minputStream=mSocket.getInputStream();
            beginListenForData();
          //  ivConnect.setImageResource(R.drawable.bluetooth_icon);
        }catch (Exception e) {
            showToast("현재 장치와 연결이 되지 않습니다.");
          //  ivConnect.setImageResource(R.drawable.bluetooth_grayicon);
        }
    }
    //페어링된 블루투스 장치를 이름으로 찾기
    BluetoothDevice getDeviceFromBondedList(String name){
        BluetoothDevice selectedDevice=null;
        for (BluetoothDevice device:mDevices){
            if(name.equals(device.getName())){
                selectedDevice=device;
                break;

            }
        }
        return selectedDevice;
    }
    //데이터 수신 준비 및 처리
    void beginListenForData() {
        final Handler handler=new Handler();
        readBuffer=new byte[1024];
        readBufferPosition=0;
        mWorkerThread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (Thread.currentThread().isInterrupted()){
                    try{
                        int bytesAvailable=minputStream.available();
                        if (bytesAvailable>0) {
                            byte paketBytes[] = new byte[bytesAvailable];
                            minputStream.read(paketBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = paketBytes[i];
                                if (b == mCharDelimiter) {
                                    byte encodeBytes[] = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodeBytes, 0, encodeBytes.length);
                                    final String data = new String(encodeBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //수신된 문자열 데이터에 대한 처리작업
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }catch (IOException e) {
                        showToast("데이터 수신 중 오류가 발생하였습니다.");
                    }
                }
            }
        });
        mWorkerThread.start();
    }
    //데이터 송신
    void sendData(String msg){
        msg+=mStrDelimiter;
        try {
            moutputStream.write(msg.getBytes());
        }
        catch (Exception e){
            showToast("데이터 전송 중 오류가 발생했습니다.");

        }

    }

    @Override
    protected void onDestroy() {
        try{
            mWorkerThread.interrupt(); //데이터 수신 쓰레드 종료
            minputStream.close();
            moutputStream.close();
            mSocket.close();
        }catch (Exception e){

        }
        super.onDestroy();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode==RESULT_OK){
                    selectDevice();
                }else if(resultCode==RESULT_CANCELED){
                    showToast("블루투스 연결을 취소");
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void showToast(String msg) {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }
}
