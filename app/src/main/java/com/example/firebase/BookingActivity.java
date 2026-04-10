package com.example.firebase;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BookingActivity extends AppCompatActivity {

    private TextView tvMovieTitle, tvSelectedSeat, tvTotalPrice;
    private RecyclerView rvSeats;
    private Button btnConfirm;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String movieId, movieTitle;
    
    private SeatAdapter seatAdapter;
    private List<String> allSeats;
    private List<String> bookedSeats;
    private List<String> selectedSeatsList = new ArrayList<>();
    
    private final double PRICE_PER_SEAT = 120000.0;
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mAuth = FirebaseAuth.getInstance();
        movieId = getIntent().getStringExtra("movieId");
        movieTitle = getIntent().getStringExtra("movieTitle");

        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvMovieTitle = findViewById(R.id.tvBookingMovieTitle);
        tvSelectedSeat = findViewById(R.id.tvSelectedSeat);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        rvSeats = findViewById(R.id.rvSeats);
        btnConfirm = findViewById(R.id.btnConfirmBooking);

        tvMovieTitle.setText(movieTitle);
        updateTotalPrice(0);

        setupSeatList();
        fetchBookedSeats();

        btnConfirm.setOnClickListener(v -> {
            if (selectedSeatsList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ghế!", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmBooking();
        });
    }

    private void setupSeatList() {
        allSeats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D"};
        for (String row : rows) {
            for (int i = 1; i <= 10; i++) {
                allSeats.add(row + i);
            }
        }
        
        bookedSeats = new ArrayList<>();
        seatAdapter = new SeatAdapter(allSeats, bookedSeats, selectedSeats -> {
            selectedSeatsList = new ArrayList<>(selectedSeats);
            if (selectedSeatsList.isEmpty()) {
                tvSelectedSeat.setText("Ghế đã chọn: Chưa chọn");
            } else {
                tvSelectedSeat.setText("Ghế đã chọn: " + String.join(", ", selectedSeatsList));
            }
            updateTotalPrice(selectedSeatsList.size());
        });

        rvSeats.setLayoutManager(new GridLayoutManager(this, 5));
        rvSeats.setAdapter(seatAdapter);
    }

    private void updateTotalPrice(int count) {
        double total = count * PRICE_PER_SEAT;
        tvTotalPrice.setText("Tổng tiền: " + df.format(total) + " VNĐ");
    }

    private void fetchBookedSeats() {
        mDatabase.child("movies").child(movieId).child("booked_seats")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        bookedSeats.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            bookedSeats.add(data.getKey());
                        }
                        seatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void confirmBooking() {
        if (mAuth.getCurrentUser() == null) return;

        int numberOfSeats = selectedSeatsList.size();
        double totalPrice = numberOfSeats * PRICE_PER_SEAT;

        String ticketId = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(
                ticketId,
                mAuth.getCurrentUser().getUid(),
                movieId,
                movieTitle,
                "Galaxy Cinema",
                new Date(),
                new ArrayList<>(selectedSeatsList),
                totalPrice
        );

        mDatabase.child("tickets").child(ticketId).setValue(ticket)
                .addOnSuccessListener(aVoid -> {
                    for (String seat : selectedSeatsList) {
                        mDatabase.child("movies").child(movieId).child("booked_seats").child(seat).setValue(true);
                    }
                    
                    Toast.makeText(this, "Đã đặt vé thành công!", Toast.LENGTH_LONG).show();
                    
                    // HẸN GIỜ THÔNG BÁO: Sau 10 giây sẽ hiện thông báo nhắc nhở
                    scheduleNotification(movieTitle);
                    
                    seatAdapter.clearSelection();
                    selectedSeatsList.clear();
                    tvSelectedSeat.setText("Ghế đã chọn: Chưa chọn");
                    updateTotalPrice(0);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi đặt vé: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void scheduleNotification(String title) {
        Data inputData = new Data.Builder()
                .putString("movieTitle", title)
                .build();

        OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(10, TimeUnit.SECONDS) // Hẹn giờ 10 giây để bạn thấy ngay kết quả
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(notificationWork);
    }
}
