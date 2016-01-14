package cl.uchile.ing.adi.quicklooklib.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import cl.uchile.ing.adi.quicklooklib.R;
import cl.uchile.ing.adi.quicklooklib.fragments.AbstractFragment.OnListFragmentInteractionListener;
import cl.uchile.ing.adi.quicklooklib.fragments.items.AbstractItem;
import cl.uchile.ing.adi.quicklooklib.fragments.items.DefaultItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DefaultItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class FolderRecyclerViewAdapter extends RecyclerView.Adapter<FolderRecyclerViewAdapter.ViewHolder> {

    protected final List<AbstractItem> mValues;
    protected final OnListFragmentInteractionListener mListener;

    public FolderRecyclerViewAdapter(List<AbstractItem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_fileitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mTextView.setText(mValues.get(position).getName());
        holder.mSubTextView.setText(mValues.get(position).getType()+ " - " +
                mValues.get(position).getFormattedSize());
        holder.mImageView.setImageResource(mValues.get(position).getImage());
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    clickAction(holder);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTextView;
        public final TextView mSubTextView;
        public final ImageView mImageView;
        public AbstractItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTextView = (TextView) view.findViewById(R.id.item_text);
            mSubTextView = (TextView) view.findViewById(R.id.item_subtext);
            mImageView = (ImageView) view.findViewById(R.id.item_image);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }
    }

    public void clickAction(ViewHolder holder) {
        mListener.onListFragmentInteraction(holder.mItem);
    }

}