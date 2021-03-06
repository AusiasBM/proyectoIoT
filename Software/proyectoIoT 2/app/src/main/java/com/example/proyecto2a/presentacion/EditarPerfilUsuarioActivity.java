package com.example.proyecto2a.presentacion;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.example.proyecto2a.R;
import com.example.proyecto2a.datos.Usuarios;
import com.example.proyecto2a.modelo.Usuario;
import com.example.proyecto2a.presentacion.ResActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.MenuCompat;

import java.net.URI;
import java.util.concurrent.Callable;

public class EditarPerfilUsuarioActivity extends AppCompatActivity {
    private String idUsuario;
    private Usuario usuario;
    private EditText nombre;
    private EditText telefono;
    private EditText direccion;
    private EditText poblacion;
    private TextView correo;
    private FirebaseFirestore db;
    private static ProgressDialog progressDialog;
    private Usuarios usuarios;




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perfil_menu);

        //Diálogo de carga mientras se ponen los dats en los editText
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Revisando datos del usuario...");
        progressDialog.show();

        Bundle extra = getIntent().getExtras();
        idUsuario = extra.getString("id");




        nombre = findViewById(R.id.etNombre);
        telefono = findViewById(R.id.etTelefono);
        direccion = findViewById(R.id.etDireccion);
        poblacion = findViewById(R.id.etPoblacion);
        correo = findViewById(R.id.tvCorreo);

        usuarios = new Usuarios();

        //Cargar el usuario y poner los datos en su perfil
        cargarUsuarioPerfil(idUsuario);

        /*//extraemos el drawable en un bitmap
        Drawable originalDrawable = getResources().getDrawable(R.drawable.example_img);
        Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();

        //creamos el drawable redondeado
        RoundedBitmapDrawable roundedDrawable =
                RoundedBitmapDrawableFactory.create(getResources(), originalBitmap);

        //asignamos el CornerRadius
        roundedDrawable.setCornerRadius(originalBitmap.getHeight());

        ImageView imageView = (ImageView) findViewById(R.id.imagenPerfil);

        imageView.setImageDrawable(roundedDrawable);*/

        // Inicialización Volley (Hacer solo una vez en Singleton o Applicaction)

//Foto de usuario

       //*/


    }

    public void mostrarDatosUsuario(Usuario user){
        Log.d("PROVAAA URIIII", "" + user.getFoto());
        nombre.setText(user.getNombre());
        direccion.setText(user.getDirección());
        poblacion.setText(user.getPoblación());
        if (String.valueOf(user.getTelefono()).length() != 1){
            telefono.setText(String.valueOf(user.getTelefono()));
        }
        correo.setText(user.getCorreo());
        progressDialog.dismiss();
    }

    public void actualizarPerfilUsuario(){
        try{
            //Los if comprueban que los campos estén llenos o sinó se encarga de que los campos
            // que se actualizan en la bbdd contengan una cadena vacía para que, en caso de no
            //completar todos los campos (se deje alguno vacío) se puedan guardar el resto en Firestore
            if(nombre.getText().toString().equals(null)){
                usuario.setNombre("");
            }else {
                usuario.setNombre(nombre.getText().toString());
            }

            //En caso de que se introduzca un número de teléfono de menos o mas cifras de las que tocan, enviar un toast
            //avisando del error
            if (telefono.getText().toString().length() != 9 && telefono.getText().toString().length() != 0) {
                usuario.setTelefono(0);
                Toast.makeText(this, "Número de teléfono incorrecto", Toast.LENGTH_SHORT).show();
            } else if(telefono.getText().toString().length() == 0){
                usuario.setTelefono(0);
            } else {
                usuario.setTelefono(Integer.parseInt(telefono.getText().toString()));
            }

            if(direccion.getText().toString().length() == 0){
                usuario.setDirección("");
            }else {
                usuario.setDirección(direccion.getText().toString());
            }

            if(poblacion.getText().toString().length() == 0){
                usuario.setPoblación("");
            }else {
                usuario.setPoblación(poblacion.getText().toString());
            }

            //Llamar al método actualizarUsuarios de la clase Usuarios
            usuarios.actualizarUsuario(idUsuario, usuario);
            //Toast.makeText(this, "Guardados los cambios", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
           // Toast.makeText(this, "Error al modificar", Toast.LENGTH_SHORT).show();
        }
    }

    public void volverHome(View view){
        actualizarPerfilUsuario();
        Intent intent=new Intent(this, ResActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStop() {
        actualizarPerfilUsuario();
        finish();
        super.onStop();
    }


    //Cargar el usuario del perfil
    public void cargarUsuarioPerfil(String idUsuario){
        usuarios.getUsuarios().document(idUsuario).get().addOnCompleteListener(
                new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            usuario = task.getResult().toObject(Usuario.class);
                            mostrarDatosUsuario(usuario);
                        } else {
                            Log.e("Firebase", "Error al leer", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        actualizarPerfilUsuario();
        Intent intent = new Intent(this, ResActivity.class);
        startActivity(intent);
        finish();
    }

}




