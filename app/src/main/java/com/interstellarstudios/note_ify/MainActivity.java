package com.interstellarstudios.note_ify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.interstellarstudios.note_ify.database.NoteEntity;
import com.interstellarstudios.note_ify.database.RecentSearches;
import com.interstellarstudios.note_ify.fragments.BinFragment;
import com.interstellarstudios.note_ify.fragments.HomeFragment;
import com.interstellarstudios.note_ify.fragments.ProfileFragment;
import com.interstellarstudios.note_ify.fragments.SharedFragment;
import com.interstellarstudios.note_ify.models.Collection;
import com.interstellarstudios.note_ify.models.Note;
import com.interstellarstudios.note_ify.repository.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hotchemi.android.rate.AppRate;

public class MainActivity extends AppCompatActivity {

    private Context context = this;
    private Window window;
    private View container;
    private TextView textViewFragmentTitle;
    private FirebaseFirestore mFireBaseFireStore;
    private String mCurrentUserId;
    private Toolbar toolbar;
    private AutoCompleteTextView searchField;
    private BottomNavigationView bottomNav;
    private ImageView imageViewToolbarAdd;
    private Repository repository;
    private ArrayList<String> searchSuggestions = new ArrayList<>();
    private List<RecentSearches> recentSearchesList = new ArrayList<>();
    private ArrayList<String> recentSearchesStringArrayList = new ArrayList<>();

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    switch (item.getItemId()) {
                        case R.id.navigation_home:
                            selectedFragment = new HomeFragment();
                            textViewFragmentTitle.setText("Home");
                            break;
                        case R.id.navigation_bin:
                            selectedFragment = new BinFragment();
                            textViewFragmentTitle.setText("Bin");
                            break;
                        case R.id.navigation_shared:
                            selectedFragment = new SharedFragment();
                            textViewFragmentTitle.setText("Shared");
                            break;
                        case R.id.navigation_profile:
                            selectedFragment = new ProfileFragment();
                            textViewFragmentTitle.setText("Profile");
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_frame, selectedFragment).commit();
                    return true;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new Repository(getApplication());

