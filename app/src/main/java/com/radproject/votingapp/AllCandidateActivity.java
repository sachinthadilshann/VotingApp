package com.radproject.votingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.radproject.votingapp.adapter.CandidateAdapter;
import com.radproject.votingapp.model.Candidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AllCandidateActivity extends AppCompatActivity {

    private RecyclerView candidateRV;
    private Button startBtn;
    private List<Candidate> list;
    private CandidateAdapter adapter;

    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_candidate);

        candidateRV = findViewById(R.id.candidate_rv);
        startBtn = findViewById(R.id.start_btn);
        firebaseFirestore = FirebaseFirestore.getInstance();

        list = new ArrayList<>();
        adapter = new CandidateAdapter(this,list);
        candidateRV.setLayoutManager(new LinearLayoutManager(this));
        candidateRV.setAdapter(adapter);

        if(FirebaseAuth.getInstance().getCurrentUser()!=null){
            firebaseFirestore.collection("Candidate")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {

                             if(task.isSuccessful()){

                                 for(DocumentSnapshot snapshot : Objects.requireNonNull(task).getResult()){
                                        list.add(new Candidate(
                                                snapshot.getString("name"),
                                                snapshot.getString("party"),
                                                snapshot.getString("election"),
                                                snapshot.getString("image"),
                                                snapshot.getId()
                                        ));
                                 }

                                 adapter.notifyDataSetChanged();
                             }else{
                                 Toast.makeText(AllCandidateActivity.this,"Candidate not found", Toast.LENGTH_SHORT);
                             }
                        }
                    });
        }
    }
}