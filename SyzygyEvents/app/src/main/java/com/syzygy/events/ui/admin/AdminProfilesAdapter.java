package com.syzygy.events.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.syzygy.events.R;
import com.syzygy.events.database.User;
import com.syzygy.events.database.Image;

import java.util.List;
import java.util.Locale;

public class AdminProfilesAdapter extends ArrayAdapter<User> {

    public AdminProfilesAdapter(Context context, List<User> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        User user = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_admin_profiles, parent, false);
        }

        TextView name = view.findViewById(R.id.user_item_name_text);
        name.setText(user.getName());

        ImageView image = view.findViewById(R.id.user_item_profile_img);
        Image.getFormatedAssociatedImage(user, Image.Options.Circle(Image.Options.Sizes.SMALL)).into(image);


        ///
        ///

        return view;
    }


}