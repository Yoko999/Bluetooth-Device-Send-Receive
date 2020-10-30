package ru.tkvprok.yoequilibrium.testbluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnClickListener {
  private static final String TAG = "MY_MainActivity_Client";
  private static final int REQUEST_ENABLE_BT = 788;

  private BluetoothAdapter bluetoothAdapter;
  private MySTBroadcastReceiver mReceiver;

  private EditText et;
  private Button btn;

  private static final UUID MY_UUID_INSECURE =
      UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//("8ce255c0-200a-11e0-ac64-0800200c9a66");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    et=findViewById(R.id.et);
    btn=findViewById(R.id.btn);
    btn.setOnClickListener(this);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      // Device doesn't support Bluetooth
      Toast.makeText(this,"Блютуз не поддерживается!",Toast.LENGTH_SHORT).show();
    }

    mReceiver = new MySTBroadcastReceiver();
    IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    registerReceiver(mReceiver,intentFilter);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if(mReceiver!=null)
      unregisterReceiver(mReceiver);
  }

  public class MySTBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
          Log.d(TAG, "BOND CHANGED:" + intent.toString());
          int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.BOND_NONE);
          Log.d(TAG, "BOND STATE:" + bondState);

          if(bondState==BluetoothDevice.BOND_BONDED){
            Log.d(TAG, "Ресивер, девайс в статусе BONDED");
            ConnectThread btThr = new ConnectThread(device);
            btThr.start();
          }
      }
    }
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode){
      case REQUEST_ENABLE_BT:
        Log.d(TAG,"Вернулись из настрок блютуза! "+(data!=null ? data.toString():"data null"));
        break;
    }
  }

  private BluetoothDevice device;

  @Override
  public void onClick(View view) {
    switch (view.getId()){
      case R.id.btn:
        if(bluetoothAdapter!=null) {
          if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
          } else {
            String mac = et.getText().toString().toUpperCase();

            for (BluetoothDevice bd : bluetoothAdapter.getBondedDevices()) {
              Log.d(TAG, "Bonded with " + bd.getName() + " - " + bd.getAddress());
              mac = bd.getAddress();
            }
            Log.d(TAG, "Подключаемся к "+mac);

            if(BluetoothAdapter.checkBluetoothAddress(mac)) {

              if(device==null)
                device = bluetoothAdapter.getRemoteDevice(mac);

              if(device!=null){
                if(device.getBondState()!=BluetoothDevice.BOND_BONDED)
                  device.createBond();
                else{
                  ConnectThread btThr = new ConnectThread(device);
                  btThr.start();
                }
              }
            }else
              Toast.makeText(this,"Неправильный МАС-адрес!",Toast.LENGTH_SHORT).show();
          }
        }
        break;
    }
  }

  private class ConnectThread extends Thread {
    private BluetoothSocket mmSocket=null;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) {
      // Use a temporary object that is later assigned to mmSocket
      // because mmSocket is final.
      mmDevice = device;

      BluetoothSocket temp=null;
      try {
        // Get a BluetoothSocket to connect with the given BluetoothDevice.
        // MY_UUID is the app's UUID string, also used in the server code.
        temp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
      } catch (IOException e) {
        Log.e(TAG, "Socket's create() method failed", e);
      }
      mmSocket = temp;
    }

    public void run() {
      // Cancel discovery because it otherwise slows down the connection.
      bluetoothAdapter.cancelDiscovery();

      if(mmSocket!=null) {
        try {
          // Connect to the remote device through the socket. This call blocks
          // until it succeeds or throws an exception.
          if(!mmSocket.isConnected()) {
            Log.e(TAG, "Клиент не был подключён...");
            mmSocket.connect();
          }

          // The connection attempt succeeded. Perform work associated with
          // the connection in a separate thread.
          if(mmSocket.isConnected())
            manageMyConnectedSocket();
          else
            Log.e(TAG, "Не подключён к серверу....");

        } catch (IOException connectException) {
          // Unable to connect; close the socket and return.
          Log.e(TAG, "Could not CONNECT the client socket", connectException);
        } finally {
          /*try {
            mmSocket.close();
          } catch (IOException closeException) {
            Log.e(TAG, "Could not close the client socket", closeException);
          }*/
        }
      }
    }

    private void manageMyConnectedSocket(){
      OutputStream os = null;
      InputStream is = null;
      try {
        os = mmSocket.getOutputStream();

        //byte[] msg = new byte[]{22,43,12};
        byte[] msg = "HELLO,DUDES!11".getBytes();
        os.write(msg);
        //os.write(msg);

        /*Handler handler = new Handler(Looper.getMainLooper());
        // Share the sent message with the UI activity.
        Message writtenMsg = handler.obtainMessage(1, -1, -1, msg);
        writtenMsg.sendToTarget();*/


       // os.flush();
      }catch (IOException e) {
        Log.e(TAG, "Ошибка при записи в поток сокета:", e);
      }finally {
        /*try {
          if (os != null)
            os.close();
        }catch (IOException e){
          Log.e(TAG, "Could not close out-stream", e);
        }*/
      }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
      try {
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "Could not close the client socket", e);
      }
    }
  }

}
