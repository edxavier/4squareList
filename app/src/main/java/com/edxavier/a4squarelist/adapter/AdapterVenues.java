package com.edxavier.a4squarelist.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.edxavier.a4squarelist.R;
import com.edxavier.a4squarelist.api.apiModel.Item_;
import com.edxavier.a4squarelist.api.apiModel.Venue;

import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Created by Eder Xavier Rojas on 12/07/2016.
 */
public class AdapterVenues extends RecyclerView.Adapter<AdapterVenues.ViewHolder> {
    List<Item_> items = null;

    public AdapterVenues(List<Item_> items) {
        this.items = items;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //declarar los Widgets
        @Bind(R.id.name)
        TextView name;
        @Bind(R.id.category)
        TextView category;
        @Bind(R.id.status)
        TextView status;
        @Bind(R.id.address)
        TextView address;
        @Bind(R.id.rating)
        ImageView rating;
        @Bind(R.id.price)
        TextView price;
        @Bind(R.id.distance)
        TextView distance;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_venues, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Venue venue = items.get(position).getVenue();
        holder.name.setText(venue.getName());
        try {
            holder.address.setText(venue.getLocation().getFormattedAddress().get(0));
        }catch (Exception e){
            holder.address.setText(venue.getLocation().getAddress());
        }

        if(venue.getLocation().getDistance()>1000) {
            double kms = (venue.getLocation().getDistance()/ 1000d);
            holder.distance.setText(String.format(Locale.getDefault(),"%.2f km", kms));
        }else {
            holder.distance.setText(String.valueOf(venue.getLocation().getDistance())+" m");
        }

        try {
            holder.category.setText(venue.getCategories().get(0).getName());
        }catch (Exception e) {
            holder.category.setText("----");
        }
        try{
            if(venue.getHours().getStatus()!=null && venue.getHours().getStatus().length()>0) {
                holder.status.setText(venue.getHours().getStatus());
            }else if(venue.getHours().getIsOpen()){
                holder.status.setText("Abierto");
            }else {
                holder.status.setText("Cerrado");
            }
        }catch (Exception e) {
            holder.status.setText("-----");
        }
        if(venue.getRating()!=null){
            holder.rating.setImageDrawable(getAvatar(venue.getRating().toString(), venue.getRatingColor()));
        }else {
            holder.rating.setImageDrawable(getAvatar("0", "000000"));
        }
    }

    public static TextDrawable getAvatar(String rating, String color_str){
        int color = Color.parseColor("#"+color_str);
        return TextDrawable.builder()
                .buildRound(rating, color);
    }
    /**
     * Showing popup menu when tapping on 3 dots
     */

    @Override
    public int getItemCount() {
        if (items != null) {
            return items.size();
        } else {
            return 0;
        }
    }

    public void removeAll() {
        items.clear();
        notifyDataSetChanged();
    }
}
