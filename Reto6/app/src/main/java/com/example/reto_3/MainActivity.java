 package com.example.reto_3;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TicTacToeGame mGame;

    // Buttons making up the board
    private Button mBoardButtons[];
    // Various text displayed
    private TextView mInfoTextView;

    //Game over variable
    private boolean mGameOver;
    //Who starts game
    private Random mGameStartsH;
    // Results text displayed
    private TextView mHumanWinTextView;
    private TextView mTieWinTextView;
    private TextView mAndroidWinTextView;

    private BoardView mBoardView;

    private int mResults[];

    static final int DIALOG_DIFFICULTY_ID = 0;
    static final int DIALOG_RESET_ID = 1;
    static final int DIALOG_ABOUT_ID = 2;

    private boolean mHTurn;

    private SharedPreferences mPrefs;

    MediaPlayer mHumanMediaPlayer;
    MediaPlayer mComputerMediaPlayer;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharArray("board", mGame.getBoardState());
        outState.putBoolean("mGameOver", mGameOver);
        outState.putInt("mHumanWins", mResults[0]);
        outState.putInt("mComputerWins", mResults[2]);
        outState.putInt("mTies", mResults[1]);
        outState.putCharSequence("info", mInfoTextView.getText());
        outState.putBoolean("mHTurn", mHTurn);
        //outState.putChar("mGoFirst", mGoFirst);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHumanMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.xsound2);
        mComputerMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.osound);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mHumanMediaPlayer.release();
        mComputerMediaPlayer.release();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_game:
                startNewGame();
                return true;
            case R.id.ai_difficulty:
                showDialog(DIALOG_DIFFICULTY_ID);
                return true;
            case R.id.reset:
                showDialog(DIALOG_RESET_ID);
                return true;
            case R.id.about:
                showDialog(DIALOG_ABOUT_ID);
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id) {
            case DIALOG_DIFFICULTY_ID:
                builder.setTitle(R.string.difficulty_choose);
                final CharSequence[] levels = {
                        getResources().getString(R.string.difficulty_easy),
                        getResources().getString(R.string.difficulty_harder),
                        getResources().getString(R.string.difficulty_expert)};

                // selected is the radio button that should be selected.
                int selected = 2;
                builder.setSingleChoiceItems(levels, selected,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                dialog.dismiss(); // Close dialog
                                // Display the selected difficulty level
                                if(item == 0)
                                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy);
                                else if(item == 1)
                                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Harder);
                                else
                                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Expert);
                                mGame.getDifficultyLevel();
                                Toast.makeText(getApplicationContext(), levels[item],
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                dialog = builder.create();
                break;
            case DIALOG_RESET_ID:
                // Create the quit confirmation dialog
                builder.setMessage(R.string.reset_question)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mResults[0] = 0;
                                mResults[2] = 0;
                                mResults[1] = 0;
                                displayScores();
                            }
                        })
                        .setNegativeButton(R.string.no, null);
                dialog = builder.create();
                break;
            case DIALOG_ABOUT_ID:
                Context context = getApplicationContext();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.about_dialog, null);
                builder.setView(layout);
                builder.setPositiveButton("OK", null);
                dialog = builder.create();
        }
        return dialog;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBoardButtons = new Button[TicTacToeGame.BOARD_SIZE];
        mInfoTextView = (TextView) findViewById(R.id.information);

        mHumanWinTextView = (TextView) findViewById(R.id.humanR);
        mTieWinTextView = (TextView) findViewById(R.id.tieR);
        mAndroidWinTextView = (TextView) findViewById(R.id.androidR);
        mResults = new int[3];
        for(int i=0;i<3;i++){
            mResults[i] = 0;
        }

        mGame = new TicTacToeGame();
        mBoardView = (BoardView) findViewById(R.id.board);
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);

        mGameStartsH = new Random();

        mPrefs = getSharedPreferences("ttt_prefs", MODE_PRIVATE);

        // Restore the scores
        mResults[0] = mPrefs.getInt("mHumanWins", 0);
        mResults[2] = mPrefs.getInt("mComputerWins", 0);
        mResults[1] = mPrefs.getInt("mTies", 0);

        if (savedInstanceState == null) {
            startNewGame();
        }
        else {
            // Restore the game's state
            mGame.setBoardState(savedInstanceState.getCharArray("board"));
            mGameOver = savedInstanceState.getBoolean("mGameOver");
            mInfoTextView.setText(savedInstanceState.getCharSequence("info"));
            mResults[0] = savedInstanceState.getInt("mHumanWins");
            mResults[2] = savedInstanceState.getInt("mComputerWins");
            mResults[1] = savedInstanceState.getInt("mTies");
            mHTurn = savedInstanceState.getBoolean("mHTurn");
            //mGoFirst = savedInstanceState.getChar("mGoFirst");
        }
        displayScores();
    }

    @Override
    protected void onStop() {
        super.onStop();
// Save the current scores
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("mHumanWins", mResults[0]);
        ed.putInt("mComputerWins", mResults[2]);
        ed.putInt("mTies", mResults[1]);
        ed.commit();
    }

    private void displayScores() {
        mHumanWinTextView.setText("Human: " + (mResults[0]));
        mTieWinTextView.setText("Tie: " + (mResults[1]));
        mAndroidWinTextView.setText("Android: " + (mResults[2]));
    }
    // Set up the game board.
    private void startNewGame() {
        mGame.clearBoard();
        mBoardView.invalidate(); // Redraw the board
        // Human goes first
        mGameOver = false;
        if(!mGameStartsH.nextBoolean()){
            mInfoTextView.setText(R.string.first_computer);
            mHTurn = false;
            int move = mGame.getComputerMove();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (mGame.setMove(TicTacToeGame.COMPUTER_PLAYER, move)) {
                        mBoardView.invalidate();
                    }
                    mComputerMediaPlayer.start();
                    mInfoTextView.setText(R.string.turn_human);
                    mHTurn = true;
                }
            }, 1500);
        }else{
            mHTurn = true;
            mInfoTextView.setText(R.string.first_human);
        }
    } // End of startNewGame

    private boolean setMove(char player, int location) {
        if (mGame.setMove(player, location)) {
            mBoardView.invalidate(); // Redraw the board
            return true;
        }
        return false;
    }

    // Listen for touches on the board
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            // Determine which cell was touched
            int col = (int) event.getX() / mBoardView.getBoardCellWidth();
            int row = (int) event.getY() / mBoardView.getBoardCellHeight();
            int pos = row * 3 + col;
            if(mHTurn){
                if (!mGameOver && setMove(TicTacToeGame.HUMAN_PLAYER, pos)){
                    setMove(TicTacToeGame.HUMAN_PLAYER, pos);
                    mHTurn = false;
                    mHumanMediaPlayer.start();
                    // If no winner yet, let the computer make a move
                    int winner = mGame.checkForWinner();
                    if (winner == 0) {
                        mInfoTextView.setText(R.string.turn_computer);
                        int move = mGame.getComputerMove();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                if(setMove(TicTacToeGame.COMPUTER_PLAYER, move)){
                                    mBoardView.invalidate();
                                }
                                mComputerMediaPlayer.start();
                                mHTurn = true;
                                int winner = mGame.checkForWinner();
                                if (winner == 0)
                                    mInfoTextView.setText(R.string.turn_human);
                                else if (winner == 1) {
                                    mResults[1] = mResults[1] + 1;
                                    mInfoTextView.setText(R.string.result_tie);
                                    mTieWinTextView.setText("Tie: " + (mResults[1]));
                                    mGameOver = true;
                                }else if (winner == 2){
                                    mResults[0] = mResults[0] + 1;
                                    mInfoTextView.setText(R.string.result_human_wins);
                                    mHumanWinTextView.setText("Human: " + (mResults[0]));
                                    mGameOver = true;
                                }else{
                                    mResults[2] = mResults[2] + 1;
                                    mInfoTextView.setText(R.string.result_computer_wins);
                                    mAndroidWinTextView.setText("Android: " + (mResults[2]));
                                    mGameOver = true;
                                }
                            }
                        }, 1500);
                    }
                    else if (winner == 1) {
                        mResults[1] = mResults[1] + 1;
                        mInfoTextView.setText(R.string.result_tie);
                        mTieWinTextView.setText("Tie: " + (mResults[1]));
                        mGameOver = true;
                    }else if (winner == 2){
                        mResults[0] = mResults[0] + 1;
                        mInfoTextView.setText(R.string.result_human_wins);
                        mHumanWinTextView.setText("Human: " + (mResults[0]));
                        mGameOver = true;
                    }else{
                        mResults[2] = mResults[2] + 1;
                        mInfoTextView.setText(R.string.result_computer_wins);
                        mAndroidWinTextView.setText("Android: " + (mResults[2]));
                        mGameOver = true;
                    }
                }
            }
            // So we aren't notified of continued events when finger is moved
            return false;
        }
    };
}