package de.androidcrypto.ntag424sdmfeature;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Button menu1Prepare, menu2PlaintextSun, menu3EncryptedSun, menu4EncryptedFileSun, menu5Unset;
    private Button menu6NdefReader, menu7PlaintextReadCounterLimitSun, menu8EncryptedFileSunCustomKeys;
    private Button menu9TagOverview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        menu1Prepare = findViewById(R.id.btnMenu1Prepare);
        menu2PlaintextSun = findViewById(R.id.btnMenu2PlaintextSun);
        menu3EncryptedSun = findViewById(R.id.btnMenu3EncryptedSun);
        menu4EncryptedFileSun = findViewById(R.id.btnMenu4EncryptedFileSun);
        menu5Unset = findViewById(R.id.btnMenu5Unset);
        menu6NdefReader = findViewById(R.id.btnMenu6NdefReader);
        menu7PlaintextReadCounterLimitSun = findViewById(R.id.btnMenu7PlaintextReadCounterLimitSun);
        menu8EncryptedFileSunCustomKeys = findViewById(R.id.btnMenu8EncryptedFileSunCustomKeys);
        menu9TagOverview = findViewById(R.id.btnMenu9TagInformation);

        menu1Prepare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu1PrepareSun");
                Intent intent = new Intent(MainActivity.this, PrepareActivity.class);
                startActivity(intent);
            }
        });

        menu2PlaintextSun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu2PlaintextSun");
                Intent intent = new Intent(MainActivity.this, PlaintextSunActivity.class);
                startActivity(intent);
            }
        });

        menu3EncryptedSun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu3EncryptedSun");
                Intent intent = new Intent(MainActivity.this, EncryptedSunActivity.class);
                startActivity(intent);
            }
        });

        menu4EncryptedFileSun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu4EncryptedFileSun");
                Intent intent = new Intent(MainActivity.this, EncryptedFileSunActivity.class);
                startActivity(intent);
            }
        });

        menu5Unset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu5UnsetSun");
                Intent intent = new Intent(MainActivity.this, UnsetActivity.class);
                startActivity(intent);
            }
        });

        menu6NdefReader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu6NdefReader");
                Intent intent = new Intent(MainActivity.this, NdefReaderActivity.class);
                startActivity(intent);
            }
        });

        menu7PlaintextReadCounterLimitSun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu7PlaintextReadCounterLimitSun");
                Intent intent = new Intent(MainActivity.this, PlaintextReadCounterLimitSunActivity.class);
                startActivity(intent);
            }
        });

        menu8EncryptedFileSunCustomKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu8EncryptedFileSunCustomKeys");
                Intent intent = new Intent(MainActivity.this, EncryptedFileSunCustomKeysActivity.class);
                startActivity(intent);
            }
        });

        menu9TagOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "menu9TagOverview");
                Intent intent = new Intent(MainActivity.this, TagOverviewActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * section for options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mApplications = menu.findItem(R.id.action_applications);
        mApplications.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                //llApplicationHandling.setVisibility(View.VISIBLE);
                return false;
            }
        });

        MenuItem mStandardFile = menu.findItem(R.id.action_standard_file);
        mStandardFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                //llStandardFile.setVisibility(View.VISIBLE);
                return false;
            }
        });

        MenuItem mExportTextFile = menu.findItem(R.id.action_export_text_file);
        mExportTextFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Log.i(TAG, "mExportTextFile");
                //exportTextFile();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    /*
    public void showDialog(Activity activity, String msg) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.logdata);
        TextView text = dialog.findViewById(R.id.tvLogData);
        //text.setMovementMethod(new ScrollingMovementMethod());
        text.setText(msg);
        Button dialogButton = dialog.findViewById(R.id.btnLogDataOk);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

     */
}