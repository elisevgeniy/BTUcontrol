package ru.shlyahten.btucontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static long pressed_main;                                                               // Время нажатия гл. кнопки
    private static long pressed_func;                                                               // Время нажатия функц. кнопки

    private static int count_pressed_main = 0;                                                      // Начальное кол-во нажатий гл. кнопки: 1 шт
    private static int count_pressed_func = 0;                                                      // Начальное кол-во нажатий функц. кнопки: 1 шт
    private static boolean long_pressed_main = false;                                               // Флаг долгого нажатия гл. кнопки
    private static boolean long_pressed_func = false;                                               // Флаг долгого нажатия функц. кнопки

    private static int count_signals_main = 0;                                                      // Количесво принятых сигналов
    private static int count_signals_func = 0;                                                      // Количесво принятых сигналов

    private String BTConnectStatus;
    private TextView tvAction;
    private TextView tvBTStatus;

    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.mipmap.ic_launcher));
        setSupportActionBar(toolbar);

        tvAction = (TextView) findViewById(R.id.tv_action);
        tvBTStatus = (TextView) findViewById(R.id.tv_online);

        pressed_main = System.currentTimeMillis();

        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("00:0D:18:00:00:01");
        final Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();

        if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
            BTConnectStatus = "Bluetooth был выключен, включаю";
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        } else {
            BTConnectStatus = "Bluetooth выключен";
        }


        BroadcastReceiver afterBTon = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    BTConnectStatus = "Bluetooth включен";
                }
                if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    BTConnectStatus = "Bluetooth был выключен, включаю";
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent);
                }
            }
        };
        registerReceiver(afterBTon, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        Timer temp_bt = new Timer();
        temp_bt.schedule(new BTStatusTimer(), 0, 3000);

    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:                                                            // Нажата главная кнопка

                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && count_pressed_main <= 2 && count_signals_main < 5) {        // Кнопка НАЖАТА и количесво сделанных нажатий <=2 и кол-во полученных сигналов до 10 шт
                    Log.d("TEST", "PRESSED: " + (System.currentTimeMillis() - pressed_main));       // Чисто для отладки

                    if (mTimer != null) {                                                           // Очищаем таймер TODO: Убрать?
                        mTimer.cancel();
                        mTimer = null;
                    }

                    count_signals_main++;                                                           // Увеличивается количество полученных сигналов

                    if ((System.currentTimeMillis() - pressed_main) < 60) {                          // Между предыдущим НАЖАТИЕМ и нынешним меньше 0.06 c
                        long_pressed_main = true;                                                   // Устанавливаем флаг на длинное нажатие
                    }
                    if ((System.currentTimeMillis() - pressed_main) > 490 &&                        // Между предыдущим НАЖАТИЕМ и нынешним от 0.495 с до 0.505 c
                            (System.currentTimeMillis() - pressed_main) < 510) {
                        count_pressed_main--;                                                       // Уменьшаем количество нажатий на 1
                        long_pressed_main = true;                                                   // Устанавливаем флаг на длинное нажатие
                    }
                    if ((System.currentTimeMillis() - pressed_main) > 150 &&                        // Между предыдущим НАЖАТИЕМ и нынешним от 0.15 с до 0.5 c
                            (System.currentTimeMillis() - pressed_main) < 650) {
                        count_pressed_main++;                                                       // Увеличиваем количество нажатий на 1
                    }
                    if ((System.currentTimeMillis() - pressed_main) > 850) {                         // Между предыдущим НАЖАТИЕМ и нынешним больше 0.75 c
                        count_pressed_main = 1;                                                     // Увеличиваем количество нажатий на 1
                    }

                    pressed_main = System.currentTimeMillis();                                      // Устанавливаем время последнего нажатия

                    mTimer = new Timer();                                                           // Создаём таймер
                    mTimer.schedule(new MainTimerTask(), 700);
                }

                break;

            case KeyEvent.KEYCODE_ENTER:                                                        // Нажата функциональная кнопка

                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (count_pressed_func <= 2 && count_signals_func < 5) {                    // Кнопка НАЖАТА и количесво сделанных нажатий <=2 и кол-во полученных сигналов до 5 шт
                            Log.d("TEST", "PRESSED: " + (System.currentTimeMillis() - pressed_func));       // Чисто для отладки

                            if (mTimer != null) {                                                           // Очищаем таймер TODO: Убрать?
                                mTimer.cancel();
                                mTimer = null;
                            }

                            count_signals_func++;                                                           // Увеличивается количество полученных сигналов

                            if ((System.currentTimeMillis() - pressed_func) < 60) {                          // Между предыдущим НАЖАТИЕМ и нынешним меньше 0.06 c
                                long_pressed_func = true;                                                   // Устанавливаем флаг на длинное нажатие
                            }
                            if ((System.currentTimeMillis() - pressed_func) > 480 &&                        // Между предыдущим НАЖАТИЕМ и нынешним от 0.495 с до 0.505 c
                                    (System.currentTimeMillis() - pressed_func) < 520) {
                                count_pressed_func--;                                                       // Уменьшаем количество нажатий на 1
                                long_pressed_func = true;                                                   // Устанавливаем флаг на длинное нажатие
                            }
                            if ((System.currentTimeMillis() - pressed_func) > 150 &&                        // Между предыдущим НАЖАТИЕМ и нынешним от 0.15 с до 0.5 c
                                    (System.currentTimeMillis() - pressed_func) < 650) {
                                count_pressed_func++;                                                       // Увеличиваем количество нажатий на 1
                            }
                            if ((System.currentTimeMillis() - pressed_func) > 850) {                         // Между предыдущим НАЖАТИЕМ и нынешним больше 0.75 c
                                count_pressed_func = 1;                                                     // Увеличиваем количество нажатий на 1
                            }

                            pressed_func = System.currentTimeMillis();                                      // Устанавливаем время последнего нажатия

                            mTimer = new Timer();                                                           // Создаём таймер
                            mTimer.schedule(new FuncTimerTask(), 700);
                        }
                        break;
                }

                break;

            default:
                break;
        }

        return true;
    }

    class MainTimerTask extends TimerTask {                                                         // Класс таймера для главной кнопки

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);         // Для получения данных о плеере, наверное

        @Override
        public void run() {

            Log.d("TEST_timer", "Task_started, count_pressed_main: " + count_pressed_main + ", isLong: " + long_pressed_main);

            if (long_pressed_main && count_pressed_main == 0) {                                      // Долгое нажатие + 0 коротких
                Log.d("TEST_timer_main", "Raise volume");
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        tvAction.setText("Увеличение громкости");                                   // Отображение действия на экране
                    }
                });

                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);      // Увеличитвается звук
            }

            switch (count_pressed_main) {
                case 1:                                                                             // Кнопка была нажата 1 раз
                    Log.d("TEST_timer_main", "Action: play/pause");

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            tvAction.setText("Воспроизведение/пуза");                               // Отображение действия на экране
                        }
                    });

                    int KeyCode;                                                                    // Код кнопки зависит от

                    if (audioManager.isMusicActive()) {                                             // того, запущен ли плеер TODO: точно ли не музыка проигрывается?
                        KeyCode = KeyEvent.KEYCODE_MEDIA_PAUSE;                                // Воспроизвести/Пауза
                    } else {
                        KeyCode = KeyEvent.KEYCODE_MEDIA_PLAY;                                            // Принять вызов
                    }

                    KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyCode);                // Имитируется НАЖАТИЕ на кнопку с кодом KeyCode
                    Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                    MainActivity.this.sendOrderedBroadcast(intent, null);

                    keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyCode);                           // Имитируется ОТЖАТИЕ на кнопку с кодом KeyCode
                    intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                    MainActivity.this.sendOrderedBroadcast(intent, null);

                    break;

                case 2:
                    Log.d("TEST_timer_main", "Action: next track");
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            tvAction.setText("Следующий трек");                                             // Отображение действия на экране
                        }
                    });

                    KeyEvent keyEvent2 = new KeyEvent(KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_MEDIA_NEXT);   // Имитируется НАЖАТИЕ на кнопку с кодом KEYCODE_MEDIA_NEXT
                    Intent intent2 = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    intent2.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent2);
                    MainActivity.this.sendOrderedBroadcast(intent2, null);

                    keyEvent2 = new KeyEvent(KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_MEDIA_NEXT);       // Имитируется ОТЖАТИЕ на кнопку с кодом KEYCODE_MEDIA_NEXT
                    intent2 = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    intent2.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent2);
                    MainActivity.this.sendOrderedBroadcast(intent2, null);


                    break;
            }

            count_signals_main = 0;                                                                 // Устанавливется начальное количество полученных сигналов
            count_pressed_main = 0;                                                                 // Устанавливется начальное кол-во нажатий: 0 шт
            long_pressed_main = false;                                                              // Устанавливется начальный флаг нажатий: false

            Timer temp1 = new Timer();
            temp1.schedule(new ActionTimer(), 500);
        }
    }

    class FuncTimerTask extends TimerTask {

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);         // Для получения данных о плеере, наверное

        @Override
        public void run() {

            Log.d("TEST_timer_func", "Task_started, count_pressed_func: " + count_pressed_func + ", isLong: " + long_pressed_func);

            if (long_pressed_func && count_pressed_func == 0) {                                      // Долгое нажатие + 0 коротких
                Log.d("TEST_timer_func", "Lower volume");
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        tvAction.setText("Уменьшение громкости");
                    }
                });
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);      // Уменьшается звук

            }

            switch (count_pressed_func) {
                case 2:                                                                             // Кнопка зажата 1 раз
                    Log.d("TEST_timer_main", "Action: previous track");
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            tvAction.setText("Предыдущий трек");
                        }
                    });

                    KeyEvent keyEvent3 = new KeyEvent(KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS);   // Имитируется НАЖАТИЕ на кнопку с кодом KEYCODE_MEDIA_PREVIOUS
                    Intent intent3 = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    intent3.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent3);
                    MainActivity.this.sendOrderedBroadcast(intent3, null);

                    keyEvent3 = new KeyEvent(KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS);       // Имитируется ОТЖАТИЕ на кнопку с кодом KEYCODE_MEDIA_PREVIOUS
                    intent3 = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    intent3.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent3);
                    MainActivity.this.sendOrderedBroadcast(intent3, null);

                    break;
            }

            count_signals_func = 0;                                                                 // Устанавливется начальное количество полученных сигналов
            count_pressed_func = 0;                                                                 // Устанавливется начальное кол-во нажатий: 0 шт
            long_pressed_func = false;                                                              // Устанавливется начальный флаг нажатий: false

            Timer temp2 = new Timer();
            temp2.schedule(new ActionTimer(), 2000);
        }
    }

    class ActionTimer extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    tvAction.setText("Ожидание");
                }
            });
        }
    }

    class BTStatusTimer extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    tvBTStatus.setText(BTConnectStatus);
                }
            });
        }
    }

}
