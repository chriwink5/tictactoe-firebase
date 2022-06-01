package com.example.tictactoe;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import java.util.Date;
import java.util.List;

public class GameActivity extends AppCompatActivity {
    // Mit der Firebase cloud verbinden
    FirebaseDatabase database = FirebaseDatabase.getInstance("https://tictactoe-winksai-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference reference;


    boolean kicked = false;

    boolean isXActive = false;
    boolean isOActive = false;

    ValueEventListener moveListener;

    int gameID = 0;

    boolean anyFieldSet = false;

    boolean gameActive = true;

    // Wer sollte einen Move machen?
    // 0 - X
    // 1 - O
    int activePlayer = 0;
    int playerID = 0;

    // Array, welcher den Status jedes Feldes speichert
    int[] fieldState = {2, 2, 2, 2, 2, 2, 2, 2, 2};

    // Feld-Belegung
    // 0 - X
    // 1 - O
    // 2 - Empty

    // alle Siegespositionen/kombinationen in einem array speichern
    //  0  1  2
    //  3  4  5
    //  6  7  8
    int[][] winPositions = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},    // horizontal
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},    // vertical
            {0, 4, 8}, {2, 4, 6}                // diagonal
    };
    // Eine Gewinnkombination wir gespeichert in winPosition[x]. Es gibt 8 Siegespositionen.


    // Spiel zurücksetzten bei Unentschieden
    public static int counter = 0;

    // Funktion makeMove wird bei jedem Klick im Spielfeld ausgefüht
    public void makeMove(View view) {
        Log.w("MainActivity", "Game ID: " + gameID);

        // Blockieren des Spielers, wenn er nicht am Zug it
        if (activePlayer != playerID) {
            return;
        }

        // Spiel zurücksetzten, wenn Sieg oder Unentschieden
        if (!gameActive) {
            resetGame();
        }

        // ImageView.tag speichert die position id auf dem Game board (0-8)
        ImageView img = (ImageView) view;
        int tappedImage = Integer.parseInt(img.getTag().toString());

        Log.w("MainActivity", "x: " + tappedImage);

        // Nur eine Aktion zulassen wenn dieses Feld bereits noch nicht belegt ist
        if (fieldState[tappedImage] == 2) {
            // move-counter erhöhen
            counter++;

            // Bei 9 Aktionen mit keinem Gewinner -> Unentschieden
            if (counter == 9) {
                gameActive = false;
            }

            fieldState[tappedImage] = activePlayer;

            // wechseln des aktiven Spielers (der Spieler, welcher am Zug ist)
            refreshImage(img, activePlayer);

            if (activePlayer == 0) {
                activePlayer = 1;
            } else {
                activePlayer = 0;
            }

            // sieger flag
            int flag = checkGameWinner();

            List<Integer> fieldStateList = new ArrayList<>(fieldState.length);
            for (int i : fieldState) {
                fieldStateList.add(i);
            }

            reference.child("fieldState").setValue(fieldStateList);
            reference.child("activePlayer").setValue(activePlayer);
        }
    }

    public void resetGame() {
        gameActive = true;
        counter = 0;

        // alle Felder zurücksetzten
        Arrays.fill(fieldState, 2);

        List<Integer> fieldStateList = new ArrayList<>(fieldState.length);
        for (int i : fieldState) {
            fieldStateList.add(i);
        }

        reference.child("fieldState").setValue(fieldStateList);

        ((ImageView) findViewById(R.id.imageView0)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView1)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView2)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView3)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView4)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView5)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView6)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView7)).setImageResource(0);
        ((ImageView) findViewById(R.id.imageView8)).setImageResource(0);
    }

    public int checkGameWinner() {
        int winningFlag = 0;
        // Kontrollieren ob ein Spieler gewonnen hat
        for (int[] winPosition : winPositions) {

            //schauen ob alle Felder 0 oder 1 sind
            if (fieldState[winPosition[0]] != 2 &&
                    fieldState[winPosition[0]] == fieldState[winPosition[1]] &&
                    fieldState[winPosition[1]] == fieldState[winPosition[2]]) {

                // Setzten der Gewinner-flag
                winningFlag = 1;

                String winnerStr;

                // Spiel beim nächsten Zug zurücksetzten
                gameActive = false;

                if (fieldState[winPosition[0]] == 0) {
                    winnerStr = "X hat das Spiel gewonnen";
                } else {
                    winnerStr = "O hat das Spiel gewonnen";
                }


            }
        }

        // winnerflag auf unentschiden setzten, wenn 9 Aktionen ausgeführt wurden und es keine Sieger gibt
        return winningFlag;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Der Spieler der das Spiel erstellte, bekommt die ID 0
        gameID = getIntent().getIntExtra("gameID", 0);
        playerID = getIntent().getIntExtra("playerID", 0);





        reference = database.getReference("games")
                .child(String.valueOf(gameID));

        if (playerID == 0) {
            // Wenn der Spieler das Spiel erstellt hat, erstellt er eine neue ArrayList, die alle 2 enthält, und überträgt sie an die Firebase.
            List<Integer> fieldStateList = new ArrayList<>(fieldState.length);
            for (int i : fieldState) {
                fieldStateList.add(i);
            }

            // Der Spieler, der das Spiel erstellt, macht die erste Aktion
            activePlayer = 0;

            reference.child("fieldState").setValue(fieldStateList);
            reference.child("activePlayer").setValue(activePlayer);

            reference.child("XisActive").setValue(true);

            if (!isOActive) {
                reference.child("OisActive").setValue(false);
            }

            reference.child("createdAt").setValue(new Date());

            TextView eidi = findViewById(R.id.ID);
            eidi.setText("ID: "+ gameID);




            listenForMoves();
        } else {
            anyFieldSet = false;

            // Vor dem Joinen eines Spieles, zuerst schauen ob schon eine Aktion ausgeführt wurde
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Long> fieldStateList = (List<Long>) dataSnapshot.child("fieldState").getValue();
                    Log.w("MainActivity", "Field state list: " + fieldStateList);

                    isXActive = (Boolean) dataSnapshot.child("XisActive").getValue();
                    isOActive = (Boolean) dataSnapshot.child("OisActive").getValue();


                    refreshScene(fieldStateList);

                    listenForMoves();

                    if (isOActive) {
                        denyAccess();
                        return;
                    }

                    reference.child("OisActive").setValue(true);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Exeption wenn fehler beim laden
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });
        }
    }

    public void denyAccess() {
        Toast.makeText(this, "Das Spiel ist bereits voll.", Toast.LENGTH_LONG).show();
        kicked = true;
        finish();
    }

    public void listenForMoves() {
        // Auf eine Aktion der Datenbank warten nach jede Zug
        moveListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!gameActive) {
                    resetGame();
                }

                // Firebase benötigt ein Logfile, hier ist eine liste damit
                Log.w("MainActivity", "Data change.");
                List<Long> fieldStateList = (List<Long>) dataSnapshot.child("fieldState").getValue();

                activePlayer = dataSnapshot.child("activePlayer").getValue(Integer.class);

                // Game board refreshen
                refreshScene(fieldStateList);

                isXActive = (Boolean) dataSnapshot.child("XisActive").getValue();
                isOActive = (Boolean) dataSnapshot.child("OisActive").getValue();


                checkGameWinner();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Fehler beim lesen
                Log.w("MainActivity", "Failed to read value.", error.toException());
            }
        };

        reference.addValueEventListener(moveListener);
    }

    public void refreshScene(List<Long> fieldStateList) {
        // Arraylist mit lokalem int[] array vergleichen
        // fieldStateList = neue field states
        // fieldState = aktuelle lokale field state

        for(int i = 0; i < fieldState.length; i++) {  // läuft 9 mal
            if (fieldStateList.get(i) != fieldState[i]) {  // wenn lokal und server nicht übereinstimmen
                anyFieldSet = true;
                ImageView img = findViewById(R.id.linearLayout).findViewWithTag(String.valueOf(i));

                // refreshen des lokalen states
                fieldState[i] = fieldStateList.get(i).intValue();

                refreshImage(img, fieldState[i]);
            }
        }


    }

    public void refreshImage(ImageView img, Integer playerID) {
        // setzten des display image
        if (playerID == 0) {
            img.setImageResource(R.drawable.x);



        } else {
            img.setImageResource(R.drawable.o);



        }
    }

    @Override
    protected void onDestroy() {
        if (!kicked) {
            Log.w("GameActivity", "Activity closed..");

            if (playerID == 0) {
                reference.child("XisActive").setValue(false);
            } else {
                reference.child("OisActive").setValue(false);
            }

            // Fehler beim lesen
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    isXActive = (Boolean) dataSnapshot.child("XisActive").getValue();
                    isOActive = (Boolean) dataSnapshot.child("OisActive").getValue();

                    if (!isXActive && !isOActive) {
                        dataSnapshot.getRef().removeValue();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            };


            // Fehler beim lesen
            reference.addListenerForSingleValueEvent(listener);

            reference.removeEventListener(moveListener);
        } else {
            kicked = false;
        }

        super.onDestroy();
    }



    public void leaveGame(View view) {
        finish();
    }
}