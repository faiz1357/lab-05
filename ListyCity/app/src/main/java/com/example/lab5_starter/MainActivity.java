package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((QuerySnapshot value, FirebaseFirestoreException error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
                return;
            }
            if (value != null && !value.isEmpty()){
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value){
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            } else {
                // Clear list if no data exists
                cityArrayList.clear();
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });
    }

    @Override
    public void updateCity(City city, String newName, String newProvince) {
        String oldName = city.getName();

        // Update locally
        city.setName(newName);
        city.setProvince(newProvince);
        cityArrayAdapter.notifyDataSetChanged();

        // Update in Firestore
        if (!oldName.equals(newName)) {
            // Delete old document
            citiesRef.document(oldName).delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Old city document deleted");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error deleting old city", e);
                    });

            // Create new document with new name
            Map<String, Object> cityData = new HashMap<>();
            cityData.put("name", newName);
            cityData.put("province", newProvince);

            citiesRef.document(newName).set(cityData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "City updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error updating city", e);
                    });
        } else {
            // Just update the existing document
            Map<String, Object> cityData = new HashMap<>();
            cityData.put("name", newName);
            cityData.put("province", newProvince);

            citiesRef.document(newName).set(cityData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "City updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error updating city", e);
                    });
        }
    }

    @Override
    public void addCity(City city){
        Map<String, Object> cityData = new HashMap<>();
        cityData.put("name", city.getName());
        cityData.put("province", city.getProvince());

        citiesRef.document(city.getName()).set(cityData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "City added successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding city", e);
                });
    }
    @Override
    public void deleteCity(City city) {
        // Delete from Firestore - the snapshot listener will automatically update the UI
        citiesRef.document(city.getName()).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "City deleted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting city", e);
                });
    }
}