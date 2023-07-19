package com.samsung.itschool.firereconize;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class RecyclerAdaptor extends RecyclerView.Adapter<RecyclerAdaptor.ViewHolder> {

    private ArrayList<String> list;

    public RecyclerAdaptor(ArrayList list) {
        this.list = list;
    }

    @NonNull
    @Override
    public RecyclerAdaptor.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        LayoutInflater inflater=LayoutInflater.from(parent.getContext());
        View view =inflater.inflate(R.layout.custom_item_layout,parent,false);
        view.setOnClickListener(new View.OnClickListener() {
            //Отображении картинки по клику в списке
            @Override
            public void onClick(View view) {
                int itemPosition =(int) ((ConstraintLayout) view).getChildAt(0).getTag();
                Uri uri = FileProvider.getUriForFile(view.getContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        new File(list.get(itemPosition)));
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                view.getContext().startActivity(intent);
            }
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdaptor.ViewHolder holder, int position) {
        holder.imageView.setTag(position);
        holder.imageView.setImageURI(Uri.fromFile(new File(list.get(position))));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView=itemView.findViewById(R.id.imageView);
        }
    }
}
