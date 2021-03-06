package com.interstellarstudios.note_ify.adapters;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.interstellarstudios.note_ify.models.Collection;
import com.interstellarstudios.note_ify.MoveSelectFolder;
import com.interstellarstudios.note_ify.models.Note;
import com.interstellarstudios.note_ify.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;

import jp.wasabeef.richeditor.RichEditor;

public class NoteAdapter extends FirestoreRecyclerAdapter<Note, NoteAdapter.NoteHolder> {

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private String current_user_id = firebaseAuth.getCurrentUser().getUid();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private OnItemClickListener listener;
    private boolean switchThemesOnOff;
    private Context mContext;

    public NoteAdapter(@NonNull FirestoreRecyclerOptions<Note> options, SharedPreferences sharedPreferences, Context context) {
        super(options);
        switchThemesOnOff = sharedPreferences.getBoolean("switchThemes", true);
        mContext = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull NoteHolder holder, int position, @NonNull Note model) {

        holder.attachmentName.setVisibility(View.GONE);
        holder.attachment_icon.setVisibility(View.GONE);
        holder.playIcon.setVisibility(View.GONE);
        holder.playText.setVisibility(View.GONE);

        holder.textViewTitle.setText(model.getTitle());

        String fullDescription = model.getDescription();
        if (fullDescription != null) {
            String shortDescription;
            if (fullDescription.length() > 100) {
                shortDescription = fullDescription.substring(0, 100).trim() + "...";
            } else {
                shortDescription = fullDescription;
            }
            holder.mEditor.setHtml(shortDescription);
        }

        if (switchThemesOnOff) {
            String colorDarkThemeTextString = "#" + Integer.toHexString(ContextCompat.getColor(mContext, R.color.colorPrimary));
            holder.textViewTitle.setTextColor(Color.parseColor(colorDarkThemeTextString));
            holder.textViewDate.setTextColor(Color.parseColor(colorDarkThemeTextString));
            holder.textViewFromUserEmail.setTextColor(Color.parseColor(colorDarkThemeTextString));
            holder.textViewRevision.setTextColor(Color.parseColor(colorDarkThemeTextString));
            holder.textViewPriority.setTextColor(Color.parseColor(colorDarkThemeTextString));
            holder.container.setBackgroundResource(R.color.colorPrimaryDark);
            holder.container2.setBackgroundResource(R.drawable.rounded_edges_dark);

            String colorDarkThemeCardBackgroundString = "#" + Integer.toHexString(ContextCompat.getColor(mContext, R.color.cardBackgroundDarkTheme));
            holder.mEditor.setBackgroundColor(Color.parseColor(colorDarkThemeCardBackgroundString));
            holder.mEditor.setEditorFontColor(Color.parseColor(colorDarkThemeTextString));
        }

        holder.textViewPriority.setText("Priority: " + model.getPriority());

        holder.textViewDate.setText(model.getDate());
        holder.textViewFromUserEmail.setText(model.getFromEmailAddress());
        holder.textViewRevision.setText("Revision: " + model.getRevision());
        holder.attachmentName.setText(model.getAttachmentName());

        String attachmentURL = model.getAttachmentUrl();
        if (attachmentURL != null && !attachmentURL.equals("")) {

            holder.attachmentName.setVisibility(View.VISIBLE);
            holder.attachmentName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(attachmentURL));
                    mContext.startActivity(browserIntent);
                }
            });

            holder.attachment_icon.setVisibility(View.VISIBLE);
            holder.attachment_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(attachmentURL));
                    mContext.startActivity(browserIntent);
                }
            });
        }

        String audioDownloadUrl = model.getAudioUrl();
        if (audioDownloadUrl != null && !audioDownloadUrl.equals("")) {

            holder.playIcon.setVisibility(View.VISIBLE);
            holder.playIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(audioDownloadUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setDataAndType(Uri.parse(audioDownloadUrl), "audio/*");
                    mContext.startActivity(intent);
                }
            });

            holder.playText.setVisibility(View.VISIBLE);
            holder.playText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(audioDownloadUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setDataAndType(Uri.parse(audioDownloadUrl), "audio/*");
                    mContext.startActivity(intent);
                }
            });
        }
    }

    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item,
                parent, false);
        return new NoteHolder(v);
    }

    //Move - folder to Bin
    public void moveItem1(int position) {

        getSnapshots().getSnapshot(position).getReference();

        DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
        String id = snapshot.getId();

        DocumentReference from = snapshot.getReference();
        DocumentReference to = db.collection("Users").document(current_user_id).collection("Bin").document(id);
        moveFirestoreDocument(from, to);
    }

    //Move - Bin to Notebook
    public void moveItem2(int position) {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd\nHH:mm");
        String date = sdf.format(calendar.getTime());

        DocumentReference RestoredDocumentPath = db.collection("Users").document(current_user_id).collection("Main").document("Restored");
        RestoredDocumentPath.set(new Collection("Restored", date));

        getSnapshots().getSnapshot(position).getReference();

        DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
        String id = snapshot.getId();

        DocumentReference from = db.collection("Users").document(current_user_id).collection("Bin").document(id);
        DocumentReference to = db.collection("Users").document(current_user_id).collection("Main").document("Restored").collection("Restored").document(id);
        moveFirestoreDocument(from, to);
    }

    //Move - folder to folder
    public void moveItem3(int position, Context context, String folderId, String directory) {

        getSnapshots().getSnapshot(position).getReference();
        DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
        String id = snapshot.getId();

        Intent i = new Intent(context, MoveSelectFolder.class);
        i.putExtra("documentId", id);
        i.putExtra("fromFolderId", folderId);
        i.putExtra("directory", directory);
        context.startActivity(i);
    }

    public void deleteItem(int position) {

        getSnapshots().getSnapshot(position).getReference().delete();
    }

    class NoteHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle;
        RichEditor mEditor;
        TextView textViewPriority;
        TextView textViewDate;
        TextView textViewFromUserEmail;
        TextView textViewRevision;
        ImageView attachment_icon;
        TextView attachmentName;
        ImageView playIcon;
        TextView playText;
        ConstraintLayout container;
        ConstraintLayout container2;

        public NoteHolder(View itemView) {
            super(itemView);

            String colorLightThemeTextString = "#" + Integer.toHexString(ContextCompat.getColor(mContext, R.color.colorLightThemeText));
            String colorLightThemeCardBackgroundString = "#" + Integer.toHexString(ContextCompat.getColor(mContext, R.color.SecondaryLight));
            mEditor = itemView.findViewById(R.id.mEditor);
            mEditor.setInputEnabled(false);
            mEditor.setBackgroundColor(Color.parseColor(colorLightThemeCardBackgroundString));
            mEditor.setEditorFontColor(Color.parseColor(colorLightThemeTextString));

            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewPriority = itemView.findViewById(R.id.text_view_priority);
            textViewDate = itemView.findViewById(R.id.text_view_date);
            textViewFromUserEmail = itemView.findViewById(R.id.fromUserEmail);
            textViewRevision = itemView.findViewById(R.id.revision);
            attachment_icon = itemView.findViewById(R.id.attachment_icon);
            attachmentName = itemView.findViewById(R.id.attachmentName);
            playIcon = itemView.findViewById(R.id.audio_icon);
            playText = itemView.findViewById(R.id.audio_text);
            container = itemView.findViewById(R.id.container);
            container2 = itemView.findViewById(R.id.container2);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(getSnapshots().getSnapshot(position), position);
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    //Move method
    public void moveFirestoreDocument(final DocumentReference fromPath, final DocumentReference toPath) {
        fromPath.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null) {
                        toPath.set(document.getData())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        fromPath.delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                    }
                }
            }
        });
    }
}
