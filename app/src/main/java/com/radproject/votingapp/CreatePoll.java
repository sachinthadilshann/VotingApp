package com.radproject.votingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreatePoll extends AppCompatActivity {

    private ImageView candidateImage;
    private EditText candidateName, candidateParty;
    private Spinner candidateSpinner;
    private Button submitBtn;
    private Uri mainUri = null;
    private String [] election = {"President", "Parliament", "Municipal Council", "Urban Council", "Pradeshiya Sabha"};
    private StorageReference reference;
    private FirebaseFirestore firebaseFiretore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_poll);

        Intent data = getIntent();

        reference = FirebaseStorage.getInstance().getReference();
        firebaseFiretore = FirebaseFirestore.getInstance();

        candidateImage = findViewById(R.id.candidate_image);
        candidateName = findViewById(R.id.candidate_name);
        candidateParty = findViewById(R.id.candidate_party);
        candidateSpinner = findViewById(R.id.candidate_spinner);
        submitBtn = findViewById(R.id.candidate_submit_btn);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,election);

        candidateSpinner.setAdapter(adapter);

        candidateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(openGalleryIntent,1000);
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = candidateName.getText().toString().trim();
                String party = candidateParty.getText().toString().trim();
                String electionType = candidateSpinner.getSelectedItem().toString();

                if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(party) && !TextUtils.isEmpty(electionType) && mainUri!=null){

                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    StorageReference imagePath = reference.child("candidate_image").child(uid + ".jpg");
                    imagePath.putFile(mainUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if(task.isSuccessful()){
                                imagePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("name", name);
                                        map.put("party", party);
                                        map.put("election", electionType);
                                        map.put("image", uri.toString());
                                        map.put("timestamp", FieldValue.serverTimestamp());

                                        firebaseFiretore.collection("Candidate")
                                                .add(map)
                                                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentReference> task) {
                                                        if(task.isSuccessful()){
                                                            startActivity(new Intent(CreatePoll.this, MainActivity.class));
                                                            finish();
                                                        }else{
                                                            Toast.makeText(CreatePoll.this, "Data not stored", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });

                                    }
                                });
                            }else{
                                Toast.makeText(CreatePoll.this, "" + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else{
                    Toast.makeText(CreatePoll.this, "One or Many fields are empty.", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000){
            if(resultCode == Activity.RESULT_OK){
                Uri mainUri = data.getData();
            }
        }

    }

    }




}