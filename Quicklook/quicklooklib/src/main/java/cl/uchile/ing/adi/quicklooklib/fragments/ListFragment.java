package cl.uchile.ing.adi.quicklooklib.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cl.uchile.ing.adi.quicklooklib.R;
import cl.uchile.ing.adi.quicklooklib.fragments.items.ListItem;

/**
 * Opens folders and lists the items inside them. There are extensions of this
 * class for showing elements inside compressed files.
 */
public class ListFragment extends QuicklookFragment {

    public ListFragment() {
    }

    /**
     * Defines the list view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fileitem_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(((ListItem)(this.item)).getAdapter(mListener));
        }
        return view;
    }
}