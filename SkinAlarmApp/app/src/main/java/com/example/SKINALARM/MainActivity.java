package com.example.SKINALARM;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

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
import android.graphics.Bitmap;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.graphics.BitmapFactory;
import org.tensorflow.lite.Interpreter;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1888;
    private ImageView imageViewXD;
    private TextView textView;
    private TextView theReco;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    Interpreter tflite;
    //directory donde se guardo la foto
    String currentPhotoPath;

    private int channelSize = 3;
    private int inputImageWidth = 256;
    private int inputImageHeight = 256;
    private int modelInputSize = inputImageHeight * inputImageWidth * channelSize * 4;
    private float[][] resultArray = new float[1][10];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button takePic = (Button) this.findViewById(R.id.camera_access);
        Button loadPic = (Button) this.findViewById(R.id.load);
        textView = (TextView) this.findViewById(R.id.classification);
        imageViewXD =  (ImageView) this.findViewById(R.id.showPic);
        theReco = (TextView) this.findViewById(R.id.reco);


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

                    int pixel = 0;
                    for(int i = 0; i < inputImageWidth; i++)
                        for(int j = 0; j < inputImageHeight; j++)
                        {
                            int pixelVal = arrPixels[pixel++];
                            //pixel++;
                            byteBuffer.putFloat((float)(Integer.rotateRight(pixelVal,16) & 0xff)/255f);
                            byteBuffer.putFloat((float)(Integer.rotateRight(pixelVal,8) & 0xff)/255f);
                            byteBuffer.putFloat( (float)(pixelVal & 0xff)/ 255f);
                        }

/*
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
*/
//                    byte[] bytesOfArrImg = new byte[modelInputSize];
//                    bytesOfArrImg = byteBuffer.array();
//                    float[] floatArrImg = new float[modelInputSize];
//                    for(int i = 0; i < modelInputSize; i++) {
//                        //conver the signed bytes to unsigened with and to 0xff
//                        floatArrImg[i] = bytesOfArrImg[i]/ (float) 255.0;
//                    }
                    tflite.run(byteBuffer,resultArray);
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
                    theReco.setText("Unfortunately we can not recommend medications for this kind of lesion. Please make an appointment with your doctor in the following days");



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
                //Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
               if(cameraIntent.resolveActivity(getPackageManager()) != null)
               {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        //Toast.makeText(this, "error creating camera intent", Toast.LENGTH_LONG).show();
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
        //Toast.makeText(this, currentPhotoPath, Toast.LENGTH_SHORT).show();
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
