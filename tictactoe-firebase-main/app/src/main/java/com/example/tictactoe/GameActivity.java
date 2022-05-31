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
    // Initialize the Firebase instance
    FirebaseDatabase database = FirebaseDatabase.getInstance("https://tictactoe-winksai-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference reference;


    boolean kicked = false;

    boolean isXActive = false;
    boolean isOActive = false;

    ValueEventListener moveListener;

    int gameID = 0;

    boolean anyFieldSet = false;

    boolean gameActive = true;

    // Who shall make a move?
    // 0 - X
    // 1 - O
    int activePlayer = 0;
    int playerID = 0;

    // Array storing the state of each field.
    int[] fieldState = {2, 2, 2, 2, 2, 2, 2, 2, 2};

    // Field state info:
    // 0 - X
    // 1 - O
    // 2 - Empty

    // Store all winning positions in a two-dimensional array.
    // The board:
    //  0  1  2
    //  3  4  5
    //  6  7  8
    int[][] winPositions = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},    // Horizontal positions
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},    // Vertical positions
            {0, 4, 8}, {2, 4, 6}                // Diagonal positions
    };
    // A win position is stored as winPosition[x]. There are 8 winning positions in total.
    // To get each field of a single winPosition, winPosition[x][y] with 1 <= y <= 3 can be used.
    // For tic tac toe, every single winning position can be defined by hand. Would be harder for a game like "4 Gewinnt".

    // For resetting the game on draw.
    public static int counter = 0;

    // This function is called at every click on an ImageView at the game activity.
    public void makeMove(View view) {
        Log.w("MainActivity", "Game ID: " + gameID);

        // Block move-making when the player is waiting for the turn of another player.
        if (activePlayer != playerID) {
            return;
        }

        // Reset the game on next click when a player won or the game is drawn.
        if (!gameActive) {
            resetGame();
        }

        // The ImageView.tag stores the position id on the game board from 0 to 8.
        ImageView img = (ImageView) view;
        int tappedImage = Integer.parseInt(img.getTag().toString());

        Log.w("MainActivity", "x: " + tappedImage);

        // Only proceed if the tapped image is empty.
        if (fieldState[tappedImage] == 2) {
            // Increase the current move counter.
            counter++;

            // If its the 9th move and no winner can be determined later, the game will be drawn.
            if (counter == 9) {
                gameActive = false;
            }

            fieldState[tappedImage] = activePlayer;

            // change the active player from 0 to 1 or from 1 to 0
            refreshImage(img, activePlayer);

            if (activePlayer == 0) {
                activePlayer = 1;
            } else {
                activePlayer = 0;
            }

            // winner flag
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

        // Reset all fields inside the grid.
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
        // Check if a player just won the game.
        for (int[] winPosition : winPositions) {
            // Iterate through the winning positions and check if all three fields on the board are either 0 or 1 (player X or O).
            if (fieldState[winPosition[0]] != 2 &&
                    fieldState[winPosition[0]] == fieldState[winPosition[1]] &&
                    fieldState[winPosition[1]] == fieldState[winPosition[2]]) {

                // Set the winner flag.
                winningFlag = 1;

                String winnerStr;

                // Reset the game on next tap.
                gameActive = false;

                if (fieldState[winPosition[0]] == 0) {
                    winnerStr = "X hat das Spiel gewonnen";
                } else {
                    winnerStr = "O hat das Spiel gewonnen";
                }

                // Update the status TextView text.
                TextView status = findViewById(R.id.status);
                status.setText(winnerStr);
            }
        }

        // If 9 moves were made and no winner is determined (flag was not set), the game is a draw.
        if (winningFlag == 0 && counter == 9) {
            TextView status = findViewById(R.id.status);
            status.setText("Spiel unentschieden");
        }

        return winningFlag;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // the player which created the game is player ID 0
        gameID = getIntent().getIntExtra("gameID", 0);
        playerID = getIntent().getIntExtra("playerID", 0);

        // Display the game ID
        TextView gameIdView = (TextView) findViewById(R.id.gameIdView);
        gameIdView.setText("Game-ID: " + gameID);

        // Display whoAmI
        TextView whoAmI = findViewById(R.id.whoAmI);
        if (playerID == 0) {
            whoAmI.setText("Du spielst X");
        } else {
            whoAmI.setText("Du spielst O");
        }

        reference = database.getReference("games")
                .child(String.valueOf(gameID));

        if (playerID == 0) {
            // If the player created the game, created a new ArrayList containing all 2's and push it to Firebase.
            List<Integer> fieldStateList = new ArrayList<>(fieldState.length);
            for (int i : fieldState) {
                fieldStateList.add(i);
            }

            // The player creating the game makes the first move.
            activePlayer = 0;

            reference.child("fieldState").setValue(fieldStateList);
            reference.child("activePlayer").setValue(activePlayer);

            reference.child("XisActive").setValue(true);

            if (!isOActive) {
                reference.child("OisActive").setValue(false);
            }

            reference.child("createdAt").setValue(new Date());

            TextView status = findViewById(R.id.status);
            status.setText("Bitte mach den ersten Zug..");

            refreshActiveStatus();

            listenForMoves();
        } else {
            anyFieldSet = false;

            // On joining an existing game, first check if any field is already checked.
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Long> fieldStateList = (List<Long>) dataSnapshot.child("fieldState").getValue();
                    Log.w("MainActivity", "Field state list: " + fieldStateList);

                    isXActive = (Boolean) dataSnapshot.child("XisActive").getValue();
                    isOActive = (Boolean) dataSnapshot.child("OisActive").getValue();

                    refreshActiveStatus();

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
                    // Failed to read value
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
        // Wait for an action from the database after every turn.
        moveListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!gameActive) {
                    resetGame();
                }

                // A List containing Longs is needed, as Firebase seems to return this.
                Log.w("MainActivity", "Data change.");
                List<Long> fieldStateList = (List<Long>) dataSnapshot.child("fieldState").getValue();

                activePlayer = dataSnapshot.child("activePlayer").getValue(Integer.class);

                // Refresh the game board
                refreshScene(fieldStateList);

                isXActive = (Boolean) dataSnapshot.child("XisActive").getValue();
                isOActive = (Boolean) dataSnapshot.child("OisActive").getValue();
                refreshActiveStatus();

                checkGameWinner();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("MainActivity", "Failed to read value.", error.toException());
            }
        };

        reference.addValueEventListener(moveListener);
    }

    public void refreshScene(List<Long> fieldStateList) {
        // Compare the ArrayList from Firebase to the local int[] array.
        // fieldStateList = new field states
        // fieldState = current local field state

        for(int i = 0; i < fieldState.length; i++) {  // running 9 times, for each tile
            if (fieldStateList.get(i) != fieldState[i]) {  // if local and server state don't match
                anyFieldSet = true;
                ImageView img = findViewById(R.id.linearLayout).findViewWithTag(String.valueOf(i));  // get the non-matching tile

                // Refresh the local state at this position.
                fieldState[i] = fieldStateList.get(i).intValue();

                refreshImage(img, fieldState[i]);
            }
        }

        if (playerID == 1 && !anyFieldSet) {
            TextView status = findViewById(R.id.status);
            status.setText("Warte auf ersten Zug des Gegners..");
        }
    }

    public void refreshImage(ImageView img, Integer playerID) {
        // Set the displaying image accordingly.
        if (playerID == 0) {
            img.setImageResource(R.drawable.x);

            // Change the text of the status TextBox
            TextView status = findViewById(R.id.status);
            status.setText("Am Zug: O");
        } else {
            img.setImageResource(R.drawable.o);

            // Change the text of the status TextBox
            TextView status = findViewById(R.id.status);
            status.setText("Am Zug: X");
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

            // Failed to read value
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


            // Failed to read value
            reference.addListenerForSingleValueEvent(listener);

            reference.removeEventListener(moveListener);
        } else {
            kicked = false;
        }

        super.onDestroy();
    }

    public void refreshActiveStatus() {
        TextView xactive = findViewById(R.id.textViewActiveX);

        if (isXActive) {
            xactive.setText("X: Online");
        } else {
            xactive.setText("X: Offline");
        }

        TextView oactive = findViewById(R.id.textViewActiveO);

        if (isOActive) {
            oactive.setText("O: Online");
        } else {
            oactive.setText("O: Offline");
        }

        if (playerID == 1 && isXActive == false) {
            Toast.makeText(this, "Der Host hat das Spiel verlassen.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void leaveGame(View view) {
        finish();
    }
}