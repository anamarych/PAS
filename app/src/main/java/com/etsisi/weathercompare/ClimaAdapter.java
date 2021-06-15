package com.etsisi.weathercompare;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.etsisi.weathercompare.db.AppDataBase;
import com.etsisi.weathercompare.db.ClimaDao;
import com.etsisi.weathercompare.db.ClimaEntity;

import org.w3c.dom.Text;

import java.util.List;

public class ClimaAdapter extends RecyclerView.Adapter<ClimaAdapter.MyViewHolder> {

    private Context context;
    private List<ClimaEntity> climaEntityList;

    public ClimaAdapter(Context context){
        this.context = context;
    }

    public void setClimaEntityList(List<ClimaEntity> climaEntityList){
        this.climaEntityList = climaEntityList;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ClimaAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_row, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClimaAdapter.MyViewHolder holder, int position) {
        holder.tvCityName.setText(this.climaEntityList.get(position).city_name);
        holder.tvDescription.setText(this.climaEntityList.get(position).descrp);
        holder.tvTemperature.setText("Temperature:" + this.climaEntityList.get(position).temperat +"C");
        holder.tvHumidity.setText("Humidity: " + this.climaEntityList.get(position).humidit + "%");
        holder.tvPressure.setText("Pressure:" + this.climaEntityList.get(position).pressur);

    }

    @Override
    public int getItemCount()
    {
        return this.climaEntityList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView tvCityName;
        TextView tvDescription;
        TextView tvTemperature;
        TextView tvHumidity;
        TextView tvPressure;


        public MyViewHolder(View view){
             super(view);
             tvCityName = view.findViewById(R.id.tvCityName);
             tvDescription = view.findViewById(R.id.tvDescription);
             tvTemperature = view.findViewById(R.id.tvTemperature);
             tvHumidity = view.findViewById(R.id.tvHumidity);
             tvPressure = view.findViewById(R.id.tvPressure);
         }
    }
}
