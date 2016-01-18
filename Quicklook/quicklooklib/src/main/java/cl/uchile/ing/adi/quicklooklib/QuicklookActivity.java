package cl.uchile.ing.adi.quicklooklib;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import cl.uchile.ing.adi.quicklooklib.fragments.AbstractFragment;
import cl.uchile.ing.adi.quicklooklib.fragments.items.AbstractItem;
import cl.uchile.ing.adi.quicklooklib.fragments.FolderFragment;
import cl.uchile.ing.adi.quicklooklib.fragments.items.ItemFactory;
import cl.uchile.ing.adi.quicklooklib.fragments.items.VirtualItem;

public class QuicklookActivity extends AppCompatActivity implements FolderFragment.OnListFragmentInteractionListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private String path;
    private static String TAG = "QuickLookPermissions";
    private Runnable r;
    private View coordinator;
    private static int WRITE_PERMISSIONS = 155;
    private AbstractFragment current;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quicklook);
        coordinator = findViewById(R.id.quicklook_coordinator);
        this.path = getIntent().getStringExtra("localurl");

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AbstractItem item = current.getItem();
                Uri pathUri = Uri.parse("file://" + item.getPath());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pathUri, item.getType());
                Log.d("FAB", pathUri.getPath());
                startActivity(Intent.createChooser(intent, "Open"));
            }
        });
        if (savedInstanceState==null) {
            String name = AbstractItem.getNameFromPath(this.path);
            long size = AbstractItem.getSizeFromPath(this.path);
            String type = AbstractItem.loadMimeType(this.path);
            AbstractItem item = ItemFactory.getInstance().createItem(this.path, type,name,size);
            checkPermissionsAndChangeFragment(item);
        }
    }


    private void checkPermissionsAndChangeFragment(final AbstractItem item) {
        r = new Runnable(){
            public void run() {
                changeFragment(item,false);
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Contacts permissions have not been granted.
            Log.i(TAG, "Storage permissions has NOT been granted. Requesting permissions.");
            requestStoragePermissions();
        } else {

            // Contact permissions have been granted. Show the contacts fragment.
            Log.i(TAG,
                    "Contact permissions have already been granted. Displaying contact details.");
            r.run();
        }
    }

    /**
     * Manages the transition between the fragments which shows the items.
     * @param item Item to show.
     */
    public void changeFragment(AbstractItem item) {
        changeFragment(item, true);
    }

    /**
     * Manages the transition between the fragments which shows the items.
     * @param item Item to show.
     * @param backstack Adds the previous fragment to backstack.
     */
    public void changeFragment(AbstractItem item, boolean backstack){
        updateActivity(item);
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.quicklook_fragment, current, "QuickLook");
        if (backstack) t.addToBackStack(null);
        checkIfShowingFab(item);
        t.commitAllowingStateLoss();
    }

    /**
     * Method called by fragment when item is clicked on list view.
     * @param item the item which is going to be displayed.
     */
    public void onListFragmentInteraction(AbstractItem item) {
        changeFragment(item);

    }

    /**
     * Manages the text in Action Bar, with current path in filesystem.
     * @param item the item which is going to be displayed
     */
    public void onListFragmentCreation(AbstractItem item) {
        updateActivity(item);
    }

    /**
     * Manages the extraction of elements in compressed files.
     * Also shows them after extraction.
     * @param item the item which is going to be displayed.
     */
    public void onListFragmentExtraction(final VirtualItem item) {
        AbstractItem extracted = item.retrieve(getApplicationContext());
        changeFragment(extracted);
    }

    /**
     * Updates... the action bar!
     * @param item Item with new info for the actionbar.
     */
    private void updateActivity(AbstractItem item) {
        setFragment(item.getFragment());
        getSupportActionBar().setTitle(item.getTitle());
        getSupportActionBar().setSubtitle(item.getSubTitle());
        checkIfShowingFab(item);

    }

    private void requestStoragePermissions() {

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == WRITE_PERMISSIONS) {
            // Received permission result for storage permission.
            Log.i(TAG, "Received response for storage permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // storage permission has been granted, preview can be displayed
                Log.i(TAG, "STORAGE permission has now been granted. Showing preview.");
                r.run();
            } else {
                Log.i(TAG, "STORAGE permission was NOT granted.");
                Snackbar.make(coordinator, "Permission error. Can't show files.",
                        Snackbar.LENGTH_SHORT).show();

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void checkIfShowingFab(AbstractItem item) {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        if (item.isFolder()) {
            p.setAnchorId(View.NO_ID);
            fab.setLayoutParams(p);
            fab.setVisibility(View.GONE);
        } else {
            p.anchorGravity = Gravity.BOTTOM | Gravity.END;
            p.setAnchorId(R.id.quicklook_coordinator);
            fab.setLayoutParams(p);
            fab.setVisibility(View.VISIBLE);
        }
    }

    public AbstractFragment getFragment() {
        return current;
    }

    public void setFragment(AbstractFragment fragment) {
        current = fragment;
    }

}
