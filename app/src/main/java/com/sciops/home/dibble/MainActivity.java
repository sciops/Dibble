package com.sciops.home.dibble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
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
    TextView textView1 =null;
    EditText editText = null;
    final String filepath = "content://media/external/images/media/dibble-photo";
    final Uri mmsUri = Uri.parse(filepath);

    OnEditorActionListener listener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //save the text field
                Editable e = editText.getText();
                storeMessageField(String.valueOf(e));
                handled = true;
            }
            return handled;
        }
    };

    public void setPhoneNoField(String phoneNo) {
        textView1 = (TextView) findViewById(R.id.phoneNoTV);
        textView1.setText(phoneNo);
    }

    public String getPhoneNoField() {
        textView1 = (TextView) findViewById(R.id.phoneNoTV);
        return (String) textView1.getText();
    }

    public void setMessageField(String message) {
        editText = (EditText) findViewById(R.id.messageInputField);
        editText.setText(message);
    }

    public String getMessageField() {
        editText = (EditText) findViewById(R.id.messageInputField);
        return String.valueOf(editText.getText());
    }

    public void setImageDisplayed(Uri uri) {
        mImageView = (ImageView) findViewById(R.id.mImageView);
        //TODO: place bitmap stored in mmsUri into mImageView
    }

    public void storeMessageField(String message) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("messageToSend",message);
        editor.commit();
    }

    public void storePhoneNoField(String phoneNo) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("phoneNo",phoneNo);
        editor.commit();
    }

    public String retrieveStoredMessage() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString("messageToSend","MSG NOT SET");
    }

    public String retrieveStoredPhoneNo() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString("phoneNo","NUM NOT SET");
    }

    //https://stackoverflow.com/questions/26375969/action-send-intent-with-image-doesnt-update-if-draft-not-delete-android-sms-ha
    public static Bitmap getBitmapFromView(View view, int width, int height) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        view.setBackgroundColor(Color.WHITE);

        // creates immutable clone
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        view.draw(canvas);

        view.setDrawingCacheEnabled(false); // clear drawing cache
        return bmp;
    }

    //https://stackoverflow.com/questions/26375969/action-send-intent-with-image-doesnt-update-if-draft-not-delete-android-sms-ha
    //same problem. "Can't find photo"
    public void composeMmsMessage() {

        Bitmap icon = getBitmapFromView(mImageView, mImageView.getWidth(), mImageView.getHeight());
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");//
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        //String path1 = Environment.getExternalStorageDirectory() + File.separator + "lolhistory_sc.jpg";
        //File f = new File(path1);
        File f = new File(filepath);
        try {
            if (f.exists()) {
                // delete if exists
                f.delete();
            }
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes.toByteArray());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String path2 = "file://" + f.getAbsolutePath();
        //Log.d("", path1);
        Log.d("", path2);
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(path2));
        startActivity(Intent.createChooser(share, "Share Image"));

    }

    /*
//crashes.
//https://cells-source.cs.columbia.edu/plugins/gitiles/platform/packages/apps/Mms/+/android-4.3_r2.1/src/com/android/mms/ui/ComposeMessageActivity.java
    public void composeMmsMessage() {
        Activity activity = this;
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) // Android 4.4 and up
        {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
            intent.putExtra("sms_body", getMessageField());
            intent.putExtra("address", getPhoneNoField());
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filepath));
            intent.setType("image/png");

            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(activity);

            if (defaultSmsPackageName != null) // Can be null in case that there is no default, then the user would be able to choose any app that supports this intent.
            {
                intent.setPackage(defaultSmsPackageName);
            }
        } else {//earlier versions of android
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.putExtra("address", getPhoneNoField());
            intent.putExtra("sms_body", getMessageField());

            intent.putExtra(Intent.EXTRA_STREAM, mmsUri);
        }
        startActivity(intent);
    }
    */

    /*
    //https://stackoverflow.com/questions/20079047/android-kitkat-4-4-hangouts-cannot-handle-sending-sms-intent
    //this works without attaching picture.
    public void composeMmsMessage() {
        Activity activity = this;
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) // Android 4.4 and up
        {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(activity);
            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(getPhoneNoField())));
            intent.putExtra("sms_body", getMessageField());
            intent.putExtra(Intent.EXTRA_STREAM, mmsUri);//ignored by intent?

            if (defaultSmsPackageName != null) // Can be null in case that there is no default, then the user would be able to choose any app that supports this intent.
            {
                intent.setPackage(defaultSmsPackageName);
            }
        } else {//earlier versions of android
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.putExtra("address", getPhoneNoField());
            intent.putExtra("sms_body", getMessageField());

            intent.putExtra(Intent.EXTRA_STREAM, mmsUri);
        }
        startActivity(intent);
    }
    */

/*
    public void composeMmsMessage() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        editText = (EditText) findViewById(R.id.messageInputField);
        intent.putExtra("sms_body", String.valueOf(editText.getText()));
        intent.putExtra(Intent.EXTRA_STREAM, mmsUri);
        intent.setType("image/png");
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivity(intent);
    }
*/
    /*
    //https://stackoverflow.com/questions/10833697/send-mms-without-user-interaction-in-android
    public void composeMmsMessage() {
        textView1 = (TextView) findViewById(R.id.phoneNoTV);
        String phoneNo = (String) textView1.getText();
        editText = (EditText) findViewById(R.id.messageInputField);
        String message = String.valueOf(editText.getText());
        Uri attachment = this.uri;
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        //String dataParam = "mmsto:"+phoneNo;
        //intent.setData(Uri.parse(dataParam));  // This ensures only SMS apps respond
        intent.putExtra("sms_body", message);
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        intent.setType("image/png");
        Log.i("composeMmsMessage","begin if statement");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    */

    /*
    //https://stackoverflow.com/questions/8295773/how-can-i-transform-a-bitmap-into-a-uri
    public Uri getImageUri(Context inContext, Bitmap inImage) throws IOException {
        String filename = "dibble-photo-"+getCurrentTimeStamp();
        File file = new File(getExternalFilesDir(null),filename);
        return Uri.fromFile(file);
    }
    */

    /*
    // https://stackoverflow.com/questions/1459656/how-to-get-the-current-time-in-yyyy-mm-dd-hhmisec-millisecond-format-in-java
    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
    */

    /* http://www.codota.com/android/methods/android.content.Context/getExternalFilesDir */
    private Uri saveImage(Bitmap finalBitmap) {
        //String filename = "dibble-photo-"+getCurrentTimeStamp()+".png";
        //File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/Camera/"+filename);
        File file = new File(filepath);
        if (file.exists ()) file.delete ();//overwrites existing file
        try {
            FileOutputStream out = new FileOutputStream(file);
            //finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            //out.flush();
            if (out != null)
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
            mImageView.setImageBitmap(imageBitmap);
            saveImage(imageBitmap);
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
                String phoneNo = cursor.getString(column);

                // Do something with the phone number...
                setPhoneNoField(phoneNo);
                storePhoneNoField(phoneNo);
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
        });

        this.mImageView = (ImageView) findViewById(R.id.mImageView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        storePhoneNoField(getPhoneNoField());
        storeMessageField(getMessageField());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPhoneNoField(retrieveStoredPhoneNo());
        setMessageField(retrieveStoredMessage());
        setImageDisplayed(mmsUri);

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
}
