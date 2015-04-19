package com.sciops.home.dibble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.widget.TextView.OnEditorActionListener;


public class MainActivity extends Activity {
    static final int PICK_CONTACT_REQUEST = 792415;  // The request code
    public final static String EXTRA_MESSAGE = "com.sciops.home.dibble.MESSAGE";
    static final int REQUEST_IMAGE_CAPTURE = 792416;
    static final int SEND_INTENT_ID = 564561;
    static final String IMG_STORAGE_KEY = "photoDibble";
    ImageView mImageView = null;
    EditText editText = null;
    final String filename = "dibble-photo";
    final String filepath = "content://media/external/images/media/" + filename;
    final Uri mmsUri = Uri.parse(filepath);

    OnEditorActionListener listener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //save the text field
                storeField(editText.getId());
                handled = true;
            }
            return handled;
        }
    };

    public void setMessageField(String message, int id) {
        editText = (EditText) findViewById(id);
        editText.setText(message);
    }

    public String getMessageField(int id) {
        editText = (EditText) findViewById(id);
        return String.valueOf(editText.getText());
    }

    private void setImageDisplayed(Bitmap bitmap) {
        mImageView = (ImageView) findViewById(R.id.mImageView);
        mImageView.setImageBitmap(bitmap);
        String msg = bitMapToString(bitmap);
        // also store it
        storeString(IMG_STORAGE_KEY,msg);
    }

    private void retrieveImageDisplayed() {
        mImageView = (ImageView) findViewById(R.id.mImageView);
        String msg = retrieveString(IMG_STORAGE_KEY);
        Bitmap bitmap = stringToBitMap(msg);
        mImageView.setImageBitmap(bitmap);
    }

    private void storeField(int id) {
        String key = ""+id;
        String message = getMessageField(id);
        storeString(key,message);
    }

    private String retrieveField(int id) {
        String key = ""+id;
        String message = retrieveString(key);
        setMessageField(message, id);
        return message;
    }

    public void storeString(String key, String message) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, message);
        editor.commit();
    }

    public String retrieveString(String key) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString(key, "");
    }

    /*
    //http://androidforums.com/threads/send-an-image-action_send.25988/
    //this correctly attaches photo but does not specify recipient or message.
    private void sendComposeIntent() {
        File F = getFileStreamPath(filename + ".jpg");
        Uri U = Uri.fromFile(F);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/jpg");
        i.putExtra(Intent.EXTRA_STREAM, U);
        startActivity(Intent.createChooser(i, "Send Image To:"));
    }
    */

    /*
    //modified above. will compose message with image and message text, but contact must be chosen each time because it's not SENDTO
    //attempts to modify this with SENDTO result in intent filter problem.
    //doesnt work for 2.1 eris: messaging intent incompatible.
    private void sendComposeIntent(String message) {
        Intent intent = null;
        File file = getFileStreamPath(filename + ".jpg");
        Uri uri = Uri.fromFile(file);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {// Android 4.4 and up
            intent = new Intent(Intent.ACTION_SEND);//working line but without phone number passed
            intent.setType("image/jpg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, message);//https://developer.android.com/training/sharing/send.html#send-text-content
        } else {//earlier versions of android
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            //intent.putExtra("address", getPhoneNoField());
            intent.putExtra("sms_body", message);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        startActivityForResult(Intent.createChooser(intent, "Send Image To:"),SEND_INTENT_ID);
    }
    */

    //https://code.google.com/p/android/issues/detail?id=6151
    //uses explicit intent for ComposeMessageActivity, may not work with older versions of android
    //also may not work when a different mms application is set as default
    private void sendComposeIntent(String message, String phoneNo) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        File file = getFileStreamPath(filename + ".jpg");
        Uri uri = Uri.fromFile(file);
        intent.setClassName("com.android.mms",
                "com.android.mms.ui.ComposeMessageActivity");
        intent.putExtra("address", phoneNo);//telephone number here
        intent.putExtra("sms_body", message);//sms message body here
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("image/jpeg");
        startActivity(intent);
    }

    //http://androidforums.com/threads/send-an-image-action_send.25988/
    private Uri saveImage(Bitmap bm) throws IOException {
        FileOutputStream Os = openFileOutput(filename + ".jpg", Context.MODE_WORLD_READABLE);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, Os);
        Os.close();
        File F = getFileStreamPath(filename + ".jpg");
        Uri U = Uri.fromFile(F);
        return U;
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void pickContact() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        if (pickContactIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            //set displayed image thumbnail and store in shared preferences
            setImageDisplayed(imageBitmap);
            try {
                //save the image to SD card so other apps can access it
                saveImage(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        this.mImageView = (ImageView) findViewById(R.id.mImageView);
        mImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                takePhoto();
            }
        });

        //send button
        final Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = getMessageField(R.id.msgInField);
                String phoneNo = getMessageField(R.id.phoneInField);
                sendComposeIntent(message, phoneNo);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        storeField(R.id.msgInField);
        storeField(R.id.phoneInField);
        //image view is only stored at time of capture
    }

    @Override
    protected void onResume() {
        super.onResume();
        retrieveField(R.id.msgInField);
        retrieveField(R.id.phoneInField);
        retrieveImageDisplayed();
    }

    //https://stackoverflow.com/questions/13562429/how-many-ways-to-convert-bitmap-to-string-and-vice-versa
    public String bitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    /**
     * @param encodedString
     * @return bitmap (from given string)
     */
    public Bitmap stringToBitMap(String encodedString){
        try {
            byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }
}
