package ru.tkvprok.yoequilibrium.testbluetooth;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MY_MainActivity";
  private static final int REQUEST_ENABLE_BT = 789;

  private MySTBroadcastReceiver mReceiver;
  public static final String BT_ACTION = MainActivity.class.getName()+"_ACTION_BT";
  public static final String BT_NEED_ENABLE_ACTION = MainActivity.class.getName()+"_ACTION_BT_NEED_ENABLE";

  public static final String BT_EXTRA_MSG = MainActivity.class.getName()+"_EXTRA_MSG_BT";

  private Intent btServiceIntent;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
    startActivity(discoverableIntent);

    mReceiver = new MySTBroadcastReceiver();
    IntentFilter intentFilter = new IntentFilter(BT_ACTION);
    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    registerReceiver(mReceiver,intentFilter);

    btServiceIntent=new Intent(this,BTService.class);
    startService(btServiceIntent);

   /* btServiceIntent = new Intent(this, TryBoundService.class);
    startService(btServiceIntent);
    bindService(btServiceIntent,m_serviceConnection,BIND_AUTO_CREATE);*/
  }

  private TryBoundService m_service;
  private ServiceConnection m_serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      m_service = ((TryBoundService.MyBinder)service).getService();
      Log.d(TAG,"Service connected! "+className);
    }

    public void onServiceDisconnected(ComponentName className) {
      m_service = null;
      Log.e(TAG,"Service DISconnected! "+className);
    }
  };

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(mReceiver);
  //  unbindService(m_serviceConnection);
    stopService(btServiceIntent);
  }

  public class MySTBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
      if(BT_ACTION.equals(intent.getAction())){
          if(intent.hasExtra(BT_EXTRA_MSG)){
            String msg = intent.getStringExtra(BT_EXTRA_MSG);
              Log.d(TAG,"Received msg:"+msg);
            Toast.makeText(MainActivity.this,"Месдж: "+msg,Toast.LENGTH_LONG).show();
          }
      }else if(BT_NEED_ENABLE_ACTION.equals(intent.getAction())){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

      }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        Log.d(TAG,"Received BT state: "+state);
        switch(state) {
          case BluetoothAdapter.STATE_OFF:
             // unbindService(m_serviceConnection);
              stopService(btServiceIntent);
            break;
          case BluetoothAdapter.STATE_ON:
            if(!isMyServiceRunning(BTService.class))
              startService(btServiceIntent);
       //     bindService(btServiceIntent,m_serviceConnection,BIND_AUTO_CREATE);
            break;
        }
      }
    }
  }

  //Проверка, запущен ли сервис (http://www.cyberforum.ru/android-dev/thread1313536.html)
  private boolean isMyServiceRunning(Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    if(manager!=null) {
      for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.getName().equals(service.service.getClassName()))
          return true;
      }
    }
    return false;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    Log.d(TAG,"onActivityResult from "+requestCode);
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode){
      case REQUEST_ENABLE_BT:
        Log.d(TAG,"Вернулись из настрок блютуза! "+(data!=null ? data.toString():"data null"));
        startService(btServiceIntent);
        break;
    }
  }
}
