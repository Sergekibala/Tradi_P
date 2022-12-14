package com.example.tradi_p.tlogin;

import static com.example.tradi_p.common.Constants.AVATAR;
import static com.example.tradi_p.common.Constants.EMAIL;
import static com.example.tradi_p.common.Constants.FIRESTORE_INSTANCE;
import static com.example.tradi_p.common.Constants.NAME;
import static com.example.tradi_p.common.Constants.ONLINE;
import static com.example.tradi_p.common.Constants.STORAGE_INSTANCE;
import static com.example.tradi_p.common.Constants.TRADIPRATICIEN;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tradi_p.R;
import com.example.tradi_p.common.Util;
import com.example.tradi_p.no_internet.NoInternetActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class TSignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    //1 Variables globales
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private String name, email, password, confirmPassword;
    //5.1 Ajout de la variable FirebaseUser
    private FirebaseUser firebaseUser;
    //6.1 Ajout de la variable de liaison avec les collections du Cloud FireStore
    private CollectionReference collectionReference; // Le reste est dans la m??thode initFireStore
    //7.1 Ajout de la variable de liaison avec FirebaseStorage
    private StorageReference fileStorage;
    //7.5 Variables des Uri du fichier image de l'avatar utilisateur
    private Uri localFileUri; // L'Uri du fichier sur le terminal
    private Uri serverFileUri; // L'UrL du fichier stock?? dans le storage (on parle bien ici d'un U R L (ELLE))
    private String urlStorageAvatar; // Le String de l'url stock?? dans le storage pour l'ajouter dans la base Users
    //7.6 Variable pour la localisation de l'ImageView
    private ImageView ivAddAvatar;
    private String userID;
    //11 Ajout de la progressBar
    private View progressBar;

    //2 M??thode initUI pour faire le lien entre le design et le code
    public void initUI() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        /** 7.6 Bis lien avec le design **/
        ivAddAvatar = findViewById(R.id.ivAddAvatar);
        /** 11.1 Init ProgressBar **/
        progressBar = findViewById(R.id.progressBar);
    }

    //6 M??thode initFirebase pour initialiser les composants de Firebase
    private void initFirebase() {
        /** 6.2 Insertion dans Firestore **/
        collectionReference = FIRESTORE_INSTANCE.collection(TRADIPRATICIEN);  // Instance d??finie dans la classe des constantes

        /** 7.2 Initialisation du bucket pour le stockage des avatars utilisateurs **/
        fileStorage = STORAGE_INSTANCE.getReference();
    }

    //4 M??thode pour la gestion du clic du bouton signUp
    public void btnSignupClick(View v) {
        name = etName.getText().toString().trim();
        email = etEmail.getText().toString().trim();
        password = etPassword.getText().toString().trim();
        confirmPassword = etConfirmPassword.getText().toString().trim();

        //4.1 Les v??rifications
        //4.1.1 Si les cases sont vides
        if (name.equals("")) {
            etName.setError(getString(R.string.enter_name));
        } else if (email.equals("")) {
            etEmail.setError(getString(R.string.enter_email));
        } else if (password.equals("")) {
            etPassword.setError(getString(R.string.enter_password));
        } else if (confirmPassword.equals("")) {
            etConfirmPassword.setError(getString(R.string.confirm_password));
        }
        //4.1.2 Les patterns pour v??rifier si il s'agit bien d'un email
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etName.setError(getString(R.string.enter_correct_email));
        }
        //4.1.3 V??rification password identique
        else if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.password_mismatch));
        }
        //4.2 Si toutes les v??rifications sont valid??es, il est possible de s'enregistrer
        else {
            // 12 Ajout de la v??rification de la connection internet
            if (Util.connectionAvailable(this)) // Si la connexion fonctionne
            { // Alors on ex??cute la m??thode
                // 11.9 ProgressBar
                progressBar.setVisibility(View.VISIBLE);
                final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        // Ajout la m??thode addOnCompleteListener pour v??rifier la bonne transmition des
                        // informations ?? Firebase Authenticator
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull @org.jetbrains.annotations.NotNull Task<AuthResult> task) {
                                // 11.10 ProgressBar
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    /** 5.2 Association de l'utilisateur courant ?? FirebaseUser dans le cadre du
                                     * changement de nom **/
                                    firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                                    userID = firebaseUser.getUid();
                                    Log.i(TAG, "User creation " + userID);
//                               // Affichage d'un toast de r??ussite
//                               Toast.makeText(SignupActivity.this, R.string.user_created_successfully, Toast.LENGTH_SHORT).show();
//                               // Lancement de l'activit?? suivante
//                               startActivity(new Intent(SignupActivity.this, LoginActivity.class));
//                                /** 6.3 On appelle la m??thode updateOnlyUser() pour valider l'enreistrement dans Authenticator
//                                 * et dans RealTime db les 2 lignes comment??es ci-dessus ne s'affiche que si l'enregistrement
//                                 * c'est bien pass?? dans les 2 endroits
//                                 */
//                                updateOnlyUser();
                                    /** 8 On lance la bonne m??thode d'enregistrement dans la base en fonction de l'ajout d'un avatar ou non **/
                                    if (localFileUri != null) {
                                        updateNameAndPhoto();
                                    } else {
                                        updateNameOnly();
                                    }
                                } else {
                                    // Affichage d'un Toast d'erreur avec l'erreur de la task (%1$s)
                                    Toast.makeText(TSignUpActivity.this,
                                            getString(R.string.signup_failed, task.getException()),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                // A noter qu'il est possible d'ajouter la m??thode suivante, identique au else ci-dessus
//            .addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                  Toast.makeText(SignupActivity.this,
//                                       getString(R.string.signup_failed, task.getException()),
//                                       Toast.LENGTH_SHORT).show();
//                }
//            });
                // 9.1 Sinon
            } else {
                startActivity(new Intent(TSignUpActivity.this, NoInternetActivity.class));
            }
        }
    }

    /**
     * 5.1 Ajout de la m??thode pour changer le nom de l'utilisateur
     **/
    private void updateNameOnly() {
        // 11.5 ProgressBar
        progressBar.setVisibility(View.VISIBLE);
        // Utilisation de la m??thode UserProfileChangeRequest pour charger le nom de l'utilisateur
        // qui s'est enregistr??
        // Gestion de remplissage d'Authenticator
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etName.getText().toString().trim())
                .build();

        // Gestion du remplissage de la base de donn??es
        /** 5.3 Update du nom du profile utilisateur ?? partir de l'edittext  **/
        firebaseUser.updateProfile(request)
                // Ajout d'un listener qui affiche un Toast si tout c'est bien d??roul??
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull @org.jetbrains.annotations.NotNull Task<Void> task) {
                        // Tout c'est bien pass??
                        // 11.6 ProgressBar
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Cr??ation du HashMap pour la gestion des donn??es
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put(NAME, etName.getText().toString().trim());
                            hashMap.put(EMAIL, etEmail.getText().toString().trim());
                            hashMap.put(ONLINE, "true"); // User set ONLINE, true, car il est dans on profile
                            hashMap.put(AVATAR, ""); // Vide pour le moment
                            Log.i(TAG, "Name only " + userID);
                            // 11.7 ProgressBar
                            progressBar.setVisibility(View.VISIBLE);
                            // Envoie des donn??es vers Realtime db
                            collectionReference.document(userID).set(hashMap)
                                    // On v??rifie le bon d??roulement avec .addOnCompleteListener()
                                    // Si tout se passe bien l'utilisateur est dirig?? vers la page de login
                                    // A noter qu'il faut rappeler le contexte (l'endroit o?? s'ex??cute la m??thode
                                    // pour que l'action soit valid??e
                                    .addOnCompleteListener(TSignUpActivity.this, new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull @org.jetbrains.annotations.NotNull Task<Void> task) {
                                            //11.8 ProgressBar
                                            progressBar.setVisibility(View.GONE);
                                            // Affichage d'un toast de r??ussite
                                            Toast.makeText(TSignUpActivity.this, R.string.user_created_successfully,
                                                    Toast.LENGTH_SHORT).show();
                                            // Lancement de l'activit?? suivante
                                            startActivity(new Intent(TSignUpActivity.this, TloginActivity.class));
                                        }
                                    });

                        } else {
                            // Il y a un probl??me
                            Toast.makeText(TSignUpActivity.this,
                                    getString(R.string.nameUpdateFailed, task.getException()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * 7.3 Ajout de la m??thode pour la gestion de l'avatar de l'utilisateur
     * Ne pas oublier de la lier ?? l'imageView dans le XML
     **/
    public void pickImage(View v) {
        /**
         *  9 Ajout de la v??rification de la permission de parcourir les dossiers du terminal
         * Avant toute chose il faut ajouter la permission dans le manifest
         **/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // Ajout de l'intent qui va ouvrir la galerie du terminal pour choisir un photo : Intent.ACTION_PICK
            // Il faut ensuite ajouter l'espace de stockage dans lequel recherch??, ici les images stock??es sur tout le terminal
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // On demande de d??marr?? l'activit?? avec un r??sultat, en effet il faut r??cup??rer l'URL de l'espace de stockage (Storage)
            // de l'image pour le recopier dans notre base de donn??es Users
            startActivityForResult(intent, 101);
            // A noter que le request code peut-??tre n'importe quoi, il n'y en a un qu'un seul dans cette activit??
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    102);
        }
    }

    /**
     * 10 Ajout de la m??thode pour v??rifier si l'on ?? la permission ou non
     **/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 101);
            } else {
                Toast.makeText(this, R.string.access_permission_is_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 7.4 Action ?? effectu??e en r??sultat de la m??thode pickImage()
     **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // V??rifications
        // Le requestCode renvoy?? est-il le bon ?
        if (requestCode == 101) {
            // Il y'a bien eu s??lection d'image (sinon resultcode = RESULT_CANCELED)
            if (resultCode == RESULT_OK) {
                // Ajout des variables globales des uri cf 7.5
                localFileUri = data.getData();
                // Affectation de l'image s??lectionn??e ?? l'avatar (pour la variable globale cf 7.5)
                ivAddAvatar.setImageURI(localFileUri);
            }
        }
    }



    /**
     * 7.7 Ajout de la m??thode pour uploader l'image sur le storage et r??cup??rer son URL pour remplir la db Users
     */
    private void updateNameAndPhoto() {
        //  Renommage du fichier avec l'userid + le type de fichier (ici jpg)
        String strFileName = firebaseUser.getUid() + ".jpg";
        // On place la photo dans un dossier dans le storage
        final StorageReference fileRef = fileStorage.child("avatars_user/" + strFileName);
        // 11.2 Ajout progressBar
        progressBar.setVisibility(View.VISIBLE);
        // On fait l'upload
        fileRef.putFile(localFileUri)
                .addOnCompleteListener(TSignUpActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        // 11.3 ProgressBar
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // On r??cup??re l'url de l'image upload??e
                            fileRef.getDownloadUrl()
                                    .addOnCompleteListener(TSignUpActivity.this, new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            serverFileUri = task.getResult();
                                            urlStorageAvatar = serverFileUri.toString();

                                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(etName.getText().toString().trim())
                                                    .setPhotoUri(serverFileUri)
                                                    .build();

                                            firebaseUser.updateProfile(request)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull @org.jetbrains.annotations.NotNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                String userID = firebaseUser.getUid();

                                                                HashMap<String, String> hashMap = new HashMap<>();

                                                                hashMap.put(NAME, etName.getText().toString().trim());
                                                                hashMap.put(EMAIL, etEmail.getText().toString().trim());
                                                                hashMap.put(ONLINE, "true");
                                                                hashMap.put(AVATAR, urlStorageAvatar);


                                                                collectionReference.document(userID).set(hashMap)
                                                                        .addOnCompleteListener(TSignUpActivity.this, new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                Toast.makeText(TSignUpActivity.this, R.string.user_created_successfully, Toast.LENGTH_SHORT).show();
                                                                                startActivity(new Intent(TSignUpActivity.this, TloginActivity.class));
                                                                            }
                                                                        });
                                                            } else {
                                                                Toast.makeText(TSignUpActivity.this,
                                                                        getString(R.string.nameUpdateFailed, task.getException()),
                                                                        Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });

                                        }
                                    });
                        }
                    }
                });
    }


    /**
     * 12 Ajout des boutons next et send ?? la place du retour chariot du keyboard
     **/
    private TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Utilisation de actionId qui correspond ?? l'action ajouter dans le xml
            switch (actionId) {
                case EditorInfo.IME_ACTION_DONE:
                    btnSignupClick(v);
            }
            return false; // On laisse le return ?? false pour emp??cher le comportement normal du clavier
        }
    };

    //========== CYCLES DE VIE ==========//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tsign_up);
        //3 Appel de la m??thode initUI
        initUI();
        initFirebase();
        /** 12.2 Association du clic dans le keyboard **/
        etConfirmPassword.setOnEditorActionListener(editorActionListener);
    }
    //========== END CYCLES DE VIE ==========//
}
