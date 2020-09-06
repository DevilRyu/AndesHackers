package com.example.appcamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.res.AssetFileDescriptor;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Debug;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.app.Activity;
import android.graphics.Bitmap;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.tensorflow.lite.Interpreter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1888;
    private ImageView imageViewXD;
    //Marcelin si no funciona borralo
    private TextView textView, tratamiento, medicamentos;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    Interpreter tflite;
    //directory donde se guardo la foto
    String currentPhotoPath;

    private int channelSize = 3;
    private int inputImageWidth = 256;
    private int inputImageHeight = 256;
    private int modelInputSize = inputImageHeight * inputImageWidth * channelSize;
    private float[][] resultArray = new float[1][10];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button takePic = (Button) this.findViewById(R.id.camera_access);
        Button loadPic = (Button) this.findViewById(R.id.load);
        textView = (TextView) this.findViewById(R.id.classification);
        imageViewXD =  (ImageView) this.findViewById(R.id.showPic);
        medicamentos = (TextView) this.findViewById(R.id.medicamentos);
        tratamiento = (TextView) this.findViewById(R.id.tratamiento);

        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d("tag","checking");
                    if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                        Log.d("tag", "checking2");
                    }
                    else
                    {
                        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        if(cameraIntent.resolveActivity(getPackageManager()) != null)
                        {
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                            } catch (IOException ex) {
                                Log.e("tag","error creating img");
                            }
                            Log.e("tag","error1");
                            if (photoFile != null) {
                                Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                        "com.example.android.fileprovider",
                                        photoFile);


                                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                                Log.e("tag","error2" + currentPhotoPath);
                                //imageViewXD.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
                            }
                        }
                    }
                }
            }
        });
        loadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentPhotoPath != null) {
                    //cargando el interpreter = loading the interpreter
                    try {
                        tflite = new Interpreter(loadModelFile());
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                    Bitmap photoXDXD = BitmapFactory.decodeFile(currentPhotoPath);
                    Bitmap resized = Bitmap.createScaledBitmap(photoXDXD, inputImageWidth, inputImageHeight, true);
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(modelInputSize);
                    byteBuffer.order(ByteOrder.nativeOrder());
                    int[] arrPixels = new int[inputImageHeight * inputImageWidth];
                    resized.getPixels(arrPixels,0,resized.getWidth(),0,0,resized.getWidth(), resized.getHeight());
                    FloatBuffer floatBuffer = FloatBuffer.allocate(modelInputSize);

                    int pixel = 0;
                    for(int i = 0; i < inputImageWidth; i++)
                        for(int j = 0; j < inputImageHeight; j++)
                        {
                            int pixelVal = arrPixels[pixel];
                            pixel++;
                            //byteBuffer.put((byte) (Integer.rotateRight(pixelVal,16) & 0xff));
                            // byteBuffer.put((byte) (Integer.rotateRight(pixelVal,8) & 0xff));
                            floatBuffer.put((float) (((int) (pixelVal & 0xff) )/ 255.0));
                        }

//                    byte[] bytesOfArrImg = new byte[modelInputSize];
//                    bytesOfArrImg = byteBuffer.array();
//                    float[] floatArrImg = new float[modelInputSize];
//                    for(int i = 0; i < modelInputSize; i++) {
//                        //conver the signed bytes to unsigened with and to 0xff
//                        floatArrImg[i] = bytesOfArrImg[i]/ (float) 255.0;
//                    }
                    tflite.run(floatBuffer,resultArray);
                    Log.e("tag","success");
                    //MODIFICAR SI MODELO CAMBIA, change the labels if models changes
                    String[] labelXD;
                    labelXD = new String[]{"actinic keratosis","basal cell carcinoma","dermatofibroma",
                            "melanoma", "nevus", "pigmented benign keratosis", "seborrheic keratosis",
                            "squamous cell carcinoma", "vascular lesion"};
                    float maximumClassVal = 0;
                    int classMaxPos = 0;
                    for(int i = 0; i < 9; i++) {
                        if(maximumClassVal < resultArray[0][i]) {
                            maximumClassVal = resultArray[0][i];
                            classMaxPos = i;
                        }
                        Log.e("tag", "xd" + resultArray[0][i]);
                    }

                    Log.e("tag", "Classification:" + labelXD[classMaxPos]);
                    textView.setText(labelXD[classMaxPos]);
                    
                    //Marcelin si esta mal borralo 
                    if(textView.getText().equals("actinic keratosis")){
                        medicamentos.setText("Topical fluorouracil Cream (0.5%).\n Imiquimod 5% cream. \n  Ingenol mebutate  0.015% gel");
                        tratamiento.setText("Destructive therapies: surgery, cryotherapy, dermabrasion, photodynamic therapy.\n Topical medications, \n Field ablation treatments: chemical peels, laser resurfacing");
                    }else if(textView.getText().equals("basal cell carcinoma")){
                        medicamentos.setText("");
                        tratamiento.setText("No Problem");
                    }else if(textView.getText().equals("dermatofibroma")){
                        medicamentos.setText("");
                        tratamiento.setText("no treatment is required unless the lesion is symptomatic. Liquid nitrogen cryotherapy may be an alternative treatment option");
                    }else if(textView.getText().equals("melanoma")){
                        medicamentos.setText("");
                        tratamiento.setText("No Problem");
                    }else if(textView.getText().equals("pigmented benign keratosis")){
                        medicamentos.setText("");
                        tratamiento.setText("Cryotherapy, Curettage/shave excision, or Electrodesiccation");
                    }else if(textView.getText().equals("seborrheic keratosis")){
                        medicamentos.setText("");
                        tratamiento.setText("Cryotherapy, Curettage/shave excision, or Electrodesiccation");
                    }else if(textView.getText().equals("squamous cell carcinoma")){
                        medicamentos.setText("");
                        tratamiento.setText("No problem");
                    }else if(textView.getText().equals("vascular lesion")){
                        medicamentos.setText("");
                        tratamiento.setText("No problem");
                    }else if(textView.getText().equals("nevus")){
                        medicamentos.setText("");
                        tratamiento.setText("No Problem");
                    }
                    
                    /*
                    int size = resized.getRowBytes()*resized.getHeight();
                    byte[] bytesOfArrImg = new byte[size];
                    ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                    resized.copyPixelsToBuffer(byteBuffer);
                    bytesOfArrImg = byteBuffer.array();
                    float[] floatArrImg = new float[size];
                    for(int i = 0; i < size; i++) {
                        //conver the signed bytes to unsigened with and to 0xff
                        floatArrImg[i] = (bytesOfArrImg[i] & 0xff) / (float) 255.0;
                    }*/




                    imageViewXD.setImageBitmap(photoXDXD);
//                    resized.recycle();
//                    photoXDXD.recycle();
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
               if(cameraIntent.resolveActivity(getPackageManager()) != null)
               {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Toast.makeText(this, "error creating camera intent", Toast.LENGTH_LONG).show();
                    }
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "com.example.android.fileprovider",
                                photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }


            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data)
//    {
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK)
//        {
//            Bitmap photo = (Bitmap) data.getExtras().get("data");
//            Log.e("tag","here");
//        }
//    }

    private File createImageFile() throws IOException{
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
        Log.e("tag","dddirecctory" + currentPhotoPath);
        Toast.makeText(this, currentPhotoPath, Toast.LENGTH_SHORT).show();
        return image;
    }

    /** Memory map the model file in Assets */
    private MappedByteBuffer loadModelFile() throws IOException{
        //Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("trained_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


}
