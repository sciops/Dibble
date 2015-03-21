package com.sciops.home.dibble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
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
        String msg = BitMapToString(bitmap);
        storeString("photoDibble",msg);
    }

    private void retrieveImageDisplayed() {
        mImageView = (ImageView) findViewById(R.id.mImageView);
        String msg = retrieveString("photoDibble");
        Bitmap bitmap = StringToBitMap(msg);
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
        startActivityForResult(Intent.createChooser(intent, "Send Image To:"),564561);

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

        editText = (EditText) findViewById(R.id.msgInField3);

        this.mImageView = (ImageView) findViewById(R.id.mImageView);
        mImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                takePhoto();
            }
        });

        //send button 1
        final Button sendButton1 = (Button) findViewById(R.id.sendButton1);
        sendButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editText = (EditText) findViewById(R.id.msgInField1);
                sendComposeIntent(getMessageField(editText.getId()));
            }
        });

        //send button 2
        final Button sendButton2 = (Button) findViewById(R.id.sendButton2);
        sendButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editText = (EditText) findViewById(R.id.msgInField2);
                sendComposeIntent(getMessageField(editText.getId()));
            }
        });

        //send button 3
        final Button sendButton3 = (Button) findViewById(R.id.sendButton3);
        sendButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editText = (EditText) findViewById(R.id.msgInField3);
                sendComposeIntent(getMessageField(editText.getId()));
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();
        //storePhoneNoField(getPhoneNoField());
        editText = (EditText) findViewById(R.id.msgInField1);
        storeField(editText.getId());
        editText = (EditText) findViewById(R.id.msgInField2);
        storeField(editText.getId());
        editText = (EditText) findViewById(R.id.msgInField3);
        storeField(editText.getId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setPhoneNoField(retrieveStoredPhoneNo());
        editText = (EditText) findViewById(R.id.msgInField1);
        retrieveField(editText.getId());
        editText = (EditText) findViewById(R.id.msgInField2);
        retrieveField(editText.getId());
        editText = (EditText) findViewById(R.id.msgInField3);
        retrieveField(editText.getId());


    }


    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    //https://stackoverflow.com/questions/13562429/how-many-ways-to-convert-bitmap-to-string-and-vice-versa
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    /**
     * @param encodedString
     * @return bitmap (from given string)
     */
    public Bitmap StringToBitMap(String encodedString){
        try {
            byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }
}
