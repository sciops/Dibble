package com.sciops.home.dibble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.widget.TextView.OnEditorActionListener;


public class MainActivity extends Activity {
    static final int PICK_CONTACT_REQUEST = 792415;  // The request code
    public final static String EXTRA_MESSAGE = "com.sciops.home.dibble.MESSAGE";
    static final int REQUEST_IMAGE_CAPTURE = 792416;
    ImageView mImageView = null;
    TextView textView1 =null;
    EditText editText = null;
    Uri uri = null;

    OnEditorActionListener listener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //save the text field
                Editable e = editText.getText();
                saveTextField(String.valueOf(e));
                handled = true;
            }
            return handled;
        }
    };

    public void saveTextField(String message) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("messageToSend",message);
        editor.commit();
    }

    public String retreiveTextField() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String message = sharedPref.getString("messageToSend","UNK ERR");
        return message;
    }

    //https://stackoverflow.com/questions/10833697/send-mms-without-user-interaction-in-android
    public void composeMmsMessage() {
        textView1 = (TextView) findViewById(R.id.phoneNoTV);
        String phoneNo = (String) textView1.getText();
        editText = (EditText) findViewById(R.id.messageInputField);
        String message = String.valueOf(editText.getText());
        Uri attachment = this.uri;
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        String dataParam = "mmsto:"+phoneNo;
        intent.setData(Uri.parse(dataParam));  // This ensures only SMS apps respond
        intent.putExtra("sms_body", message);
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        intent.setType("image/png");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /*
    //https://stackoverflow.com/questions/8295773/how-can-i-transform-a-bitmap-into-a-uri
    public Uri getImageUri(Context inContext, Bitmap inImage) throws IOException {
        String filename = "dibble-photo-"+getCurrentTimeStamp();
        File file = new File(getExternalFilesDir(null),filename);
        return Uri.fromFile(file);
    }
    */

    // https://stackoverflow.com/questions/1459656/how-to-get-the-current-time-in-yyyy-mm-dd-hhmisec-millisecond-format-in-java
    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    private File getNewPhotoFile() {
        String filename = "dibble-photo-"+getCurrentTimeStamp()+".jpg";
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/Camera/"+filename);
        return file;
    }

    /* http://www.codota.com/android/methods/android.content.Context/getExternalFilesDir */
    private Uri saveImage(Bitmap finalBitmap) {
        File file = getNewPhotoFile();
        if (file.exists ()) file.delete ();//overwrites existing file
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Uri.fromFile(file);
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void helloMessage(String message) {
        Intent intent = new Intent(this, helloContact.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void pickContact() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        if (pickContactIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();


        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
            String phoneNo = sharedPref.getString("phoneNo","UNK ERR");
            Uri uri = saveImage(imageBitmap);
            this.uri = uri;

        }


        // Check which request it is that we're responding to
        if (requestCode == PICK_CONTACT_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Get the URI that points to the selected contact
                Uri contactUri = data.getData();
                // We only need the NUMBER column, because there will be only one row in the result
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

                // Perform the query on the contact to get the NUMBER column
                // We don't need a selection or sort order (there's only one result for the given URI)
                // CAUTION: The query() method should be called from a separate thread to avoid blocking
                // your app's UI thread. (For simplicity of the sample, this code doesn't do that.)
                // Consider using CursorLoader to perform the query.
                Cursor cursor = getContentResolver()
                        .query(contactUri, projection, null, null, null);
                cursor.moveToFirst();

                // Retrieve the phone number from the NUMBER column
                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(column);

                // Do something with the phone number...

                textView1 = (TextView) findViewById(R.id.phoneNoTV);
                textView1.setText(number);


                editor.putString("phoneNo",number);
                editor.commit();

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.messageInputField);

        //pick contact button
         final Button buttonContact = (Button) findViewById(R.id.buttonPickContact);
        buttonContact.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 // Perform action on click
                 pickContact();
             }
         });

        //take photo button
        final Button buttonPic = (Button) findViewById(R.id.buttonTakePic);
        buttonPic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                takePhoto();
            }
        });

        //send mms button
        final Button mmsButton = (Button) findViewById(R.id.buttonSendMMS);
        mmsButton.setOnClickListener(new View.OnClickListener() {
                                         public void onClick(View v) {
                                             composeMmsMessage();
                                         }
                                     }
        );

        this.mImageView = (ImageView) findViewById(R.id.mImageView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        textView1 = (TextView) findViewById(R.id.phoneNoTV);
        String phoneNo = (String) textView1.getText();
        editor.putString("phoneNo",phoneNo);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String phoneNo = sharedPref.getString("phoneNo","UNK ERR");
        textView1 = (TextView) findViewById(R.id.phoneNoTV);
        textView1.setText(phoneNo);
    }


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
}
