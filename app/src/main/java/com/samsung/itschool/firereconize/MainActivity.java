package com.samsung.itschool.firereconize;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> listFiles, listFoundFiles = new ArrayList<>();
    private ArrayList<float[]> listFeatures;
    private int counterAllImagesInPhone=0;
    private boolean isRunning,isReady1,isReady2;

    public static final String NETWORK_FILE = "model.pt";
    public static final int CAMERA_REQUEST_CODE = 001;
    public static final int FILE_REQUEST_CODE = 002;
    ProgressBar progressBar;
    RecyclerView recyclerView;
    TextView count;
    Button captureBtn, fileBtn, indexBtn;
    ImageButton stopBtn;
    RecyclerAdaptor adaptor;
    Bitmap imageBitmap = null;
    Classifier classifier;
    String pictureImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // выдача разрешений
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] perm= {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(perm,0) ;
        }
        // считаем сколько всего картинок на тел, нужно для прогресс индикатора
        countAllImagesInPhone();
        // загружаем ранее записанный в файл список уже обработанных фоток (по ним уже посчитан фичмап)
        getListAllImagesInPhone();
        classifier = new Classifier(Utils.assetFilePath(this, NETWORK_FILE));
        captureBtn = findViewById(R.id.capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent cameraIntent = new Intent(MedeStore.ACTION_IMAGE_e);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = timeStamp + ".jpg";
                File storageDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
                File file = new File(pictureImagePath);
//                Uri outputFileUri = Uri.fromFile(file);
                Uri uri = FileProvider.getUriForFile(view.getContext(),
                        BuildConfig.APPLICATION_ID + ".provider",file);
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(cameraIntent, 1);
            }
        });
        fileBtn = findViewById(R.id.file);
        fileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("*/*");
                startActivityForResult(galleryIntent, FILE_REQUEST_CODE);
            }
        });
        indexBtn = findViewById(R.id.btindex);
        progressBar=findViewById(R.id.progressBar);
        count=findViewById(R.id.count);
        indexBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRunning=true;
                stopBtn.setVisibility(View.VISIBLE);
                count.setText(""+0);
                count.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setMax(counterAllImagesInPhone);
                progressBar.setProgress(0);
                new IndexAsyncTask().execute();

            }
        });
        stopBtn = findViewById(R.id.btstop);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Завершаем, ожидайте...", Toast.LENGTH_LONG).show();
                isRunning=false;
            }
        });
        adaptor=new RecyclerAdaptor(listFoundFiles);
        recyclerView=findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this,4));
        recyclerView.setAdapter(adaptor);
    }

    // загружаем ранее записанные в файлах список уже обработанных фоток и их фичмапов
    private void getListAllImagesInPhone() {
        listFiles=new ArrayList<>();
        listFeatures=new ArrayList<>();
        new Thread(){
            @Override
            public void run() {
                Gson gson=new Gson();
                Context ctx=MainActivity.this;
                File resDir =ctx.getDir("hash_cache", ctx.MODE_PRIVATE);
                File indexFilenames = new File(resDir, "indexFilenames.txt");
                File indexFeatures = new File(resDir, "indexFeatures.txt");
                try {
                    Scanner scFilenames=new Scanner(new FileInputStream(indexFilenames));
                    Scanner scFeatures=new Scanner(new FileInputStream(indexFeatures));
                    while(scFilenames.hasNext()){
                        listFiles.add(scFilenames.nextLine());
                        try {
                            listFeatures.add(gson.fromJson(scFeatures.nextLine(), float[].class));
                        }catch (Exception e){}
                    }
                } catch (FileNotFoundException e) {
                    Log.d("app_hash",e.getMessage());
                }
                isReady2=true;
            }
        }.start();
    }
    // считаем сколько всего картинок на тел
    private void countAllImagesInPhone(){
        new Thread(){
            @Override
            public void run() {
                String[] projection = { MediaStore.MediaColumns.DATA};
                Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, null);
                counterAllImagesInPhone=cursor.getCount();
                cursor.close();
                isReady1=true;
            }
        }.start();
    }

    // поток в котором делается самая тяжелая операция - перебор всех картинок, получения фичмапов и сохранения все в файлы
    class IndexAsyncTask extends AsyncTask<Integer, Integer, String> {

        @Override
        protected String doInBackground(Integer... objects) {
            while(!isReady1 || !isReady2){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            Gson gson=new Gson();
            Context ctx=MainActivity.this;
            File resDir =ctx.getDir("hash_cache", ctx.MODE_PRIVATE);
            File indexFilenames = new File(resDir, "indexFilenames.txt");
            File indexFeatures = new File(resDir, "indexFeatures.txt");
            PrintWriter outFiles= null;
            PrintWriter outFeatures= null;
            try {
                outFiles = new PrintWriter(new FileOutputStream(indexFilenames,true));
                outFeatures = new PrintWriter(new FileOutputStream(indexFeatures,true));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            String[] projection = { MediaStore.MediaColumns.DATA};
            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                    null, null);
            int n=0;
            // бежим по списку картинок
            while (cursor.moveToNext() && isRunning) {
                publishProgress(++n);
                String imagePath = cursor.getString(0);
                if(listFiles.contains(imagePath)) continue;
                Bitmap imageBitmap = BitmapFactory.decodeFile(imagePath);
                // получаем фичмап по картинке из списка
                float[] vec = classifier.predict(imageBitmap);
                // добавляем фичмап и название картинки в списки
                outFiles.println(imagePath);
                outFeatures.println(gson.toJson(vec));
                outFiles.flush();
                outFeatures.flush();
                listFiles.add(imagePath);
                listFeatures.add(vec);
                Log.e("new_tag", ""+imagePath);
            }
            cursor.close();
            // Записываем список файлов и их фичей в 2-а файла
            outFiles.close();
            outFeatures.close();
            return null;
        }
        protected void onProgressUpdate(Integer... values) {
            count.setText(""+values[0]+"/"+counterAllImagesInPhone);
            progressBar.setProgress(values[0]);
        }
        @Override
        protected void onPostExecute(String o) {
            super.onPostExecute(o);
            stopBtn.setVisibility(View.INVISIBLE);
            count.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(MainActivity.this, "Индексация завершена, можно искать фото", Toast.LENGTH_SHORT).show();
        }
    }

    // считаем косинусное расстояние, и если оно близкое - то true
    public static boolean checkEqual(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            return false;
        }

        for (int i = 0; i < vec1.length; i++) {
            if (Float.compare(vec1[i], vec2[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    // считаем косинусное расстояние, и если оно близкое - то true
    public boolean cosine(float[] vec1, float[] vec2){
        boolean areEqual = checkEqual(vec1, vec2);

        if (areEqual) {
            Log.i("vec" ,"Массивы равны");
            return false;
        }
        // Проверяем, что размерности векторов совпадают
        if (vec1.length != vec2.length) {
            Log.i("vec" ,"Векторы имеют разную размерность");
            return false;
        }

        // Находим скалярное произведение векторов
        float dotProduct = 0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
        }

        // Находим длины векторов
        float vec1Length = 0;
        float vec2Length = 0;

        for (int i = 0; i < vec1.length; i++) {
            vec1Length += vec1[i] * vec1[i];
            vec2Length += vec2[i] * vec2[i];
        }

        vec1Length = (float) Math.sqrt(vec1Length);
        vec2Length = (float) Math.sqrt(vec2Length);

        // Находим косинусное расстояние
        float cosineDistance = dotProduct / (vec1Length * vec2Length);
        Log.i("cos" ,"Косинусное расстояние: " + cosineDistance);

        // Возвращаем true, если значение косинусного расстояния больше 0.9, иначе возвращаем false
        return cosineDistance > 0.92;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
//            imageBitmap = (Bitmap) data.getExtras().get("data");
            File imgFile = new  File(pictureImagePath);
            if(imgFile.exists()) {
                imageBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            }
        }
        if (requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData(); // String mimeType = getContentResolver().getType(uri);
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                imageBitmap = BitmapFactory.decodeStream(inputStream);
                if(imageBitmap==null) {
                    Toast.makeText(MainActivity.this, "Выбрано не изображение!", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (FileNotFoundException e) {
                Toast.makeText(MainActivity.this, "Нет файла", Toast.LENGTH_LONG).show();
            }
        }
        if (imageBitmap != null) {
            // тут предикт
            Toast.makeText(this, "Ищем, ожидайте...", Toast.LENGTH_LONG).show();
            float[] vec = classifier.predict(imageBitmap);
            for(int i=0;i<listFiles.size();i++){
                if(cosine(listFeatures.get(i),vec)){
                    listFoundFiles.add(listFiles.get(i));
                }
            }
            adaptor.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Что то пошло не так", Toast.LENGTH_LONG).show();
        }

    }

}

//package com.samsung.itschool.firereconize;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.google.gson.Gson;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.Scanner;
//
//public class MainActivity extends AppCompatActivity {
//    private ArrayList<String> listFiles,listFoundFiles=new ArrayList<>();
//    private ArrayList<float[]> listFeatures;
//    private int counterAllImagesInPhone=0;
//    private boolean isRunning,isReady1,isReady2;
//
//    public static final String NETWORK_FILE = "model.pt";
//    public static final int CAMERA_REQUEST_CODE = 001;
//    public static final int FILE_REQUEST_CODE = 002;
//    ProgressBar progressBar;
//    RecyclerView recyclerView;
//    TextView count;
//    Button captureBtn, fileBtn, indexBtn;
//    ImageButton stopBtn;
//    RecyclerAdaptor adaptor;
//    Bitmap imageBitmap = null;
//    Classifier classifier;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        // выдача разрешений
//         if (ActivityCompat.checkSelfPermission(this,
//                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
//             ActivityCompat.checkSelfPermission(this,
//                         Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//             String[] perm= {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
//             requestPermissions(perm,0) ;
//         }
//        // считаем сколько всего картинок на тел, нужно для прогресс индикатора
//        countAllImagesInPhone();
//        // загружаем ранее записанный в файл список уже обработанных фоток (по ним уже посчитан фичмап)
//        getListAllImagesInPhone();
//        classifier = new Classifier(Utils.assetFilePath(this, NETWORK_FILE));
//        captureBtn = findViewById(R.id.capture);
//        captureBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
//            }
//        });
//        fileBtn = findViewById(R.id.file);
//        fileBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
//                galleryIntent.setType("*/*");
//                startActivityForResult(galleryIntent, FILE_REQUEST_CODE);
//            }
//        });
//        indexBtn = findViewById(R.id.btindex);
//        progressBar=findViewById(R.id.progressBar);
//        count=findViewById(R.id.count);
//        indexBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                isRunning=true;
//                stopBtn.setVisibility(View.VISIBLE);
//                count.setText(""+0);
//                count.setVisibility(View.VISIBLE);
//                progressBar.setVisibility(View.VISIBLE);
//                progressBar.setMax(counterAllImagesInPhone);
//    	    	progressBar.setProgress(0);
//                new IndexAsyncTask().execute();
//
//            }
//        });
//        stopBtn = findViewById(R.id.btstop);
//        stopBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "Завершаем, ожидайте...", Toast.LENGTH_LONG).show();
//                isRunning=false;
//            }
//        });
//        adaptor=new RecyclerAdaptor(listFoundFiles);
//        recyclerView=findViewById(R.id.recycler);
//        recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this,4));
//        recyclerView.setAdapter(adaptor);
//    }
//
//    // загружаем ранее записанные в файлах список уже обработанных фоток и их фичмапов
//    private void getListAllImagesInPhone() {
//        listFiles=new ArrayList<>();
//        listFeatures=new ArrayList<>();
//         new Thread(){
//            @Override
//            public void run() {
//                Gson gson=new Gson();
//                Context ctx=MainActivity.this;
//                File resDir =ctx.getDir("hash_cache", ctx.MODE_PRIVATE);
//                File indexFilenames = new File(resDir, "indexFilenames.txt");
//                File indexFeatures = new File(resDir, "indexFeatures.txt");
//                try {
//                    Scanner scFilenames=new Scanner(new FileInputStream(indexFilenames));
//                    Scanner scFeatures=new Scanner(new FileInputStream(indexFeatures));
//                    while(scFilenames.hasNext()){
//                          listFiles.add(scFilenames.nextLine());
//                          try {
//                              listFeatures.add(gson.fromJson(scFeatures.nextLine(), float[].class));
//                          }catch (Exception e){}
//                    }
//                } catch (FileNotFoundException e) {
//                    Log.d("app_hash",e.getMessage());
//                }
//                isReady2=true;
//            }
//        }.start();
//    }
//    // считаем сколько всего картинок на тел
//    private void countAllImagesInPhone(){
//        new Thread(){
//            @Override
//            public void run() {
//                String[] projection = { MediaStore.MediaColumns.DATA};
//                Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
//                    null, null);
//                counterAllImagesInPhone=cursor.getCount();
//                cursor.close();
//                isReady1=true;
//            }
//        }.start();
//    }
//
//    // поток в котором делается самая тяжелая операция - перебор всех картинок, получения фичмапов и сохранения все в файлы
//    class IndexAsyncTask extends AsyncTask<Integer, Integer, String> {
//
//        @Override
//        protected String doInBackground(Integer... objects) {
//            while(!isReady1 || !isReady2){
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            Gson gson=new Gson();
//            Context ctx=MainActivity.this;
//            File resDir =ctx.getDir("hash_cache", ctx.MODE_PRIVATE);
//            File indexFilenames = new File(resDir, "indexFilenames.txt");
//            File indexFeatures = new File(resDir, "indexFeatures.txt");
//            PrintWriter outFiles= null;
//            PrintWriter outFeatures= null;
//            try {
//                outFiles = new PrintWriter(new FileOutputStream(indexFilenames,true));
//                outFeatures = new PrintWriter(new FileOutputStream(indexFeatures,true));
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//            String[] projection = { MediaStore.MediaColumns.DATA};
//            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
//                    null, null);
//            int n=0;
//            // бежим по списку картинок
//            while (cursor.moveToNext() && isRunning) {
//                publishProgress(++n);
//                String imagePath = cursor.getString(0);
//                if(listFiles.contains(imagePath)) continue;
//                Bitmap imageBitmap = BitmapFactory.decodeFile(imagePath);
//                // получаем фичмап по картинке из списка
//                float[] vec = classifier.predict(imageBitmap);
//                // добавляем фичмап и название картинки в списки
//                outFiles.println(imagePath);
//                outFeatures.println(gson.toJson(vec));
//                outFiles.flush();
//                outFeatures.flush();
//                listFiles.add(imagePath);
//                listFeatures.add(vec);
//                Log.e("new_tag", ""+imagePath);
//            }
//            cursor.close();
//            // Записываем список файлов и их фичей в 2-а файла
//            outFiles.close();
//            outFeatures.close();
//            return null;
//        }
//        protected void onProgressUpdate(Integer... values) {
//            count.setText(""+values[0]+"/"+counterAllImagesInPhone);
//        	progressBar.setProgress(values[0]);
//        }
//        @Override
//        protected void onPostExecute(String o) {
//            super.onPostExecute(o);
//            stopBtn.setVisibility(View.INVISIBLE);
//            count.setVisibility(View.INVISIBLE);
//            progressBar.setVisibility(View.INVISIBLE);
//            Toast.makeText(MainActivity.this, "Индексация завершена, можно искать фото", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    public static boolean checkEqual(float[] vec1, float[] vec2) {
//        if (vec1.length != vec2.length) {
//            return false;
//        }
//
//        for (int i = 0; i < vec1.length; i++) {
//            if (Float.compare(vec1[i], vec2[i]) != 0) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    // считаем косинусное расстояние, и если оно близкое - то true
//    public boolean cosine(float[] vec1, float[] vec2){
//        boolean areEqual = checkEqual(vec1, vec2);
//
//        if (areEqual) {
//            Log.i("vec" ,"Массивы равны");
//            return false;
//        }
//        // Проверяем, что размерности векторов совпадают
//        if (vec1.length != vec2.length) {
//            Log.i("vec" ,"Векторы имеют разную размерность");
//            return false;
//        }
//
//        // Находим скалярное произведение векторов
//        float dotProduct = 0;
//        for (int i = 0; i < vec1.length; i++) {
//            dotProduct += vec1[i] * vec2[i];
//        }
//
//        // Находим длины векторов
//        float vec1Length = 0;
//        float vec2Length = 0;
//
//        for (int i = 0; i < vec1.length; i++) {
//            vec1Length += vec1[i] * vec1[i];
//            vec2Length += vec2[i] * vec2[i];
//        }
//
//        vec1Length = (float) Math.sqrt(vec1Length);
//        vec2Length = (float) Math.sqrt(vec2Length);
//
//        // Находим косинусное расстояние
//        float cosineDistance = dotProduct / (vec1Length * vec2Length);
//        Log.i("cos" ,"Косинусное расстояние: " + cosineDistance);
//
//        // Возвращаем true, если значение косинусного расстояния больше 0.9, иначе возвращаем false
//        return cosineDistance > 0.92;
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
//            imageBitmap = (Bitmap) data.getExtras().get("data");
//        }
//
//        if (requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK) {
//            Uri uri = data.getData(); // String mimeType = getContentResolver().getType(uri);
//            try {
//                InputStream inputStream = getContentResolver().openInputStream(uri);
//                imageBitmap = BitmapFactory.decodeStream(inputStream);
//                if(imageBitmap==null) {
//                    Toast.makeText(MainActivity.this, "Выбрано не изображение!", Toast.LENGTH_LONG).show();
//                    return;
//                }
//            } catch (FileNotFoundException e) {
//                Toast.makeText(MainActivity.this, "Нет файла", Toast.LENGTH_LONG).show();
//            }
//        }
//
//        if (imageBitmap != null) {
//            // тут предикт
//            Toast.makeText(this, "Ищем, ожидайте...", Toast.LENGTH_LONG).show();
//            float[] vec = classifier.predict(imageBitmap);
//            for(int i=0;i<listFiles.size();i++){
//                if(cosine(listFeatures.get(i), vec)){
//                    listFoundFiles.add(listFiles.get(i));
//                }
//            }
//            adaptor.notifyDataSetChanged();
//        } else {
//            Log.e("err", "here");
//            Toast.makeText(this, "Что то пошло не так", Toast.LENGTH_LONG).show();
//        }
//
//    }
//
//}