package com.radproject.votingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.squareup.picasso.Picasso;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class VotingActivity extends AppCompatActivity {

    private CircleImageView image;
    private TextView name, party, election;
    private Button voteBtn;
    private FirebaseFirestore firebaseFirestore;
    private String TAG = VotingActivity.class.getName();
    StorageReference storageReference;
    FirebaseAuth fAuth;

    Interpreter tflite;
    private int imgX;
    private int imgY;

    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;

    public Bitmap oribitmap, testbitmap;
    public static Bitmap cropped;
    Uri imageuri;

    float[][] ori_embedding = new float[1][128];
    float[][] test_embedding = new float[1][128];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting);

        firebaseFirestore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        image = findViewById(R.id.img);
        name = findViewById(R.id.name);
        party = findViewById(R.id.party);
        election = findViewById(R.id.election);
        voteBtn = findViewById(R.id.vote_btn);

        try{
            tflite = new Interpreter(loadModelFile());
        }catch (Exception e){
            e.printStackTrace();
        }

        String url = getIntent().getStringExtra("image");
        String nm = getIntent().getStringExtra("name");
        String elec = getIntent().getStringExtra("election");
        String part = getIntent().getStringExtra("party");
        String id = getIntent().getStringExtra("id");

        Glide.with(this).load(url).into(image);
        name.setText(nm);
        election.setText(elec);
        party.setText(part);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        voteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAppPermission();
                String finish = "Voted";
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("finish",finish);
                userMap.put("deviceIp",getDevceIP());
                userMap.put(elec,id);

                firebaseFirestore.collection("Users")
                        .document(uid).update(userMap);

                Map<String, Object> candidateMap = new HashMap<>();
                candidateMap.put("deviceIp", getDevceIP());
                candidateMap.put("election",elec);
                candidateMap.put("timestamp", FieldValue.serverTimestamp());



                StorageReference profileRef = storageReference.child("users/"+fAuth.getCurrentUser().getUid()+"/profile.jpg");
                profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        try{
                            oribitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            face_detector(oribitmap, "original");
                            face_detector(testbitmap, "test");
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                double distance = calculate_distance(ori_embedding,test_embedding);

                if (distance<6.0)
                    Toast.makeText(VotingActivity.this,"Faces Matched"+test_embedding+""+ori_embedding, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(VotingActivity.this,"Faces did not matched", Toast.LENGTH_SHORT).show();

                firebaseFirestore.collection("Candidate/"+id+"/Vote")
                        .document(uid)
                        .set(candidateMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if(task.isSuccessful()){
                                    startActivity(new Intent(VotingActivity.this,ResultActivity.class));
                                    finish();
                                }else{
                                    Toast.makeText(VotingActivity.this,"Voted successfully", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
            }


        });
    }

    public void face_detector(final Bitmap bitmap, final String imagetype){
        final InputImage image = InputImage.fromBitmap(bitmap,0);
        FaceDetector detector = FaceDetection.getClient();
        detector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                            @Override
                            public void onSuccess(List<Face> faces) {
                                for (Face face : faces) {
                                    Rect bounds = face.getBoundingBox();
                                    cropped = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
                                    get_embeddings(cropped, imagetype);
                                }
                            }
                        }

                )
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                            }
                        }
                );
    }

    public void  get_embeddings(Bitmap bitmap, String imagetype){
        TensorImage inputImageBuffer;
        float[][] embedding = new float[1][128];

        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape();
        imgY = imageShape[1];
        imgX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        inputImageBuffer = loadImage(bitmap, inputImageBuffer);
        tflite.run(inputImageBuffer.getBuffer(),embedding);

        if(imagetype.equals("original"))
            ori_embedding = embedding;
        else if (imagetype.equals("test"))
            test_embedding = embedding;

    }

    private TensorImage loadImage(final Bitmap bitmap, TensorImage inputImageBuffer){
        inputImageBuffer.load(bitmap);
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize,cropSize))
                        .add(new ResizeOp(imgX,imgY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreProcessNormalizeOp())
                        .build();
        return  imageProcessor.process(inputImageBuffer);
    }

    private TensorOperator getPreProcessNormalizeOp(){
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }

    private double calculate_distance(float[][] ori_embedding,float[][] test_embedding){
        double sum = 0.0;
        for (int i=0;i<128;i++){
            sum=sum+Math.pow((ori_embedding[0][i]-test_embedding[0][i]),2.0);
        }
        return Math.sqrt(sum);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("facenet_int_quantized.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private void requestAppPermission() {
        Dexter.withActivity(VotingActivity.this)
                .withPermissions(Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if(multiplePermissionsReport.areAllPermissionsGranted()){
                            captureFrontPhoto();
                        }
                        if(multiplePermissionsReport.isAnyPermissionPermanentlyDenied()){
                            showSettingDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();


    }

    private void showSettingDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(VotingActivity.this);
        builder.setTitle("Need Permission");
        builder.setMessage("This app needs permission to use voting future. You can grant them in App Settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void openSettings(){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(),null);
        intent.setData(uri);
        startActivityForResult(intent,101);
    }

    private void captureFrontPhoto(){
        Log.d(TAG,"Preparing to take photo");
        Camera camera = null;

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        int frontCamera = 1;

        Camera.getCameraInfo(frontCamera, cameraInfo);

        try {
            camera = Camera.open(frontCamera);
            camera.enableShutterSound(false);
        } catch (RuntimeException e){
            Log.d(TAG, "Camera not available: " + 1);
            camera = null;
        }

        try{
            if (camera==null){
                Log.d(TAG,"Could not get camera instance");
            } else{
                Log.d(TAG, "Got the camera, creating the dummy surface texture");
                try{
                    camera.setPreviewTexture(new SurfaceTexture(0));
                    camera.startPreview();
                } catch (IOException e) {
                    Log.d(TAG,"Could not set the surface preview texture");
                    e.printStackTrace();
                }
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        try{
                            Bitmap testbitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        camera.release();
                    }
                });
            }
        } catch (Exception e) {
            camera.release();
        }

    }

    private String getDevceIP() {

        try{
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface inf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAddr = inf.getInetAddresses(); enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()){
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            Toast.makeText(VotingActivity.this, ""+e, Toast.LENGTH_SHORT).show();
        }
        return null;
    }
}