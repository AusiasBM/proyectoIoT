package com.example.proyecto2a.presentacion;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.proyecto2a.R;
import com.example.proyecto2a.datos.Mqtt;
import com.example.proyecto2a.modelo.DatosAlquiler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.example.proyecto2a.datos.Mqtt.qos;
import static com.example.proyecto2a.datos.Mqtt.topicRoot;


public class ServicioReservaAlquilerPatinete extends Service implements MqttCallback {

    public static MqttClient client = null;
    private String payload;

    private String estant;
    private String id;
    private String ide;
    private String ubicacion;
    private boolean flagReserva;
    private boolean flagAbrirPuerta;


    private NotificationManager notificationManager;
    static final String CANAL_ID = "mi_canal";
    static final int NOTIFICACION_ID = 1;

    @Override public void onCreate() {

        try {
            Log.i(Mqtt.TAG, "Conectando al broker " + Mqtt.broker);
            client = new MqttClient(Mqtt.broker, Mqtt.clientId,
                    new MemoryPersistence());
            client.connect();
        } catch (MqttException e) {
            Log.e(Mqtt.TAG, "Error al conectar.", e);
        }


        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(60);
        connOpts.setWill(topicRoot+"WillTopic", "App desconectada".getBytes(), qos, false);
        try {
            client.connect(connOpts);
        } catch (MqttException e) {
            e.printStackTrace();
        }


        // Nos suscribimos al topic "tiempoExpirado"
        // Servirá para que una vez pasado un tiempo determinado desde que se reserva la taquilla
        // si no se ha alquilado, liberar la taquilla.

        try {
            Log.i(Mqtt.TAG, "Suscrito a " + topicRoot+"tiempoReserva");
            client.subscribe(topicRoot+"tiempoReserva", qos);
            client.setCallback(this);
        } catch (MqttException e) {
            Log.e(Mqtt.TAG, "Error al suscribir.", e);
        }

        try {
            Log.i(Mqtt.TAG, "Suscrito a " + topicRoot+"magnetico");
            client.subscribe(topicRoot+"magnetico", qos);
            client.setCallback(this);
        } catch (MqttException e) {
            Log.e(Mqtt.TAG, "Error al suscribir.", e);
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int idArranque) {

        try {
            //Cerrar el servicio de alquiler de taquilla cuando el usuario haga Logout
            if (intent.getAction().equals( "terminarPatin")) {
                //your end servce code
                stopForeground(true);
                //Parar el servicio con el id 0
                stopSelfResult(0);
                return START_NOT_STICKY;
            }
        }catch (Exception e){}


        Bundle e = intent.getExtras();
        estant = e.getString("estant");
        ide = e.getString("ide");
        id = e.getString("id");
        ubicacion = e.getString("ubicacion");
        flagReserva = e.getBoolean("flagReserva");
        flagAbrirPuerta = e.getBoolean("flagAbrir", false);

        if (flagAbrirPuerta == true){
            try {
                Log.i(Mqtt.TAG, "Publicando mensaje: " + "cerradura ON");
                MqttMessage message = new MqttMessage("cerradura ON".getBytes());
                message.setQos(Mqtt.qos);
                message.setRetained(false);
                client.publish(Mqtt.topicRoot + "cerradura", message);
            } catch (MqttException ex) {
                Log.e(Mqtt.TAG, "Error al publicar.", ex);
            }
        }
        DatosAlquiler d = new DatosAlquiler();
        d.setFlagReserva(flagReserva);
        d.setUbicacionTaquilla(ubicacion);
        FirebaseFirestore.getInstance().collection("usuarios").document(ide).update("datos", d);


        Log.d("Id ",  ide);
        //Crear la notificació
        notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CANAL_ID, "Mis Notificaciones",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Descripcion del canal");
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(this, CANAL_ID);

        if(flagReserva){
            notificacion.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Taquilla Reservada")
                    .setContentText("Notificación de taquilla reservada. Tiene 15 minutos para confirmar " +
                            "el alquiler o la reserva quedará anulada");
        }else {
            notificacion.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Taquilla Alquilada")
                    .setContentText("Notificación de taquilla alquilada.");
        }


        //Llançar l'aplicació des de la notificació
        Intent i = new Intent(this, MenuDialogActivity.class);
        i.putExtra("idUser", ide);
        i.putExtra("nombre", ubicacion);
        PendingIntent intencionPendiente = PendingIntent.getActivity(
                this, 0, i, 	FLAG_UPDATE_CURRENT);
        notificacion.setContentIntent(intencionPendiente);


        //Servici en primer pla (DECLARAR EN EL MANIFEST)
        startForeground(NOTIFICACION_ID, notificacion.build());


        return START_STICKY;
    }

    //Accions per a donar per acabat el servici
    @Override public void onDestroy() {
        try {
            Log.i(Mqtt.TAG, "Desconectado");
            client.disconnect();
        } catch (MqttException e) {
            Log.e(Mqtt.TAG, "Error al desconectar.", e);
        }
        super.onDestroy();
    }


    @Override public IBinder onBind(Intent intencion) {
        return null;
    }


    @Override
    public void connectionLost(Throwable cause) {
        Log.d(Mqtt.TAG, "Conexión perdida: " + cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        payload = new String(message.getPayload());
        Log.d(Mqtt.TAG, "Recibiendo: " + topic + "->" + payload);

        if(topic.equals(topicRoot+"tiempoReserva") && payload.equals("parar")){
            Log.d(Mqtt.TAG, "Recibiendo: " + topic + "->" + payload);
            finTiempoReserva();
            stopSelf();
        }

        if(topic.equals(topicRoot+"magnetico") && payload.equals("cerraduraCerrada")){
            Log.d(Mqtt.TAG, "Recibiendo: " + topic + "->" + payload);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("estaciones").document(estant).collection("taquillas").document(id)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        String uid = task.getResult().getString("idUsuario");
                        if(uid.equals(ide)){
                            taquillaOcupadaCerrada();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(Mqtt.TAG, "Entrega completa");
    }

    public void finTiempoReserva(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("usuarios").document(ide).update("reservaAlquiler", false);
        db.collection("usuarios").document(ide).update("datos", new DatosAlquiler());

        DocumentReference dc = db.collection("estaciones").document(estant).collection("taquillas").document(id);
        dc.update("reservada", false);
        dc.update("idUsuario", "");

    }

    //Comprobar que la taquilla cerrada y vacía. Sirve para que cuando el usuario retira el patín alquilado
    //la taquilla pase a estar disponible para dejar un patín de la empresa
    private void taquillaOcupadaCerrada(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference document = db.collection("estaciones").document(estant).collection("taquillas").document(id);
        document.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task){
                if (task.isSuccessful()) {
                    boolean puertaAbierta = task.getResult().getBoolean("puertaAbierta");
                    boolean ocupada = task.getResult().getBoolean("ocupada");
                    Log.d("puertaAbierta", puertaAbierta+"");
                    if (puertaAbierta==false && ocupada == false) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("estaciones").document(estant).collection("taquillas").document(id)
                                .update("idUsuario", "");
                        db.collection("estaciones").document(estant).collection("taquillas").document(id)
                                .update("reservada", false);
                        db.collection("estaciones").document(estant).collection("taquillas").document(id)
                                .update("alquilada", false);
                        db.collection("estaciones").document(estant).collection("taquillas").document(id)
                                .update("estacionFinal", true);
                    }
                }
            }
        });
    }


}