package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.nearby.NearbyActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

public class ContributionsListFragment extends Fragment {

    public interface SourceRefresher {
        void refreshSource();
    }

    @BindView(R.id.contributionsList) GridView contributionsList;
    @BindView(R.id.waitingMessage) TextView waitingMessage;
    @BindView(R.id.emptyMessage) TextView emptyMessage;

    private ContributionController controller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contributions, container, false);
        ButterKnife.bind(this, v);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener)getActivity());
        if(savedInstanceState != null) {
            Timber.d("Scrolling to %d", savedInstanceState.getInt("grid-position"));
            contributionsList.setSelection(savedInstanceState.getInt("grid-position"));
        }

        //TODO: Should this be in onResume?
        SharedPreferences prefs = this.getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Timber.d("Last Sync Timestamp: %s", lastModified);

        if (lastModified.equals("")) {
            waitingMessage.setVisibility(View.VISIBLE);
        } else {
            waitingMessage.setVisibility(View.GONE);
        }

        return v;
    }

    public ListAdapter getAdapter() {
        return contributionsList.getAdapter();
    }

    public void setAdapter(ListAdapter adapter) {
        this.contributionsList.setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState == null) {
            outState = new Bundle();
        }
        super.onSaveInstanceState(outState);
        controller.saveState(outState);
        outState.putInt("grid-position", contributionsList.getFirstVisiblePosition());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //FIXME: must get the file data for Google Photos when receive the intent answer, in the onActivityResult method
        super.onActivityResult(requestCode, resultCode, data);

        if ( resultCode == RESULT_OK ) {
            Timber.d("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
            controller.handleImagePicked(requestCode, data);
        } else {
            Timber.e("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_from_gallery:
                //Gallery crashes before reach ShareActivity screen so must implement permissions check here
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        //See http://stackoverflow.com/questions/33169455/onrequestpermissionsresult-not-being-called-in-dialog-fragment
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        return true;
                    } else {
                        controller.startGalleryPick();
                        return true;
                    }
                }
                else {
                    controller.startGalleryPick();
                    return true;
                }
            case R.id.menu_from_camera:
                controller.startCameraCapture();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            // 1 = Storage allowed when gallery selected
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Call controller.startGalleryPick()");
                    controller.startGalleryPick();
                }
            }
            break;
            // 2 = Location allowed when 'nearby places' selected
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permission granted");
                    Intent nearbyIntent = new Intent(getActivity(), NearbyActivity.class);
                    startActivity(nearbyIntent);
                }
            }
            break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // See http://stackoverflow.com/a/8495697/17865
        inflater.inflate(R.menu.fragment_contributions_list, menu);

        if (!CommonsApplication.getInstance().deviceHasCamera()) {
            menu.findItem(R.id.menu_from_camera).setEnabled(false);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new ContributionController(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        controller.loadState(savedInstanceState);
    }

    protected void clearSyncMessage() {
        waitingMessage.setVisibility(View.GONE);
    }
}
