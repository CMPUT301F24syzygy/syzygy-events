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
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;

import java.util.List;

public class AdminImagesAdapter extends ArrayAdapter<Image> {

    public AdminImagesAdapter(Context context, List<Image> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        Image img = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_admin_images, parent, false);
        }

        TextView user = view.findViewById(R.id.admin_image_item_user_text);
        user.setText(img.getLocName());

        TextView type = view.findViewById(R.id.admin_image_item_type_text);
        type.setText(img.getFormatedLocType());

        ImageView image = view.findViewById(R.id.admin_image_item_img);
        if (img.getLocType() == Database.Collections.USERS) {
            Image.getFormatedAssociatedImage(img, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(image);
        } else {
            Image.getFormatedAssociatedImage(img, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(image);
        }


        return view;
    }


}