package com.example.firebase;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {

    private List<String> seatList;
    private List<String> bookedSeats;
    private List<String> selectedSeats = new ArrayList<>();
    private OnSeatSelectionListener listener;

    public interface OnSeatSelectionListener {
        void onSeatsSelected(List<String> selectedSeats);
    }

    public SeatAdapter(List<String> seatList, List<String> bookedSeats, OnSeatSelectionListener listener) {
        this.seatList = seatList;
        this.bookedSeats = bookedSeats;
        this.listener = listener;
    }

    public List<String> getSelectedSeats() {
        return selectedSeats;
    }

    public void clearSelection() {
        selectedSeats.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seat, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        String seatName = seatList.get(position);
        holder.tvSeatNumber.setText(seatName);

        if (bookedSeats.contains(seatName)) {
            // Ghế đã đặt - Màu đỏ
            holder.tvSeatNumber.setBackgroundColor(Color.parseColor("#F44336"));
            holder.itemView.setClickable(false);
        } else if (selectedSeats.contains(seatName)) {
            // Ghế đang chọn - Màu xanh dương
            holder.tvSeatNumber.setBackgroundColor(Color.parseColor("#2196F3"));
            holder.itemView.setOnClickListener(v -> {
                selectedSeats.remove(seatName);
                notifyDataSetChanged();
                listener.onSeatsSelected(selectedSeats);
            });
        } else {
            // Ghế trống - Màu xanh lá
            holder.tvSeatNumber.setBackgroundColor(Color.parseColor("#4CAF50"));
            holder.itemView.setOnClickListener(v -> {
                selectedSeats.add(seatName);
                notifyDataSetChanged();
                listener.onSeatsSelected(selectedSeats);
            });
        }
    }

    @Override
    public int getItemCount() {
        return seatList.size();
    }

    static class SeatViewHolder extends RecyclerView.ViewHolder {
        TextView tvSeatNumber;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSeatNumber = itemView.findViewById(R.id.tvSeatNumber);
        }
    }
}