        AppRate.with(this)
                .setInstallDays(7)
                .setLaunchTimes(5)
                .setRemindInterval(2)
                .monitor();
        AppRate.showRateDialogIfMeetsConditions(this);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_frame, new HomeFragment());
            fragmentTransaction.commit();
        }

        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        mFireBaseFireStore = FirebaseFirestore.getInstance();
        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            mCurrentUserId = firebaseUser.getUid();
        }

        window = this.getWindow();
        container = findViewById(R.id.container);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        textViewFragmentTitle = findViewById(R.id.text_view_fragment_title);
        textViewFragmentTitle.setText("Home");

        searchField = findViewById(R.id.searchField);
        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchAllNotes();
                    return true;
                }
                return false;
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, searchSuggestions);
        searchField.setAdapter(adapter);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        bottomNav.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_SELECTED);

        imageViewToolbarAdd = findViewById(R.id.toolbar_add);
        imageViewToolbarAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, NewNote.class);
                i.putExtra("folderId", "Notebook");
                i.putExtra("noteType", "note");
                startActivity(i);
            }
        });

        boolean darkModeOn = sharedPreferences.getBoolean("switchThemes", true);
        if (darkModeOn) {
            darkMode();
        } else {
            lightMode();
        }

        registerToken();
        getData();
    }

    private void lightMode() {

        window.setStatusBarColor(ContextCompat.getColor(context, R.color.colorPrimary));
        if (container != null) {
            container.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        toolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
        textViewFragmentTitle.setTextColor(ContextCompat.getColor(context, R.color.colorLightThemeText));

        bottomNav.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
        bottomNav.setItemIconTintList(ContextCompat.getColorStateList(context, R.color.bottom_nav_selector));

        ImageViewCompat.setImageTintList(imageViewToolbarAdd, ContextCompat.getColorStateList(context, R.color.colorLightThemeText));
    }

    private void darkMode() {

        window.setStatusBarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        if (container != null) {
            container.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        }

        toolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        textViewFragmentTitle.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));

        bottomNav.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        bottomNav.setItemIconTintList(ContextCompat.getColorStateList(context, R.color.bottom_nav_selector_light));

        ImageViewCompat.setImageTintList(imageViewToolbarAdd, ContextCompat.getColorStateList(context, R.color.colorPrimary));
    }

    private void registerToken() {

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();

                Map<String, Object> userToken = new HashMap<>();
                userToken.put("User_Token_ID", deviceToken);

                DocumentReference userTokenDocumentPath = mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("User_Details").document("User_Token");
                userTokenDocumentPath.set(userToken);
            }
        });
    }

    private void searchAllNotes() {

        recentSearchesList.clear();
        recentSearchesStringArrayList.clear();

        String searchTerm = searchField.getText().toString().trim().toLowerCase();

        recentSearchesList = repository.getRecentSearches();

        for (RecentSearches recentSearches : recentSearchesList) {
            String recentSearchesListString = recentSearches.getSearchTerm();
            recentSearchesStringArrayList.add(recentSearchesListString);
        }

        if (!recentSearchesStringArrayList.contains(searchTerm) && !searchTerm.equals("")) {
            long timeStamp = System.currentTimeMillis();
            RecentSearches recentSearches = new RecentSearches(timeStamp, searchTerm);
            repository.insert(recentSearches);

        } else if (recentSearchesStringArrayList.contains(searchTerm)) {
            long timeStampQuery = repository.getTimeStamp(searchTerm);
            RecentSearches recentSearchesOld = new RecentSearches(timeStampQuery, searchTerm);
            repository.delete(recentSearchesOld);

            long timeStamp = System.currentTimeMillis();
            RecentSearches recentSearchesNew = new RecentSearches(timeStamp, searchTerm);
            repository.insert(recentSearchesNew);
        }

        Intent i = new Intent(context, Search.class);
        i.putExtra("searchTerm", searchTerm);
        startActivity(i);
    }

    private void getData() {

        Repository repository = new Repository(getApplication());
        repository.deleteAllNotes();

        mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Bin")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {

                                Note note = document.toObject(Note.class);
                                String noteId = document.getId();
                                String title = note.getTitle();
                                String description = note.getDescription();
                                String date = note.getDate();
                                String fromEmailAddress = note.getFromEmailAddress();
                                int priority = note.getPriority();
                                int revision = note.getRevision();
                                String attachmentUrl = note.getAttachmentUrl();
                                String attachmentName = note.getAttachmentName();
                                String audioDownloadUrl = note.getAudioUrl();
                                String audioZipDownloadUrl = note.getAudioZipUrl();
                                String audioZipFileName = note.getAudioZipName();

                                NoteEntity noteEntity = new NoteEntity(noteId, "Bin", title, description, priority, date, fromEmailAddress, revision, attachmentUrl, attachmentName, audioDownloadUrl, audioZipDownloadUrl, audioZipFileName);
                                repository.insert(noteEntity);

                                searchSuggestions.add(title);
                            }
                        }
                    }
                });

        mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Public").document("Shared").collection("Shared")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                Note note = document.toObject(Note.class);
                                String noteId = document.getId();
                                String title = note.getTitle();
                                String description = note.getDescription();
                                String date = note.getDate();
                                String fromEmailAddress = note.getFromEmailAddress();
                                int priority = note.getPriority();
                                int revision = note.getRevision();
                                String attachmentUrl = note.getAttachmentUrl();
                                String attachmentName = note.getAttachmentName();
                                String audioDownloadUrl = note.getAudioUrl();
                                String audioZipDownloadUrl = note.getAudioZipUrl();
                                String audioZipFileName = note.getAudioZipName();

                                NoteEntity noteEntity = new NoteEntity(noteId, "Shared", title, description, priority, date, fromEmailAddress, revision, attachmentUrl, attachmentName, audioDownloadUrl, audioZipDownloadUrl, audioZipFileName);
                                repository.insert(noteEntity);

                                searchSuggestions.add(title);
                            }
                        }
                    }
                });

        mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Main")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {

                                Collection collection = document.toObject(Collection.class);
                                final String folderName = collection.getFolder();

                                mFireBaseFireStore.collection("Users").document(mCurrentUserId).collection("Main").document(folderName).collection(folderName)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document : task.getResult()) {

                                                        Note note = document.toObject(Note.class);
                                                        String noteId = document.getId();
                                                        String title = note.getTitle();
                                                        String description = note.getDescription();
                                                        String date = note.getDate();
                                                        String fromEmailAddress = note.getFromEmailAddress();
                                                        int priority = note.getPriority();
                                                        int revision = note.getRevision();
                                                        String attachmentUrl = note.getAttachmentUrl();
                                                        String attachmentName = note.getAttachmentName();
                                                        String audioDownloadUrl = note.getAudioUrl();
                                                        String audioZipDownloadUrl = note.getAudioZipUrl();
                                                        String audioZipFileName = note.getAudioZipName();

                                                        NoteEntity noteEntity = new NoteEntity(noteId, folderName, title, description, priority, date, fromEmailAddress, revision, attachmentUrl, attachmentName, audioDownloadUrl, audioZipDownloadUrl, audioZipFileName);
                                                        repository.insert(noteEntity);

                                                        searchSuggestions.add(title);
                                                    }
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
    }
}
