package com.example.locmanager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private TextView textLatitude, textLongitude;
    private Button btnObterLocalizacao;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textLatitude = findViewById(R.id.textLatitude);
        textLongitude = findViewById(R.id.textLongitude);
        btnObterLocalizacao = findViewById(R.id.btnObterLocalizacao);

        // Inicializa o serviço de localização do Android
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnObterLocalizacao.setOnClickListener(v -> {
            checarPermissaoEAtivarGPS();
        });
    }

    private void checarPermissaoEAtivarGPS() {
        // Verifica se o usuário já deu permissão de GPS para o app
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Se não tiver permissão, abre a caixinha nativa do Android perguntando se permite
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            // Se já tem permissão, liga o sensor imediatamente
            ativarSensorGPS();
        }
    }

    private void ativarSensorGPS() {
        try {
            // Solicita atualizações de localização usando o provedor de GPS
            // 0: tempo mínimo em milissegundos entre atualizações
            // 0: distância mínima em metros entre atualizações
            // 'this' indica que esta classe gerencia as mudanças porque implementa LocationListener
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            Toast.makeText(this, "Buscando satélites... Fique em local aberto.", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    //  MÉTODOS OBRIGATÓRIOS DO LOCATIONLISTENER

    // Esse metodo roda automaticamente sempre que o GPS detectar que você se moveu ou achou o sinal
    @Override
    public void onLocationChanged(@NonNull Location location) {
        textLatitude.setText("Latitude: " + location.getLatitude());
        textLongitude.setText("Longitude: " + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this, "Por favor, ative o GPS do seu celular!", Toast.LENGTH_SHORT).show();
    }

    // Metodo que responde o que aconteceu apos o usuário clicar em "Permitir" ou "Negar" na caixinha
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ativarSensorGPS();
        } else {
            Toast.makeText(this, "Permissão negada! O app não funciona sem GPS.", Toast.LENGTH_SHORT).show();
        }
    }
}