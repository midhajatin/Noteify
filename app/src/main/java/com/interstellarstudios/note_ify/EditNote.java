package com.interstellarstudios.note_ify;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.interstellarstudios.note_ify.models.Collection;
import com.interstellarstudios.note_ify.models.Note;
import com.interstellarstudios.note_ify.util.AlertReceiver;
import com.interstellarstudios.note_ify.util.DatePickerFragment;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import es.dmoral.toasty.Toasty;
import jp.wasabeef.richeditor.RichEditor;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class EditNote extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private Context context = this;
    private String mCurrentUserId;
    private FirebaseFirestore mFireBaseFireStore;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int SPEECH_INPUT_REQUEST = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int PICK_DOCUMENT_REQUEST = 4;
    private static final int AUDIO_RECORD_REQUEST = 5;
    private Uri mImageUri;
    private EditText editTextTitle;
    private NumberPicker numberPickerPriority;
    private String noteDate;
    private static final int PERMISSION_USE_CAMERA_REQUEST = 12;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST = 13;
    private String pathToFile;
    private File photoFile = null;
    private RichEditor mEditor;
    private String downloadUrl;
    private ProgressDialog progressDialog;
    private TextView attachmentTextView;
    private ImageView attachment_icon;
    private String filePath;
    private String fileName;
    private String title;
    private String description;
    private int priority;
    private int updatedRevision;
    private String folderId;
    private String noteId;
    private String attachmentUrl = "";
    private String attachment_name = "";
    private ImageView playAudioIcon;
    private TextView playAudioText;
    private Uri audioFileUri;
    private String audioDownloadUrl = "";
    private String audioZipDownloadUrl = "";
    private String audioZipFileName = "";
    private String localNoteId = UUID.randomUUID().toString();
    private View audioOverlay;
    private View imageOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        FirebaseAuth mFireBaseAuth = FirebaseAuth.getInstance();
        mFireBaseFireStore = FirebaseFirestore.getInstance();

        if (mFireBaseAuth.getCurrentUser() != null) {
            mCurrentUserId = mFireBaseAuth.getCurrentUser().getUid();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView priorityTextView = findViewById(R.id.priorityTextView);

        final HorizontalScrollView horizontalScrollView = findViewById(R.id.horizontalScrollView);
        horizontalScrollView.setVisibility(View.GONE);

        ImageView buttonBackground = findViewById(R.id.buttonBackground);
        editTextTitle = findViewById(R.id.edit_text_title);
        numberPickerPriority = findViewById(R.id.number_picker_priority);
        ImageView download_icon = findViewById(R.id.download_icon);
        TextView download_textview = findViewById(R.id.download_textview);
        attachmentTextView = findViewById(R.id.attachment_textview);
        attachment_icon = findViewById(R.id.attachment_icon);
        playAudioText = findViewById(R.id.audio_textview);
        playAudioIcon = findViewById(R.id.play_icon);
        mEditor = findViewById(R.id.mEditor);

        download_icon.setVisibility(View.GONE);
        download_textview.setVisibility(View.GONE);
        attachment_icon.setVisibility(View.GONE);
        attachmentTextView.setVisibility(View.GONE);
        playAudioText.setVisibility(View.GONE);
        playAudioIcon.setVisibility(View.GONE);

        String bundleTitle;
        String bundleDescription;
        int bundlePriority;

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            folderId = bundle.getString("folderId");
            noteId = bundle.getString("noteId");
            bundleTitle = bundle.getString("title");
            bundleDescription = bundle.getString("description");
            bundlePriority = bundle.getInt("priority");
            attachmentUrl = bundle.getString("attachmentUrl");
            attachment_name = bundle.getString("attachmentName");
            audioDownloadUrl = bundle.getString("audioDownloadUrl");
            audioZipDownloadUrl = bundle.getString("audioZipDownloadUrl");
            audioZipFileName = bundle.getString("audioZipFileName");
            int revision = bundle.getInt("revision");
            updatedRevision = revision + 1;
        } else {
            return;
        }

        TextView textSpeech = findViewById(R.id.textView_speech);
        TextView textVoice = findViewById(R.id.textView_voice);

        audioOverlay = findViewById(R.id.note_audio_overlay);
        audioOverlay.setVisibility(View.GONE);
        audioOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioOverlay.getVisibility() == View.VISIBLE) {
                    audioOverlay.setVisibility(View.GONE);
                }
            }
        });

        FloatingActionButton fabVoiceNote = findViewById(R.id.fab_voice_note);
        fabVoiceNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordAudio();
                audioOverlay.setVisibility(View.GONE);
            }
        });

        FloatingActionButton fabSpeechText = findViewById(R.id.fab_speech_text);
        fabSpeechText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSpeechInput();
                audioOverlay.setVisibility(View.GONE);
            }
        });

        TextView textGallery = findViewById(R.id.textView_gallery);
        TextView textCamera = findViewById(R.id.textView_camera);

        imageOverlay = findViewById(R.id.note_image_overlay);
        imageOverlay.setVisibility(View.GONE);
        imageOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageOverlay.getVisibility() == View.VISIBLE) {
                    imageOverlay.setVisibility(View.GONE);
                }
            }
        });

        FloatingActionButton fabGallery = findViewById(R.id.fab_gallery);
        fabGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFileChooser();
                imageOverlay.setVisibility(View.GONE);
            }
        });

        FloatingActionButton fabCamera = findViewById(R.id.fab_camera);
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    getPermissionToUseCamera();
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    getPermissionToWriteStorage();
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                }
                imageOverlay.setVisibility(View.GONE);
            }
        });

        mEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    horizontalScrollView.setVisibility(View.VISIBLE);
                } else {
                    horizontalScrollView.setVisibility(View.GONE);
                }
            }
        });

        TextView toolbarShare = toolbar.findViewById(R.id.toolbar_share);
        toolbarShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
            }
        });

        TextView toolbarSave = toolbar.findViewById(R.id.toolbar_save);
        toolbarSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
                hideKeyboard(EditNote.this);
            }
        });

        ImageView button_choose_image = findViewById(R.id.button_choose_image);
        button_choose_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageOverlay.setVisibility(View.VISIBLE);
            }
        });

        ImageView button_audio = findViewById(R.id.button_audio);
        button_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioOverlay.setVisibility(View.VISIBLE);
            }
        });

        ImageView button_attachment = findViewById(R.id.button_attachment);
        button_attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    getPermissionToWriteStorage();
                } else {
                    new MaterialFilePicker()
                            .withActivity(EditNote.this)
                            .withRequestCode(PICK_DOCUMENT_REQUEST)
                            .withHiddenFiles(true)
                            .start();
                }
            }
        });

        playAudioIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAudioFile();
            }
        });

        playAudioText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAudioFile();
            }
        });

        ImageView button_reminder = findViewById(R.id.button_reminder);
        button_reminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment datePicker = new DatePickerFragment();
                datePicker.show(getSupportFragmentManager(), "date picker");
            }
        });

        String colorLightThemeString = "#" + Integer.toHexString(ContextCompat.getColor(context, R.color.colorPrimary));
        String colorLightThemeTextString = "#" + Integer.toHexString(ContextCompat.getColor(context, R.color.colorLightThemeText));
        mEditor.setEditorFontColor(Color.parseColor(colorLightThemeTextString));
        mEditor.setEditorFontSize(16);
        mEditor.setBackgroundColor(Color.parseColor(colorLightThemeString));
        mEditor.setPlaceholder("Start Writing");

        findViewById(R.id.action_insert_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.insert_link_prompt, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                alertDialogBuilder.setView(promptsView);

                final EditText userInput = promptsView.findViewById(R.id.editTextDialogUserInput);

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        String link = userInput.getText().toString();
                                        mEditor.insertLink(link, link);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        findViewById(R.id.action_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.undo();
            }
        });

        findViewById(R.id.action_redo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.redo();
            }
        });

        CheckBox action_bold = findViewById(R.id.action_bold);
        action_bold.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEditor.setBold();
            }
        });

        CheckBox action_italic = findViewById(R.id.action_italic);
        action_italic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEditor.setItalic();
            }
        });

        CheckBox action_underline = findViewById(R.id.action_underline);
        action_underline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEditor.setUnderline();
            }
        });

        CheckBox action_strikethrough = findViewById(R.id.action_strikethrough);
        action_strikethrough.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEditor.setStrikeThrough();
            }
        });

        findViewById(R.id.action_subscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setSubscript();
            }
        });

        findViewById(R.id.action_superscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setSuperscript();
            }
        });

        findViewById(R.id.action_heading1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(1);
            }
        });

        findViewById(R.id.action_heading2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(2);
            }
        });

        findViewById(R.id.action_heading3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(3);
            }
        });

        findViewById(R.id.action_heading4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(4);
            }
        });

        findViewById(R.id.action_heading5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(5);
            }
        });

        findViewById(R.id.action_heading6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(6);
            }
        });

        findViewById(R.id.action_indent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setIndent();
            }
        });

        findViewById(R.id.action_outdent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setOutdent();
            }
        });

        findViewById(R.id.action_align_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignLeft();
            }
        });

        findViewById(R.id.action_align_center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignCenter();
            }
        });

        findViewById(R.id.action_align_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignRight();
            }
        });

        findViewById(R.id.action_blockquote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBlockquote();
            }
        });

        findViewById(R.id.action_insert_bullets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBullets();
            }
        });

        findViewById(R.id.action_insert_numbers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setNumbers();
            }
        });

        if (attachment_name == null) {
            attachment_name = "";
        }

        if (attachmentUrl != null && !attachmentUrl.equals("")) {

            download_icon.setVisibility(View.VISIBLE);
            download_textview.setVisibility(View.VISIBLE);

            attachmentTextView.setVisibility(View.VISIBLE);
            attachment_icon.setVisibility(View.VISIBLE);
            attachmentTextView.setText(attachment_name);

            download_textview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(attachmentUrl));
                    startActivity(browserIntent);
                }
            });
        }

        if (audioDownloadUrl != null && !audioDownloadUrl.equals("")) {

            playAudioText.setVisibility(View.VISIBLE);
            playAudioIcon.setVisibility(View.VISIBLE);
        }

        if (audioZipFileName == null) {
            audioZipFileName = "";
        }

        if (audioZipDownloadUrl == null) {
            audioZipDownloadUrl = "";
        }

        numberPickerPriority.setMinValue(1);
        numberPickerPriority.setMaxValue(10);

        editTextTitle.setText(bundleTitle);
        mEditor.setHtml(bundleDescription);
        numberPickerPriority.setValue(bundlePriority);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd\nHH:mm");
        noteDate = sdf.format(calendar.getTime());

        progressDialog = new ProgressDialog(context);

        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean switchThemesOnOff = sharedPreferences.getBoolean("switchThemes", true);

        Window window = this.getWindow();
        View container = findViewById(R.id.container);

        if (switchThemesOnOff) {
            if (container != null) {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDarkTheme));
            }
            toolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDarkTheme));
            toolbarSave.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            toolbarShare.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            priorityTextView.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            editTextTitle.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            editTextTitle.setHintTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            DrawableCompat.setTint(editTextTitle.getBackground(), ContextCompat.getColor(context, R.color.colorPrimaryDarkTheme));

            String colorDarkThemeString = "#" + Integer.toHexString(ContextCompat.getColor(context, R.color.colorPrimaryDarkTheme));
            String colorDarkThemeTextString = "#" + Integer.toHexString(ContextCompat.getColor(context, R.color.colorPrimary));
            mEditor.setEditorFontColor(Color.parseColor(colorDarkThemeTextString));
            mEditor.setBackgroundColor(Color.parseColor(colorDarkThemeString));

            horizontalScrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDarkTheme));
            buttonBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.buttonBackgroundDarkTheme));

            audioOverlay.setBackgroundResource(R.drawable.transparent_overlay_primary_dark);
            textSpeech.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            textVoice.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            imageOverlay.setBackgroundResource(R.drawable.transparent_overlay_primary_dark);
            textGallery.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            textCamera.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));

        } else {

            window.setStatusBarColor(ContextCompat.getColor(context, R.color.colorPrimary));
            if (container != null) {
                container.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    private void recordAudio() {

        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, AUDIO_RECORD_REQUEST);
        } else {
            Toasty.error(context, "No audio recorder installed", Toast.LENGTH_LONG, true).show();
        }
    }

    private void playAudioFile() {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(audioDownloadUrl));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setDataAndType(Uri.parse(audioDownloadUrl), "audio/*");
        startActivity(intent);
    }

    private void uploadAudioFile() {

        progressDialog.setMessage("Uploading Audio File");
        progressDialog.show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream audioStream = getContentResolver().openInputStream(audioFileUri);
                    byte[] audioData = getBytes(audioStream);
                    byte[] zippedAudioFile = zipBytes("audio_file.m4a", audioData);

                    StorageReference audioStorageRef = FirebaseStorage.getInstance().getReference("Users/" + mCurrentUserId + "/Audio");
                    final StorageReference fileReference = audioStorageRef.child(System.currentTimeMillis() + ".zip");

                    UploadTask uploadTask = fileReference.putBytes(zippedAudioFile);
                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return fileReference.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {

                                Uri downloadUri = task.getResult();
                                audioZipDownloadUrl = downloadUri.toString();
                                audioZipFileName = "audio_file.zip";
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream audioStream = getContentResolver().openInputStream(audioFileUri);
                    byte[] audioData = getBytes(audioStream);

                    StorageReference audioStorageRef = FirebaseStorage.getInstance().getReference("Users/" + mCurrentUserId + "/Audio");
                    final StorageReference fileReference = audioStorageRef.child(System.currentTimeMillis() + ".m4a");

                    UploadTask uploadTask = fileReference.putBytes(audioData);
                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return fileReference.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {

                                Uri downloadUri = task.getResult();
                                audioDownloadUrl = downloadUri.toString();

                                playAudioIcon.setVisibility(View.VISIBLE);
                                playAudioText.setVisibility(View.VISIBLE);
                                playAudioText.setText("Play Audio File");

                                progressDialog.dismiss();
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread2.start();
    }

    public static byte[] zipBytes(String filename, byte[] input) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry(filename);
        entry.setSize(input.length);
        zos.putNextEntry(entry);
        zos.write(input);
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        c.set(Calendar.HOUR_OF_DAY, 6);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.MILLISECOND, 0);
        startAlarm(c);

        String reminderDateString = DateFormat.getDateInstance().format(c.getTime());
        Toasty.success(context, "Reminder set for 06:00 on " + reminderDateString, Toast.LENGTH_LONG, true).show();
    }

    private void startAlarm(Calendar c) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, i, 0);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    public void getPermissionToUseCamera() {

        new AlertDialog.Builder(context)
                .setTitle("Permission needed to access Camera")
                .setMessage("This permission is needed in order to take a photo immediately for use in Notes. Manually enable in Settings > Apps & notifications > Note-ify > Permissions.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_USE_CAMERA_REQUEST);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).create().show();
    }

    public void getPermissionToWriteStorage() {

        new AlertDialog.Builder(context)
                .setTitle("Permission needed to Write to External Storage")
                .setMessage("This permission is needed in order save images taken with the camera when accessed by the App. Manually enable in Settings > Apps & notifications > Note-ify > Permissions.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_USE_CAMERA_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toasty.success(context, "Camera permission granted", Toast.LENGTH_LONG, true).show();
            } else {
                Toasty.error(context, "Camera permission denied", Toast.LENGTH_LONG, true).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toasty.success(context, "External storage permission granted", Toast.LENGTH_LONG, true).show();
            } else {
                Toasty.error(context, "External storage permission denied", Toast.LENGTH_LONG, true).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoFile = createPhotoFile();

            if (photoFile != null) {
                pathToFile = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(context, "com.interstellarstudios.note_ify.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createPhotoFile() {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String name = sdf.format(calendar.getTime());

        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File image = null;

        try {
            image = File.createTempFile(name, ".jpg", storageDir);
        } catch (IOException e) {
        }
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(pathToFile);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_INPUT_REQUEST && resultCode == RESULT_OK) {

            if (data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mEditor.setHtml(result.get(0));
            }
        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {

            mEditor.focusEditor();
            mEditor.requestFocus();

            mImageUri = data.getData();

            progressDialog.setMessage("Uploading Image");
            progressDialog.show();

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(mImageUri);
                        Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        selectedImage = handleSamplingAndRotationBitmap(context, mImageUri);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] imageData = baos.toByteArray();

                        StorageReference ImageStorageRef = FirebaseStorage.getInstance().getReference("Users/" + mCurrentUserId + "/Images");
                        final StorageReference fileReference = ImageStorageRef.child(System.currentTimeMillis() + ".jpeg");

                        UploadTask uploadTask = fileReference.putBytes(imageData);
                        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return fileReference.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {

                                    Uri downloadUri = task.getResult();
                                    downloadUrl = downloadUri.toString();
                                    mEditor.insertImage(downloadUrl, "image_upload");

                                    progressDialog.dismiss();
                                }
                            }
                        });
                    } catch (Exception e) {
                    }
                }
            });
            thread.start();
        }

        if (requestCode == PICK_DOCUMENT_REQUEST && resultCode == RESULT_OK) {

            filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

            attachmentUpload();
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            galleryAddPic();

            mEditor.focusEditor();
            mEditor.requestFocus();

            mImageUri = Uri.fromFile(photoFile);

            progressDialog.setMessage("Uploading Image");
            progressDialog.show();

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(mImageUri);
                        Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        selectedImage = handleSamplingAndRotationBitmap(context, mImageUri);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] imageData = baos.toByteArray();

                        StorageReference ImageStorageRef = FirebaseStorage.getInstance().getReference("Users/" + mCurrentUserId + "/Images");
                        final StorageReference fileReference = ImageStorageRef.child(System.currentTimeMillis() + ".jpeg");

                        UploadTask uploadTask = fileReference.putBytes(imageData);
                        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return fileReference.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {

                                    Uri downloadUri = task.getResult();
                                    downloadUrl = downloadUri.toString();
                                    mEditor.insertImage(downloadUrl, "image_upload");

                                    progressDialog.dismiss();
                                }
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }

        if (requestCode == AUDIO_RECORD_REQUEST && resultCode == RESULT_OK) {

            if (data != null) {
                audioFileUri = data.getData();
                uploadAudioFile();
            }
        }
    }

    private void attachmentUpload() {

        final Uri file = Uri.fromFile(new File(filePath));
        StorageReference AttachmentStorageRef = FirebaseStorage.getInstance().getReference("Users/" + mCurrentUserId + "/Attachments");
        final StorageReference fileReference = AttachmentStorageRef.child(fileName);

        progressDialog.setMessage("Uploading Attachment");
        progressDialog.show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UploadTask uploadTask = fileReference.putFile(file);

                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return fileReference.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {

                                Uri downloadUri = task.getResult();
                                attachmentUrl = downloadUri.toString();

                                attachmentTextView.setVisibility(View.VISIBLE);
                                attachment_icon.setVisibility(View.VISIBLE);
                                attachmentTextView.setText(fileName);

                                attachment_name = fileName;

                                progressDialog.dismiss();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage) throws IOException {

        int MAX_HEIGHT = 290;
        int MAX_WIDTH = 290;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(context, img, selectedImage);
        return img;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            final float totalPixels = width * height;
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {

        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private void saveNote() {

        title = editTextTitle.getText().toString();
        description = mEditor.getHtml();
        priority = numberPickerPriority.getValue();

        if (title.trim().isEmpty()) {
            Toasty.info(context, "Please enter a title", Toast.LENGTH_LONG, true).show();
            return;
        }

        if (folderId.equals("Bin")) {
            DocumentReference documentPath = mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Bin").document(noteId);
            documentPath.set(new Note(noteId, "", title, description, priority, noteDate, "", updatedRevision, attachmentUrl, attachment_name, audioDownloadUrl, audioZipDownloadUrl, audioZipFileName));
        } else if (folderId.equals("Shared")) {
            DocumentReference documentPath = mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Public").document("Shared").collection("Shared").document(noteId);
            documentPath.set(new Note(noteId, "", title, description, priority, noteDate, "", updatedRevision, attachmentUrl, attachment_name, audioDownloadUrl, audioZipDownloadUrl, audioZipFileName));
        } else {
            DocumentReference documentPath = mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Main").document(folderId).collection(folderId).document(noteId);
            documentPath.set(new Note(noteId, "", title, description, priority, noteDate, "", updatedRevision, attachmentUrl, attachment_name, audioDownloadUrl, audioZipDownloadUrl, audioZipFileName));
        }

        Toasty.success(context, "Note Saved", Toast.LENGTH_LONG, true).show();
        finish();
    }

    @Override
    public void onBackPressed() {

        if (audioOverlay.getVisibility() == View.VISIBLE) {

            audioOverlay.setVisibility(View.GONE);
        } else if (imageOverlay.getVisibility() == View.VISIBLE) {

            imageOverlay.setVisibility(View.GONE);
        } else {

            title = editTextTitle.getText().toString();
            description = mEditor.getHtml();

            if (!title.trim().isEmpty() || description != null || !audioDownloadUrl.equals("")) {
                new AlertDialog.Builder(context)
                        .setTitle("Save as draft")
                        .setMessage("Do you want to save this note as a draft?")
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                saveDraft();
                            }
                        })
                        .setNegativeButton("Discard changes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void saveDraft() {

        DocumentReference DraftsDocumentPath = mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Main").document("Drafts");
        DraftsDocumentPath.set(new Collection("Drafts", noteDate));

        title = editTextTitle.getText().toString();
        description = mEditor.getHtml();
        priority = numberPickerPriority.getValue();

        final DocumentReference documentPath = mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Main").document("Drafts").collection("Drafts").document(localNoteId);
        documentPath.set(new Note(localNoteId, "", title, description, priority, noteDate, "", updatedRevision, attachmentUrl, attachment_name, audioDownloadUrl, audioZipDownloadUrl, audioZipFileName));

        Toasty.success(context, "Note saved to Drafts", Toast.LENGTH_LONG, true).show();
        finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void share() {

        title = editTextTitle.getText().toString();
        description = mEditor.getHtml();
        priority = numberPickerPriority.getValue();

        if (title.trim().isEmpty()) {
            Toasty.info(context, "Please enter a title", Toast.LENGTH_LONG, true).show();
            return;
        }

        if (description == null) {
            description = "";
        }

        Intent i = new Intent(context, Share.class);
        i.putExtra("title", title);
        i.putExtra("description", description);
        i.putExtra("priority", priority);
        i.putExtra("revision", updatedRevision);
        i.putExtra("noteDate", noteDate);
        i.putExtra("attachmentUrl", attachmentUrl);
        i.putExtra("attachment_name", attachment_name);
        i.putExtra("audioDownloadUrl", audioDownloadUrl);
        i.putExtra("audioZipDownloadUrl", audioZipDownloadUrl);
        i.putExtra("audioZipFileName", audioZipFileName);
        startActivity(i);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (audioOverlay.getVisibility() == View.VISIBLE) {
            audioOverlay.setVisibility(View.GONE);
        }
        if (imageOverlay.getVisibility() == View.VISIBLE) {
            imageOverlay.setVisibility(View.GONE);
        }
    }

    private void getSpeechInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, SPEECH_INPUT_REQUEST);
        } else {
            Toast.makeText(this, "This device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }
}
