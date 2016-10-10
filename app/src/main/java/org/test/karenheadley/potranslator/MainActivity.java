package org.test.karenheadley.potranslator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.projectoxford.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.projectoxford.speechrecognition.RecognitionResult;
import com.microsoft.projectoxford.speechrecognition.RecognitionStatus;
import com.microsoft.projectoxford.speechrecognition.SpeechRecognitionMode;
import com.microsoft.projectoxford.speechrecognition.SpeechRecognitionServiceFactory;

public class MainActivity extends AppCompatActivity
        implements ISpeechRecognitionServerEvents {

    public static final String LOG_TAG = "POTranslator";
    private MicrophoneRecognitionClient mMicClient = null;
    private SpeechRecognitionMode mSpeechMode = SpeechRecognitionMode.ShortPhrase;

    private String mLanguageCode = Constants.LANGUAGE_CODES[0];
    private String mKey = Constants.PRIMARY_SUBSCRIPTION_KEY;

    private TextView mResultText;
    private FloatingActionButton mFab;

    private int onlineIcon;
    private int busyIcon;

    private boolean mHasStartedRecording = false;

    private boolean mHasOptionChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mResultText = (TextView) findViewById(R.id.resultText);

        onlineIcon = getResources().getIdentifier("@android:drawable/presence_audio_online", null, null);
        busyIcon = getResources().getIdentifier("@android:drawable/ic_voice_search", null, null);


        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasInternetConnection()) {
                    mResultText.setText("");
                    initRecording();
                    if (mMicClient != null) {
                        if (mSpeechMode.equals(SpeechRecognitionMode.ShortPhrase)) {
                            if (!mHasStartedRecording) {
                                mMicClient.startMicAndRecognition();
                            }
                        } else {
                            if (!mHasStartedRecording) {
                                mMicClient.startMicAndRecognition();
                            } else {
                                mMicClient.endMicAndRecognition();
                            }
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please check your Internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        initLanguageSpinner();
        initSpeechModeSpinner();
    }

    private boolean hasInternetConnection() {
        ConnectivityManager connectivityManager = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager != null
                && connectivityManager.getActiveNetworkInfo() != null
                && connectivityManager.getActiveNetworkInfo().isConnected();

    }

    private void initLanguageSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.language_spinner);
        spinner.setSaveEnabled(true);
         spinner.setSelection(SharedPreferenceUtils.getBaseLanguageIndex(this));
         mLanguageCode = Constants.LANGUAGE_CODES[SharedPreferenceUtils.getBaseLanguageIndex(this)];

        spinner.post(new Runnable() {
            @Override
            public void run() {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(LOG_TAG, "in Language Spinner onItemSelected");
                        mLanguageCode = Constants.LANGUAGE_CODES[position];
                        mHasOptionChanged = true;
                        SharedPreferenceUtils.updateBaseLanguageIndex(MainActivity.this, position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // no action required, but we must implement this method

                    }
                });
            }
        });
    }

    private void initSpeechModeSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.speech_mode_spinner);
        spinner.setSaveEnabled(true);
         int pref = SharedPreferenceUtils.getSpeechModeIndex(this);
         spinner.setSelection(pref);
         mSpeechMode = pref == 0 ? SpeechRecognitionMode.ShortPhrase : SpeechRecognitionMode.LongDictation;
        spinner.post(new Runnable() {
            @Override
            public void run() {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(LOG_TAG, "In Speech Mode onItemSelected");
                        mSpeechMode = position == 0 ? SpeechRecognitionMode.ShortPhrase : SpeechRecognitionMode.LongDictation;
                        mHasOptionChanged = true;
                        SharedPreferenceUtils.updatedSpeechModeIndex(MainActivity.this, position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // no action required, but we must implement this method

                    }
                });
            }
        });


    }

    private void initRecording() {
        if ((mHasOptionChanged) || (mMicClient == null)) {
            Log.d(LOG_TAG, "Language is " + mLanguageCode + "\nSpeech mode is " + mSpeechMode);
            if(mKey.equals(Constants.PRIMARY_SUBSCRIPTION_KEY)) {
                mResultText.append("Connecting with PRIMARY key \n");
            } else {
                mResultText.append("Connecting with SECONDARY key\n");
            }
            mMicClient = SpeechRecognitionServiceFactory.createMicrophoneClient(this, mSpeechMode, mLanguageCode, this, mKey);
            mHasOptionChanged = false;
        }
    }

    @Override
    public void onPartialResponseReceived(String response) {
        mResultText.append("PARTIAL RESULT:\n");
        mResultText.append(response + "\n");
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        // explanation of results at https://msdn.microsoft.com/en-us/library/mt613453.aspx
        boolean isFinalDictationMessage = (
                mSpeechMode == SpeechRecognitionMode.LongDictation
                        && (recognitionResult.RecognitionStatus == RecognitionStatus.EndOfDictation
                        || recognitionResult.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout
                        || recognitionResult.RecognitionStatus == RecognitionStatus.RecognitionSuccess));

        if(mSpeechMode == SpeechRecognitionMode.ShortPhrase || isFinalDictationMessage) {
            if (mMicClient != null) {
                mMicClient.endMicAndRecognition();
            }

            mFab.setEnabled(true);
            mFab.setImageResource(onlineIcon);
        }

        String s = "";
        if(isFinalDictationMessage) {
            s += "Final Dictation Message\n";
        }

        if(recognitionResult.Results.length >0) {
            s += "Text Suggestions\n";
            for(int i=0; i< recognitionResult.Results.length; i++) {
                s += (i+1) + " " + recognitionResult.Results[i].DisplayText + "\n";
            }
            s += "\n" + mResultText.getText().toString();
            mResultText.setText(s);
        }
    }

    @Override
    public void onIntentReceived(String response) {
        // We're not using speech recognition with intent, but
        // we must implement all the interface methods.
    }

    @Override
    public void onError(int errorCode, String response) {
        mFab.setEnabled(true);
        mFab.setImageResource(onlineIcon);
        Toast.makeText(this, "Cannot connect to server!\nError has occured!", Toast.LENGTH_LONG).show();
        mResultText.append("Error " + errorCode + ": " + response + "\n");
        mMicClient = null; // Force an initialization when recording next time
        mKey = Constants.SECONDARY_SUBSCRIPTION_KEY;
    }

    @Override
    public void onAudioEvent(boolean isRecording) {
        mHasStartedRecording = isRecording;
        if(!isRecording) {
            if(mMicClient != null) {
                mMicClient.endMicAndRecognition();
            }
            mFab.setEnabled(true);
            mFab.setImageResource(onlineIcon);
        } else {
            if(mSpeechMode == SpeechRecognitionMode.ShortPhrase) {
                mFab.setEnabled(false);
            }
            mFab.setImageResource(busyIcon);
        }
        mResultText.append(isRecording ? "RECORDING STARTED\n" : "RECORDING ENDED\n");

    }

    // prevent lose of translated text if orientation changes:
    // if orientation is changed while recording, recording stops

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mResultText", mResultText.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mResultText.setText(savedInstanceState.getString("mResultText"));
    }

    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
