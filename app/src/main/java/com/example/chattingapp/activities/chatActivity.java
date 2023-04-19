package com.example.chattingapp.activities;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.util.Base64;
import android.util.Log;
import android.view.View;


import com.example.chattingapp.adapters.ChatAdapter;
import com.example.chattingapp.databinding.ActivityChatBinding;

import com.example.chattingapp.models.ChatMessage;
import com.example.chattingapp.models.User;
import com.example.chattingapp.utilities.Constants;
import com.example.chattingapp.utilities.PreferenceManager;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;


import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;



public class chatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private User receiverUser;

    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;

    private DatabaseReference databaseReference;


   ////////////FireStore Database?/////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }
    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages , getBitmapFromEncodedString(receiverUser.image) , preferenceManager.getString(Constants.KEY_USER_ID) , databaseReference
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);

        FirebaseDatabase database = FirebaseDatabase.getInstance();///FireStore implementation






    }

    private void sendMessages(){

        ///Message Service
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getBoolean(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP , new Date());

        /*reference.child(Constants.KEY_COLLECTION_CHAT).push().setValue(message);*/

        reference.push().setValue(message);

       // database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        binding.inputMessage.setText(null);
    }

    private void listenMessages(){

if (databaseReference != null) {
    databaseReference.child("messages").child(Constants.KEY_SENDER_ID).child(Constants.KEY_RECEIVER_ID)
            .addChildEventListener(new ChildEventListener() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                    if (dataSnapshot.exists()) {
                        ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                        chatMessages.add(message);
                        chatAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled( DatabaseError databaseError) {
                }
            });
}else {
    Log.e(TAG,"Reference is null");
}

    }

    /* database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }*/


    //////In Below code EventListener(.google.firestore) type....

    private final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists()){
                int count = chatMessages.size();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()){

                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = dataSnapshot.child(Constants.KEY_SENDER_ID).getValue(String.class);
                    chatMessage.receiverId = dataSnapshot.child(Constants.KEY_RECEIVER_ID).getValue(String.class);
                    chatMessage.message = dataSnapshot.child(Constants.KEY_MESSAGE).getValue(String.class);
                    chatMessage.dateTime = getReadableDateTime(new Date(dataSnapshot.child(Constants.KEY_TIMESTAMP).getValue(Long.class)));
                    chatMessage.dateObject = new Date(dataSnapshot.child(Constants.KEY_TIMESTAMP).getValue(Long.class));
                    chatMessages.add(chatMessage);
                }
                Collections.sort(chatMessages,(obj1,obj2) -> obj1.dateObject.compareTo(obj2.dateObject));

                if (count == 0){
                    chatAdapter.notifyDataSetChanged();
                }else {
                    chatAdapter.notifyItemRangeInserted(chatMessages.size() , chatMessages.size());
                    binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() -1);
                }
                binding.chatRecyclerView.setVisibility(View.VISIBLE);
            }
            binding.progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };






    /* private final EventListener<QuerySnapshot> eventListener = (value , error) ->{
        if (error != null){
            return ;
        }
        if (value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new  ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages , (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));

            if (count == 0){
                chatAdapter.notifyDataSetChanged();
            }else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size() , chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() -1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    };*/

    private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
    private void loadReceiverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);

    }

    private void setListeners(){

        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessages());

    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd , yyyy - hh:mm a" , Locale.getDefault()).format(date);
    }

}