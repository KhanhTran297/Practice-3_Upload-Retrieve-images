package hcmute.edu.vn.practice3_uploadretrieveimages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private Button bt_choose, bt_upload;
    private TextView tv_show_upload;
    private EditText et_name;
    private ImageView img_upload;
    private ProgressBar progressBar;
    private Uri imageUri;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private UploadTask uploadTask;
    private String imgName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt_choose = findViewById(R.id.bt_choose);
        bt_upload = findViewById(R.id.bt_upload);
        tv_show_upload = findViewById(R.id.tv_show_upload);
        et_name = findViewById(R.id.et_filename);
        img_upload = findViewById(R.id.img_view);
        progressBar = findViewById(R.id.progress_bar);

        storageReference = FirebaseStorage.getInstance().getReference("images");
        databaseReference = FirebaseDatabase.getInstance().getReference("images");

        bt_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFileChooser();
            }
        });

        bt_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uploadTask != null && uploadTask.isInProgress()){
                    Toast.makeText(MainActivity.this, "In progress! ", Toast.LENGTH_SHORT).show();
                }
                else {
                    uploadFile();
                }
            }
        });

        tv_show_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ImageActivity.class);
                startActivity(intent);
            }
        });
    }
    private void openFileChooser(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            imageUri = data.getData();
            Picasso.with(this).load(imageUri).into(img_upload);
        }
    }
    private String getFileExtensions(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType((contentResolver.getType(uri)));
    }
    private void uploadFile(){
        if(imageUri != null){
            StorageReference fileReference = storageReference.child(System.currentTimeMillis() +"."+getFileExtensions(imageUri));
            uploadTask = (UploadTask) fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @SuppressLint("Range")
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(0);
                                }
                            }, 5000);
                            Toast.makeText(MainActivity.this, "Upload Successfully!", Toast.LENGTH_SHORT).show();
//                            Context context = MainActivity.this;
//                            Cursor cursor = context.getContentResolver().query(imageUri, null, null, null, null);
//                            if (cursor != null && cursor.moveToFirst()) {
//                                imgName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
//                                cursor.close();
//                            }
//                            if(imgName.endsWith(".jpg")){
//                                imgName = et_name.getText().toString()+".jpg";
//                            }
//                            else if(imgName.endsWith(".png")){
//                                imgName = et_name.getText().toString()+".jpg";
//                            }
                            Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!urlTask.isSuccessful());
                            Uri downloadUrl = urlTask.getResult();

                            Upload upload = new Upload(et_name.getText().toString().trim(), downloadUrl.toString());
                            String uploadID = databaseReference.push().getKey();
                            databaseReference.child(uploadID).setValue(upload);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            progressBar.setProgress((int)progress);
                        }
                    });
        }
        else{
            Toast.makeText(this, "No file selected!", Toast.LENGTH_SHORT).show();
        }
    }
}