package ru.tkvprok.yoequilibrium.testbluetooth;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by yo on 27.12.2019
 * (с) Рождественская В., 2019
 * ООО "Технологии и Сервис" (ТД Впрок)
 */
public class TryBoundService extends Service {

  private static final String TAG = "MY_TEST_BoundService";
  private BluetoothServerSocket mServerSocket=null;
  private BluetoothSocket clientSocket = null;

  private Thread mConnectionsListenerThread;
  private List<ConnectedThread> devicesThreads=new ArrayList<>();

  private Builder mNotifyBuilder;
  private NotificationManager mNotificationManager;

  private static final UUID MY_UUID_INSECURE =
      UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//("8ce255c0-200a-11e0-ac64-0800200c9a66");

  public TryBoundService(){
    super();
  }

  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    Log.d(TAG, "Сервис onStartCommand!");
    mConnectionsListenerThread = new Thread(new Runnable() {
      @Override
      public void run() {

        Log.d(TAG, "Сервис Запущен!");
        try {
          BluetoothServerSocket temp = null;

          try {
            // MY_UUID is the app's UUID string, also used by the client code.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
              Intent msgIntent = new Intent();
              msgIntent.setAction(MainActivity.BT_ACTION);
              msgIntent.putExtra(MainActivity.BT_EXTRA_MSG, "Блютуз не поддерживается!");
              sendBroadcast(msgIntent);
              Log.e(TAG, "Блютуз не поддерживается!");
              return;
            }

            // Device doesn't support Bluetooth
            if (!bluetoothAdapter.isEnabled()) {
              Intent msgIntent = new Intent();
              msgIntent.setAction(MainActivity.BT_NEED_ENABLE_ACTION);
              sendBroadcast(msgIntent);
              Log.e(TAG, "Блютуз не включён");
              return;
            }

            //------------------------
            mNotifyBuilder = new Notification.Builder(getApplicationContext())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Сервис Блютуз типа работает");

            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (VERSION.SDK_INT >= 26) {
              String channelID = "Test BlueTooth";
              NotificationChannel mChannel = new NotificationChannel(channelID, "TestBT", NotificationManager.IMPORTANCE_DEFAULT);
              mChannel.setDescription("Test Bluetooth");
              if (mNotificationManager != null)
                mNotificationManager.createNotificationChannel(mChannel);
              mNotifyBuilder.setChannelId(channelID);
            }
            startForeground(999, mNotifyBuilder.build());
            //------------------------

            temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(getApplicationInfo().packageName, MY_UUID_INSECURE);
            Log.d(TAG, "Server clientSocket: " + temp.toString());

          } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed", e);
            Intent msgIntent = new Intent();
            msgIntent.setAction(MainActivity.BT_ACTION);
            msgIntent.putExtra(MainActivity.BT_EXTRA_MSG, "Ошибка при инициализации!\n" + e.getMessage());
            sendBroadcast(msgIntent);
            return;
          }

          mServerSocket = temp;

          // try {
          while (true) {
            BluetoothSocket tempClient = null;
            // Keep listening until exception occurs or a clientSocket is returned.
            try {
              Log.d(TAG, "Listening....");

              tempClient = mServerSocket.accept();
              if (tempClient != null && tempClient.isConnected()) {
                Log.d(TAG, "Connected device:" + tempClient.getRemoteDevice().getName() + " from " + tempClient.getRemoteDevice().getAddress());

                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                // manageMyConnectedSocket(clientSocket);

                clientSocket = tempClient;
                ConnectedThread thread = new ConnectedThread(clientSocket);
                devicesThreads.add(thread);
                thread.start();
              }
            } catch (IOException e) {
              Log.e(TAG, "Socket's accept() method failed", e);

              Intent msgIntent = new Intent();
              msgIntent.setAction(MainActivity.BT_ACTION);
              msgIntent.putExtra(MainActivity.BT_EXTRA_MSG, "Ошибка при подключении устройства!\n" + e.getMessage());
              sendBroadcast(msgIntent);

            }

          }
        /*}finally {
          try {
            if(mServerSocket!=null)
              mServerSocket.close();
          } catch (IOException e) {
            Log.e(TAG, "Socket's close() method failed", e);
          }
        }*/

        } finally {
          Log.e(TAG, "Кажется, сервису конец!");
          stopForeground(true);
        }
      }
    });
    mConnectionsListenerThread.start();

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.e(TAG, "Сервис завершился!");
    stopForeground(true);
    try{
      if(mConnectionsListenerThread!=null)
        mConnectionsListenerThread.interrupt();
    } catch (Exception e) {
      Log.e(TAG, "onDestroy Main Listener Thread interruption error: " + e.getMessage());
    }
    for (ConnectedThread thread:devicesThreads) {
      if(thread!=null) {
        try {
          thread.interrupt();
        } catch (Exception e) {
          Log.e(TAG, "onDestroy Thread interruption error: " + e.getMessage());
        }
      }
    }
  }

  private MyBinder mMyBinder = new MyBinder();

  public class MyBinder extends Binder {
    public TryBoundService getService() {
      return TryBoundService.this;
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    if(mMyBinder==null)
      mMyBinder = new MyBinder();
    return mMyBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    try{
      if(mConnectionsListenerThread!=null)
        mConnectionsListenerThread.interrupt();
    } catch (Exception e) {
      Log.e(TAG, "onDestroy Main Listener Thread interruption error: " + e.getMessage());
    }
    for (ConnectedThread thread:devicesThreads) {
      if(thread!=null) {
        try {
          thread.interrupt();
        } catch (Exception e) {
          Log.e(TAG, "onDestroy Thread interruption error: " + e.getMessage());
        }
      }
    }
    return super.onUnbind(intent);
  }

  private class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    int hashCodeRemoteDevice;

    public ConnectedThread(BluetoothSocket socket) {
      mmSocket = socket;
      hashCodeRemoteDevice = mmSocket.getRemoteDevice().hashCode();
      InputStream tmpIn = null;
      OutputStream tmpOut = null;

      // Get the input and output streams; using temp objects because
      // member streams are final.
      try {
        tmpIn = socket.getInputStream();
      } catch (IOException e) {
        Log.e(TAG, "Error occurred when creating input stream", e);
      }
      try {
        tmpOut = socket.getOutputStream();
      } catch (IOException e) {
        Log.e(TAG, "Error occurred when creating output stream", e);
      }

      mmInStream = tmpIn;
      mmOutStream = tmpOut;

      mNotifyBuilder
          .setContentTitle("Блютуз")
          .setContentText("Connected Device: "+mmSocket.getRemoteDevice().getName());
      mNotificationManager.notify(hashCodeRemoteDevice,mNotifyBuilder.build());

    }

    public void run() {
      int numBytes; // bytes returned from read()
      //StringBuilder readBuf = new StringBuilder(1024);

      // Keep listening to the InputStream until an exception occurs.
      mmBuffer = new byte[128];

      try {
        while (mmSocket.isConnected() && (numBytes = mmInStream.read(mmBuffer))>0) {

          Log.d(TAG, "Received bytes: " + numBytes);
          Log.d(TAG, "Received data: ");
          for (byte b : mmBuffer)
            Log.d(TAG, "Byte " + b);
          //}

          // Send the obtained bytes to the UI activity.
          if (mmBuffer.length > 0) {
            Intent msgIntent = new Intent();
            msgIntent.setAction(MainActivity.BT_ACTION);
            msgIntent.putExtra(MainActivity.BT_EXTRA_MSG,
                new String(mmBuffer, ((Build.VERSION.SDK_INT >= 19) ? StandardCharsets.UTF_8 : Charset.forName("UTF-8"))));
            sendBroadcast(msgIntent);
          }

          mmBuffer = new byte[128];
        }
        //    }
      } catch (IOException e) {
        Log.e(TAG, "Input stream was disconnected", e);

        Intent msgIntent = new Intent();
        msgIntent.setAction(MainActivity.BT_ACTION);
        msgIntent.putExtra(MainActivity.BT_EXTRA_MSG, mmSocket.getRemoteDevice().getName()+ " отрубился");
        sendBroadcast(msgIntent);
        // break;
      }finally {
        // cancel();
        Log.d(TAG, "Finally должны убрать "+hashCodeRemoteDevice);
        mNotificationManager.cancel(hashCodeRemoteDevice);
      }
    }
  }
}
