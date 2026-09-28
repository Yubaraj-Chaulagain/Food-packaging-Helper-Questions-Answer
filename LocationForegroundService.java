
package com.fulkumari.farattendance;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

public class LocationForegroundService extends Service {

    private static final String CHANNEL_ID =
            "FAR_LOCATION_CHANNEL";

    private static final int NOTIFICATION_ID = 1001;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        // Stop Location Service
        if (intent != null
                && "STOP".equals(intent.getAction())) {

            stopLocationUpdates();
            stopForeground(true);
            stopSelf();

            return START_NOT_STICKY;
        }

        // Check Location permission
        if (!hasLocationPermission()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Create ongoing notification
        Notification notification =
                new NotificationCompat.Builder(
                        this, CHANNEL_ID)

                        .setContentTitle(
                                "FAR Face Attendance"
                        )

                        .setContentText(
                                "Live location tracking is running"
                        )

                        .setSmallIcon(
                                android.R.drawable.ic_menu_mylocation
                        )

                        .setOngoing(true)

                        .setPriority(
                                NotificationCompat.PRIORITY_LOW
                        )

                        .build();

        // Start Foreground Location Service
        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                );

            } else {

                startForeground(
                        NOTIFICATION_ID,
                        notification
                );
            }

        } catch (SecurityException e) {

            stopSelf();
            return START_NOT_STICKY;
        }

        // Start GPS updates
        startLocationUpdates();

        return START_STICKY;
    }

    private boolean hasLocationPermission() {

        boolean fine =
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarse =
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        return fine || coarse;
    }

    private void startLocationUpdates() {

        if (!hasLocationPermission()) {
            stopSelf();
            return;
        }

        if (locationManager == null) {
            return;
        }

        // Remove old listener if already running
        stopLocationUpdates();

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(
                    Location location) {

                double latitude =
                        location.getLatitude();

                double longitude =
                        location.getLongitude();

                float accuracy =
                        location.getAccuracy();

                long timestamp =
                        location.getTime();

                // Save latest GPS coordinates locally
                getSharedPreferences(
                        "FAR_LOCATION",
                        MODE_PRIVATE
                ).edit()

                        .putString(
                                "latitude",
                                String.valueOf(latitude)
                        )

                        .putString(
                                "longitude",
                                String.valueOf(longitude)
                        )

                        .putFloat(
                                "accuracy",
                                accuracy
                        )

                        .putLong(
                                "timestamp",
                                timestamp
                        )

                        .apply();

                // Location is stored on the device.
                // Google Sheet upload is not included here.
            }

            @Override
            public void onProviderEnabled(
                    String provider) {
            }

            @Override
            public void onProviderDisabled(
                    String provider) {
            }
        };

        try {

            // GPS Location
            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,
                        5,
                        locationListener,
                        Looper.getMainLooper()
                );
            }

            // Network Location
            if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER)) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        10000,
                        10,
                        locationListener,
                        Looper.getMainLooper()
                );
            }

        } catch (SecurityException e) {

            stopLocationUpdates();
            stopForeground(true);
            stopSelf();
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "FAR Live Location",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Used for ongoing location tracking"
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void stopLocationUpdates() {

        if (locationManager != null
                && locationListener != null) {

            locationManager.removeUpdates(
                    locationListener
            );

            locationListener = null;
        }
    }

    @Override
    public void onDestroy() {

        stopLocationUpdates();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
