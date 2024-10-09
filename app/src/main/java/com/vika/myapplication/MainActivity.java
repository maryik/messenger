package com.vika.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.text.format.DateFormat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    private static final int SIGN_IN_CODE = 1;
    private RelativeLayout activity_main;
    private FirestoreRecyclerAdapter<Message, MessageViewHolder> adapter;
    private FloatingActionButton sendBtn;
    private FirebaseFirestore db;
    private RecyclerView listOfMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity_main = findViewById(R.id.activity_main);
        sendBtn = findViewById(R.id.btnSend);
        db = FirebaseFirestore.getInstance();
        listOfMessages = findViewById(R.id.list_of_messages);
        listOfMessages.setLayoutManager(new LinearLayoutManager(this));

        sendBtn.setOnClickListener(v -> {
            EditText textField = findViewById(R.id.messageField);
            String messageText = textField.getText().toString();
            if (messageText.isEmpty()) {
                Snackbar.make(activity_main, "Введите сообщение", Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Создание сообщения
            Message message = new Message(
                    FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                    messageText
            );

            // Сохранение сообщения в Firestore
            db.collection("messages").add(message)
                    .addOnSuccessListener(documentReference -> textField.setText(""))
                    .addOnFailureListener(e -> {
                        Snackbar.make(activity_main, "Ошибка отправки сообщения: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
        });

        // Проверка авторизации пользователя
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_CODE);
        } else {
            Snackbar.make(activity_main, "Вы авторизованы", Snackbar.LENGTH_LONG).show();
            displayAllMessages();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_CODE) {
            if (resultCode == RESULT_OK) {
                Snackbar.make(activity_main, "Вы авторизованы", Snackbar.LENGTH_LONG).show();
                displayAllMessages();
            } else {
                Snackbar.make(activity_main, "Вы не авторизованы: " + resultCode, Snackbar.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void displayAllMessages() {
        // Убедитесь, что поле messageTime правильно установлено в классе Message
        Query query = db.collection("messages").orderBy("messageTime");

        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Message, MessageViewHolder>(options) {
            @Override
            protected void onBindViewHolder(MessageViewHolder holder, int position, Message model) {
                holder.mess_user.setText(model.getUserName());
                holder.mess_text.setText(model.getTextMessage());
                // Убедитесь, что messageTime - это Timestamp или Date
                holder.mess_time.setText(DateFormat.format("dd-MM-yyyy HH:mm:ss", model.getMessageTime()));
            }

            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item, parent, false);
                return new MessageViewHolder(view);
            }
        };

        listOfMessages.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        } else {
            Snackbar.make(activity_main, "Адаптер не инициализирован", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView mess_user;
        TextView mess_time;
        TextView mess_text;

        public MessageViewHolder(View itemView) {
            super(itemView);
            mess_user = itemView.findViewById(R.id.message_user);
            mess_time = itemView.findViewById(R.id.message_time);
            mess_text = itemView.findViewById(R.id.message_text);
        }
    }
}
