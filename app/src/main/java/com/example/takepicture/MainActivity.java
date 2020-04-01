package com.example.takepicture;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class MainActivity extends AppCompatActivity {

    private HttpURLConnection httpConn;
    ImageView imageView;
    Button Upload_Btn;
    private ProgressDialog uploading;
    Uri photoURI;
    String currentPhotoPath;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //permission check
        PackageManager pm  = getPackageManager();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            ActivityCompat.requestPermissions(this, new String[] {PackageManager.FEATURE_CAMERA_ANY}, MY_CAMERA_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_CAMERA_REQUEST_CODE);
        }

        uploading = new ProgressDialog(MainActivity.this);
        uploading.setCancelable(false);
        uploading.setTitle("Uploading");
        uploading.setMessage("Image upload in progress...");


        imageView=(ImageView)findViewById(R.id.IdProf);
        Upload_Btn=(Button)findViewById(R.id.UploadBtn);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error occurred while creating the File", Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.takepicture.fileprovider",
                        photoFile);
                Log.d("photoURITAG", "dispatchTakePictureIntent: " + photoURI.getPath());
                Toast.makeText(this, "File saved at: " + photoURI.getPath(), Toast.LENGTH_SHORT).show();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        uploading.show();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap photo;
            try {
                //this method below fail on SDK29, take cautions!
                photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                toStringImage(photo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            uploading.dismiss();
        } else {
            Toast.makeText(this, "Fail to take picture. Please try again", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void toStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        Log.d("TAG", "toStringImage: " + encodedImage);
        new uploadimageapi().execute("http://192.168.100.122:8000/api/uploadImage", encodedImage);
    }

    private class uploadimageapi extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            uploading.show();
            String url = strings[0];
            String bitmapDecode = strings[1];
            Log.d("LOGTAG", "doInBackground: " + url);
            Log.d("LOGTAG", "doInBackground: " + bitmapDecode);
            String access_token = "Q39I9uEt7TGwgOJjXVuo5sFfOINrnYkz7NvlmrmvzBqFOHo1R5v6DImjAPmq";

            try{
                URL url1 = new URL(url);

                JSONObject params = new JSONObject();
                params.put("email", "farhanjuve@gmail.com");
                params.put("task", "task-02");
                params.put("img_file", bitmapDecode);
                Log.d("params",params.toString());

                httpConn = (HttpURLConnection) url1.openConnection();
                httpConn.setReadTimeout(15000);
                httpConn.setConnectTimeout(15000);
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("Authorization", "Bearer " + access_token);
                httpConn.setDoInput(true);
                httpConn.setDoOutput(true);
                httpConn.connect();

                StringBuilder result = new StringBuilder();
                boolean first = true;

                Iterator<String> itr = params.keys();

                while(itr.hasNext()){

                    String key= itr.next();
                    Object value = params.get(key);

                    if (first)
                        first = false;
                    else
                        result.append("&");

                    result.append(URLEncoder.encode(key, "UTF-8"));
                    result.append("=");
                    result.append(URLEncoder.encode(value.toString(), "UTF-8"));
                }

                Log.d("params result", result.toString());

                OutputStream os = httpConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
                writer.write(result.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = httpConn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                    StringBuffer sb = new StringBuffer("");
                    String line="";

                    while((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    uploading.dismiss();
                    return sb.toString();
                }
                else {
                    uploading.dismiss();
                    Toast.makeText(MainActivity.this, "ERROR CODE : 01 Fail to upload. Please try again", Toast.LENGTH_LONG).show();
                    return new String("false : "+responseCode);
                }
            } catch (Exception e) {
                uploading.dismiss();
                Toast.makeText(MainActivity.this, "ERROR CODE : 02 Fail to upload. Please try again", Toast.LENGTH_LONG).show();
                return new String("Exception: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null){
                Toast.makeText(MainActivity.this, " " + s, Toast.LENGTH_LONG).show();
                Log.d("LOGTAG", "onPostExecute: " + s);
                uploading.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                uploading.dismiss();
            }
        }
    }
}
