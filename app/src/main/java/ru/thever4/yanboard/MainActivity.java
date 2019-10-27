package ru.thever4.yanboard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {


    private SharedPreferences preferences;
    SharedPreferences.Editor prefEditor;
    private static final String APP_PREFERENCES = "preferences";

    private static Socket client;
    private String IP;
    private int channel = 1;
    private Vibrator vibro;
    private boolean useVibro = true;
    private int octave = 0;


    private Button find_srv;
    private EditText editIP;
    private Button connectButton;
    private Switch switchVibro;

    private PianoView pianoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        String prIP = preferences.getString("IP", null);
        if(prIP != null) {
            this.IP = prIP;
            new AsyncReq().execute(IP);
        }

        if(preferences.contains("octave")) octave = preferences.getInt("octave", 0);
        if(preferences.contains("channel")) channel = preferences.getInt("channel", 1);
        if(preferences.contains("useVibro")) useVibro = preferences.getBoolean("useVibro", true);

        vibro = (Vibrator) getSystemService(VIBRATOR_SERVICE);



        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) { //Horizontal
            pianoView = (PianoView) findViewById(R.id.instrument_view);
            pianoView.setOnKeyTouchListener(new PianoView.OnKeyTouchListener() {
                @Override
                public void onTouch(int midiCode) {
                    sendNote(true, midiCode, channel);
                    if(useVibro) vibro.vibrate(100);
                }

                @Override
                public void onLongTouch(int midiCode) {
                    //sendNote(true, midiCode, channel);
                }

            });
        }
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { //Vertical


            editIP = (EditText) findViewById(R.id.editIP);
            connectButton = (Button) findViewById(R.id.ConnectButton);
            find_srv = (Button) findViewById(R.id.find_srv);
            switchVibro = (Switch) findViewById(R.id.switchVibro);
            Spinner spinOctaves = (Spinner) findViewById(R.id.midiOctaves);
            Spinner spinChannel = (Spinner) findViewById(R.id.midiChannel);

            if(IP == null) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                editIP.setText(IP);
            }

            String[] octaves = {"Субконтроктава + Контроктава", "Большая + Малая", "Первая + Вторая", "Третья + Четвёртая"};
            String[] channels = {"1 (default)", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
            ArrayAdapter<String> octadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, octaves);
            ArrayAdapter<String> chanadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, channels);
            octadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            chanadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinChannel.setAdapter(chanadapter);
            spinChannel.setPrompt("Канал");
            spinChannel.setSelection(channel - 1);
            spinChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    channel = i + 1;
                    prefEditor.putInt("channel", channel);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            spinOctaves.setAdapter(octadapter);
            spinOctaves.setPrompt("Октавы");
            spinOctaves.setSelection((octave/12)/2);
            spinOctaves.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    octave = i * 2 * 12; //12 - octave size and 2 - count of octaves
                    prefEditor.putInt("octave", octave);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            if(preferences.contains("useVibro")) {
                boolean use = preferences.getBoolean("useVibro", true);
                switchVibro.setChecked(use);
            }

            switchVibro.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    useVibro = switchVibro.isChecked();
                    prefEditor.putBoolean("useVibro", useVibro);
                }
            });

            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String eIp = editIP.getText().toString();
                    FindServer findServer = new FindServer(false);
                    if(findServer.checkAddress(eIp)){
                        IP = eIp;
                        new AsyncReq().execute(IP);
                    }
                    else alert("Wrong input", "С указанным IP-адресом что-то не так. Вы уже проверяли WiFi и сервер? Всё введено верно?", "Да");
                }
            });


            find_srv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    find_srv.setTextColor(Color.GRAY);
                    find_srv.setEnabled(false);
                    try {
                        String ip = FindYanServer();
                        if(ip != null) {
                            IP = ip;
                            editIP.setText(IP);
                        }
                        else alert("Fail", "Автопоиск завершился безуспешно\n" +
                                "Убедитесь что вы подключены к одной WiFi сети\n" +
                                "и в том, что сервер запущен без ошибок\n\n" +
                                "Попробуйте снова, если ошибка возобновляется, введите IP вручную", "OK");
                    }
                    catch (ExecutionException e) {}
                    catch (InterruptedException e) {}
                    finally {
                        find_srv.setEnabled(true);
                        find_srv.setTextColor(Color.BLACK);
                    }
                    new AsyncReq().execute(IP);
                }
            });
        }
    }

    private void toast(String text) {
        new Toast(this).makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
    }

    /*private void confirm(String title, String message, String buttonCancel, String buttonOk, DialogInterface.OnClickListener CancelListener, DialogInterface.OnClickListener OkListener) {
        if(title == null || message == null || buttonCancel == null || buttonOk == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title).setMessage(message).setCancelable(false).setNegativeButton(buttonCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).setPositiveButton(buttonOk, OkListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    //feature to the future
    */

    private void alert(String title, String message, String button) {
        if(title == null || message == null || button == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title).setMessage(message).setCancelable(false).setNegativeButton(button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private AsyncTask<String, Void, Void> sendNote(boolean keyState, int keyNum, int channel) {
        int keyNumber = keyNum + octave;
        return new AsyncNote().execute(Boolean.toString(keyState), Integer.toString(keyNumber), Integer.toString(channel));
    }

    @Override
    protected void onPause() {
        prefEditor.putString("IP", IP);
        prefEditor.apply();
        super.onPause();
    }

    @Override
    protected void onStop() {
        try {
            if(client != null) client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    private String FindYanServer() throws ExecutionException, InterruptedException {
        AsyncFinder asyncFinder = new AsyncFinder();
        asyncFinder.execute();
        return asyncFinder.get();
    }

    class AsyncFinder extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            FindServer findServer = new FindServer(true);
            return findServer.getAddress();
        }
    }

    class AsyncNote extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            if(client == null) {
                alert("Error", "Произошла ошибка сетевого уровня. В первую очередь проверьте WiFi" +
                        "\nТак же может помочь новый автопоиск сервера\n\n", "OK");
                return;
            }
            if(!client.isConnected()) {
                if(IP != null)
                new AsyncReq().execute(IP);
            }
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {

            boolean keyState = new Boolean(params[0]);
            int keyNumber = new Integer(params[1]);
            int channelNumber = new Integer(params[2]);

                int status = keyState?144:128;
                status += (channelNumber - 1);
                    try {
                        byte[] arr = {(byte) status, (byte) keyNumber, (byte) 0x7F};
                            client.getOutputStream().write(arr);
                        }
                        catch (Exception e) {
                            Log.e("YABOARD", e.toString());
                        }

            return null;
        }
    }

    class AsyncReq extends AsyncTask<String, Integer, String> {

        boolean success = false;

        @Override
        protected String doInBackground(String... strings) {
            try {
                client = new Socket(strings[0], 55765);
                success = true;
            }
            catch (Exception e) {
                Log.e("YAStart", e.toString());
                success = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if(success) toast("Подключено к " + IP);
            else toast("Не удаётся подключиться");
            super.onPostExecute(s);
        }
    }
}
