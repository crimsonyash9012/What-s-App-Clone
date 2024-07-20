package com.example.chatapp.Repository;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.chatapp.model.ChatGroup;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.views.GroupsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ktx.Firebase;

import java.util.ArrayList;
import java.util.List;

// since no use of username and pass => no need for a model class
public class Repository {
    // acts as a bridge between the viewmodel and data source
    MutableLiveData<List<ChatGroup>> chatGroupMutableLiveData;
    FirebaseDatabase database;
    DatabaseReference reference;
    DatabaseReference groupReference;

    MutableLiveData<List<ChatMessage>> messagesLiveData;

    public Repository() {
        this.chatGroupMutableLiveData = new MutableLiveData<>();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        messagesLiveData = new MutableLiveData<>();

    }

    // we can move this code to a new class but it's less cluttered here => no need to do so

    // auth
    public void firebaseAnonymousAuth(Context context){
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Intent i = new Intent(context, GroupsActivity.class);
                            // a new task should be created for the
                            // activity which is being started
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(i);
                        }
                    }
                });
    }

    // getting current user ID
    public String getCurrentUserId(){
        return FirebaseAuth.getInstance().getUid();
    }

    // sign out functionality
    public void signOUT(){
        FirebaseAuth.getInstance().signOut();
    }

    // getting chat groups available from the firebase realtime DB

    public MutableLiveData<List<ChatGroup>> getChatGroupMutableLiveData() {
        List<ChatGroup> groupList = new ArrayList<>();
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear();
                // snapshot - represent specific location in firebase database
                // and can contain data or child data
                for(DataSnapshot dataSnapshot: snapshot.getChildren()){
                    ChatGroup group = new ChatGroup(dataSnapshot.getKey());
                    groupList.add(group);
                }

                chatGroupMutableLiveData.postValue(groupList);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        return chatGroupMutableLiveData;
    }
    public void createNewChatGroup(String groupName){
        reference.child(groupName).setValue(groupName);

    }


    // getting messages from live data
    public MutableLiveData<List<ChatMessage>> getMessagesLiveData(String groupName) {
        // child(groupName) : used to specify a child
        // node under the root reference
        groupReference = database.getReference().child(groupName);

        List<ChatMessage> messageList = new ArrayList<>();
        groupReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();

                for(DataSnapshot dataSnapshot: snapshot.getChildren()){
                    ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                    messageList.add(message);
                }
                messagesLiveData.postValue(messageList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return messagesLiveData;
    }

    public void sendMessage(String messageText, String chatGroup){
        DatabaseReference ref = database.getReference(chatGroup);

        if(!messageText.trim().equals("")){
            ChatMessage msg = new ChatMessage(
                    FirebaseAuth.getInstance().getCurrentUser()
                            .getUid()
                    , messageText
                    , System.currentTimeMillis()
            );

            String randomKey = ref.push().getKey();

            ref.child(randomKey).setValue(msg);
        }
    }
}
