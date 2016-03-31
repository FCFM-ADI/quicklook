package cl.uchile.ing.adi.quicklooklib;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FileUtils;

import java.io.File;

import cl.uchile.ing.adi.quicklooklib.fragments.ListFragment;
import cl.uchile.ing.adi.quicklooklib.fragments.QuicklookFragment;
import cl.uchile.ing.adi.quicklooklib.items.BaseItem;
import cl.uchile.ing.adi.quicklooklib.items.FileItem;
import cl.uchile.ing.adi.quicklooklib.items.IListItem;
import cl.uchile.ing.adi.quicklooklib.items.VirtualItem;

public class QuicklookActivity extends AppCompatActivity implements ListFragment.OnListFragmentInteractionListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private String path;
    private Runnable r;
    private View coordinator;
    private QuicklookFragment current;
    ProgressDialog pd;
    AsyncTask loadingTask;

    private static String TAG = "QuickLookPermissions";
    private static int WRITE_PERMISSIONS = 155;


    // Activity Config.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set context to items (localization)
        BaseItem.setContext(getApplicationContext());
        setContentView(R.layout.activity_quicklook);
        coordinator = findViewById(R.id.quicklook_coordinator);
        //Only if fragment is not rendered
        if (savedInstanceState==null) {
            onNewIntent(getIntent());
        }
        //Action bar back button
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Set url of start item
        if (pd!=null) pd.dismiss();
        boolean backstack = false;
        this.path = intent.getStringExtra("localurl");
        generateFolders();
        if (getIntent()!=intent) {
            backstack=true;
        }
        setIntent(intent);
        long size = BaseItem.getSizeFromPath(this.path);
        String type = FileItem.loadFileType(new File(this.path));
        Bundle extra;
        if (getIntent().hasExtra(BaseItem.ITEM_EXTRA)) {
            extra = getIntent().getBundleExtra(BaseItem.ITEM_EXTRA);
        } else {
            extra = new Bundle();
        }
        BaseItem item = ItemFactory.getInstance().createItem(this.path, type, size, extra);
        checkPermissionsAndChangeFragment(item, backstack);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invalidateOptionsMenu();
    }

    private void generateFolders() {
        //Generate download folder
        if (BaseItem.getDownloadPath() == null) {
            BaseItem.setDownloadPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .getAbsolutePath()+"/Quicklook/");
        }
        String downloadPath = BaseItem.getDownloadPath();
        //Create downloadPath folder if not exists.
        File downloadFolder = new File(downloadPath);
        if (!downloadFolder.exists()) downloadFolder.mkdirs();
        BaseItem.setDownloadPath(downloadPath);

        //Generate cache folder
        if (BaseItem.getCachePath()==null) {
            BaseItem.setCachePath(getFilesDir().getAbsolutePath() + "/quicklook/");
        }
        //Create cachePath folder if not exists.
        String cachePath=BaseItem.getCachePath();
        File cacheFolder = new File(cachePath);
        if (!cacheFolder.exists()) cacheFolder.mkdirs();
        BaseItem.setCachePath(cachePath);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mItem) {
        // handle item selection
        int i = mItem.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        }
        else if (i == R.id.save) {
            saveItem();
            return true;
        } else if (i == R.id.share) {
            shareItem();
            return true;
        } else if (i == R.id.open_with) {
            openItem();
            return true;
        } else {
            return super.onOptionsItemSelected(mItem);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if( this.current == null ) return true;

        BaseItem item = this.current.getItem();
        if( item instanceof IListItem ) return true;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_menu, menu);
        if( ! item.isOpenable() ) menu.findItem(R.id.open_with).setVisible(false);

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String innerPath = BaseItem.getCachePath();
        File f = new File(innerPath);
        try {
            FileUtils.deleteDirectory(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == WRITE_PERMISSIONS) {
            // Received permission result for storage permission.

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // storage permission has been granted, preview can be displayed
                r.run();
            } else {
                Snackbar.make(coordinator, getResources().getString(R.string.quicklook_permission_error),
                        Snackbar.LENGTH_SHORT).show();

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Fragment management.

    /**
     * Checks if storage permissions exist
     * @param item Item to render after checking permissions.
     */
    private void checkPermissionsAndChangeFragment(final BaseItem item, final boolean addToBackstack) {
        r = new Runnable(){
            public void run() {
                changeFragment(item,addToBackstack);
                invalidateOptionsMenu();
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_PERMISSIONS);
        } else {
            r.run();
        }
    }

    /**
     * Manages the transition between the fragments which shows the items.
     * @param item Item to show.
     */
    public void changeFragment(BaseItem item) {
        changeFragment(item, true);
    }

    /**
     * Manages the transition between the fragments which shows the items.
     * @param item Item to show.
     * @param backstack if true, adds the previous fragment to backstack.
     */
    public void changeFragment(BaseItem item, boolean backstack){
        if (item!=null) {
            setFragment(item.getFragment());
            FragmentTransaction t = getSupportFragmentManager().beginTransaction();
            t.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            t.replace(R.id.quicklook_fragment, current, "QuickLook");
            if (backstack) {
                t.addToBackStack(null);
            }
            t.commitAllowingStateLoss();
            updateActionBar();
        }
    }


    /**
     * Updates... the action bar!
     */
    public void updateActionBar() {
        getSupportActionBar().setTitle(this.getItem().getTitle());
        getSupportActionBar().setSubtitle(this.getItem().getSubTitle());

    }

    //Listeners

    /**
     * Method called by fragment when item is clicked on list view.
     * @param item the item which is going to be displayed.
     */

    public BaseItem onListFragmentInteraction(BaseItem item) {
        return item;
    }

    /**
     * Shows a snack bar with information.
     * @param info
     */
    public void showInfo(String info) {
        Snackbar.make(coordinator, info,
                Snackbar.LENGTH_LONG).show();
    }

    /**
     * Action when item is retrieved...
     * @param toRetrieve the item which is going to be displayed.
     * @param container item which contains toRetrieve.
     */
    public BaseItem retrieveElement(BaseItem toRetrieve, VirtualItem container) {
        return  container.retrieve(toRetrieve, getApplicationContext());
    }

   // Getters/Setters

    public QuicklookFragment getFragment() {
        return current;
    }
    
    public BaseItem getItem() {
        return current.getItem();
    }

    public void setFragment(QuicklookFragment fragment) {
        current = fragment;
    }

    // Button item functions

    public Uri saveItem(boolean inform) {
        BaseItem item = current.getItem();
        String mime = item.getMime();
        String newPath = item.copyItem(mime);
        Uri pathUri = Uri.parse("file://" + newPath);
        if (inform) showInfo(String.format(getResources().getString(R.string.info_document_saved), BaseItem.getDownloadPath()));
        return pathUri;
    }

    public Uri saveItem() {
        return saveItem(true);
    }

    public void openItem() {
        BaseItem item = current.getItem();
        if( ! item.isOpenable() ) {
            // TO-DO toast ?
            return;
        }
        Uri pathUri = saveItem(false);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mime = item.getMime();
        intent.setDataAndType(pathUri, mime);
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.quicklook_open)));
    }


    public void shareItem() {
        Uri pathUri = saveItem(false);
        Intent intent = new Intent(Intent.ACTION_SEND);
        File f = new File(pathUri.getPath());
        if (f.exists()) {
            BaseItem item = current.getItem();
            intent.setType(item.getMime());
            intent.putExtra(Intent.EXTRA_STREAM, pathUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.item_share_title));
            intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.item_share_text));
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.quicklook_share)));
        } else {

        }
    }

    //Helper (Static) Functions

    /**
     * Registers a type to open.
     * @param className
     * @param types
     */
    public static void registerType(Class className, String... types) {
        for (String type:types) {
            ItemFactory.getInstance().register(className, type);
        }
    }

    /**
     * Sets the download path.
     * @param path
     */
    public static void setDownloadPath(String path) {
        BaseItem.setDownloadPath(path);
    }

    public void removeFromBackStack(QuicklookFragment frag) {
        FragmentManager f = getSupportFragmentManager();
        f.popBackStack();
    }

    @Override
    public void makeTransition(final BaseItem mItem) {
        final IListItem originalItem = (IListItem) (getItem());
        if (!areTasksRunning()) {
            loadingTask = new AsyncTask<Object, Object, BaseItem>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    pd = ProgressDialog.show(QuicklookActivity.this,
                            getString(R.string.quicklook_loading_file),
                            mItem.getTitle());
                }

                @Override
                protected BaseItem doInBackground(Object... params) {
                    return originalItem.onClick(QuicklookActivity.this, mItem);
                }

                @Override
                protected void onPostExecute(BaseItem result) {
                    super.onPostExecute(result);
                    if (result!=null) {
                        changeFragment(result);
                        pd.dismiss();
                    }
                }
            };
            loadingTask.execute();
        }
    }

    public boolean areTasksRunning() {
        return (loadingTask != null && loadingTask.getStatus() == AsyncTask.Status.RUNNING);
    }

    /** .
     * Opens item with default fragment if anything goes wrong
     * @param item
     */
    public void fragmentFallback(BaseItem item) {

    }
}
