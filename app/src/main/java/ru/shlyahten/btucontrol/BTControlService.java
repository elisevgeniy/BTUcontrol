package ru.shlyahten.btucontrol;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.net.rtp.AudioStream;
import android.os.IBinder;
import android.provider.MediaStore;

import java.net.InetAddress;

public class BTControlService extends Service {
    public BTControlService() {
    }



    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        throw new UnsupportedOperationException("Not yet implemented");
    }
}
