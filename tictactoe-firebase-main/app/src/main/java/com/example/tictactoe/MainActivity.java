package com.example.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    // Verbinden mit der Firebase Cloud
    FirebaseDatabase database = FirebaseDatabase.getInstance("https://tictachoe-65df8-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference reference = database.getReference("games");

    ArrayAdapter arrAdapter;

    HashMap games;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.gameList);

        ArrayList<String> gameList = new ArrayList<>();
        // Zum Anzeigen der aktuellen Spiele. Neuset wird zuerst angezeigt.
        Map<Long, String> filteredGameMap = new TreeMap<>(Collections.reverseOrder());

        arrAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, gameList);

        listView.setAdapter(arrAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int gameID = Integer.parseInt(gameList.get(position));

                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("playerID", 1);
                intent.putExtra("gameID", gameID);
                startActivity(intent);
            }
        });

        ValueEventListener gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                games = (HashMap) dataSnapshot.getValue();

                gameList.clear();
                filteredGameMap.clear();

                // Try catch, just in case
                try {
                    if (!games.isEmpty()) {
                        Iterator it = games.entrySet().iterator();

                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();

                            Long timestamp = (Long) dataSnapshot.child((String) pair.getKey()).child("createdAt").child("time").getValue();

                            // Spiel nicht hinzufÃ¼gen, wenn in Datenbank keine Zeit eingetragen ist
                            if (timestamp != null) {
                                boolean xActive = (boolean) dataSnapshot.child((String) pair.getKey()).child("XisActive").getValue();
                                boolean oActive = (boolean) dataSnapshot.child((String) pair.getKey()).child("OisActive").getValue();

                                if (!(xActive && oActive)) {
                                    filteredGameMap.put(timestamp, (String) pair.getKey());
                                }
                            }
                        }

                        Log.d("####", "Map: " + filteredGameMap);

                        Iterator it2 = filteredGameMap.entrySet().iterator();

                        for (String value : filteredGameMap.values()) {
                            gameList.add(value);
                        }

                        Log.d("####", "List: " + gameList);
                    }
                } catch (Exception e) {
                    System.out.println("ok");
                }

                listView.setAdapter(arrAdapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("MainActivity", "Failed to read value.", error.toException());
            }
        };

        reference.addValueEventListener(gameListener);
    }

    public void createGame(View view) {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);

        // Generiere nummer zwischen 1000 und 9999 --> Game ID
        int gameID =  (int) Math.floor(Math.random()*(9999-1000+1)+1000);

        intent.putExtra("playerID", 0);
        intent.putExtra("gameID", gameID);
        startActivity(intent);
    }

    public void joinGame(View view) {
        int gameID;
        try {
            EditText gameNumberField = findViewById(R.id.gameNumber);

            // Game ID von eingabefeld auslesen
            gameID = Integer.parseInt(gameNumberField.getText().toString());
        } catch (Exception e) {
            Toast.makeText(this, "Konnte dem Spiel nicht beitreten.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, GameActivity.class);

        intent.putExtra("playerID", 1);
        intent.putExtra("gameID", gameID);
        startActivity(intent);
    }
}