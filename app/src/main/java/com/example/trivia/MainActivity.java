package com.example.trivia;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trivia.data.AnswerListAsyncResponse;
import com.example.trivia.data.QuestionBank;
import com.example.trivia.model.Question;
import com.example.trivia.model.Score;
import com.example.trivia.util.Prefs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView questionTextview;
    private TextView questionCounterTextview;
    private Button trueButton;
    private Button falseButton;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private int currentQuestionIndex = 0;
    private List<Question> questionList;
    private int scoreCounter = 0;
    private Score score;
    private TextView scoreTextView;
    private TextView highestScoreTextView;
    private Prefs prefs;
    private SoundPool soundPool;
    private int sound1, sound2;
    private Button shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        score = new Score();     //score object
        prefs = new Prefs(MainActivity.this);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build();

            soundPool = new SoundPool.Builder().setMaxStreams(2)
                    .setAudioAttributes(audioAttributes).build();

            sound1 = soundPool.load(this, R.raw.correct, 1);
            sound2 = soundPool.load(this,R.raw.defeat_two,1);
        }

        nextButton = findViewById(R.id.next_button);
        prevButton = findViewById(R.id.prev_button);
        trueButton = findViewById(R.id.true_button);
        falseButton = findViewById(R.id.false_button);
        questionCounterTextview = findViewById(R.id.counter_text);
        questionTextview = findViewById(R.id.question_textview);
        scoreTextView = findViewById(R.id.score_text);
        highestScoreTextView = findViewById(R.id.highest_score);
        shareButton = findViewById(R.id.share_button);

        nextButton.setOnClickListener(this);
        prevButton.setOnClickListener(this);
        trueButton.setOnClickListener(this);
        falseButton.setOnClickListener(this);
        shareButton.setOnClickListener(this);

        scoreTextView.setText(MessageFormat.format("Current Score : {0}", String.valueOf(score.getScore())));
        highestScoreTextView.setText(MessageFormat.format("Highest Score : {0}", String.valueOf(prefs.getHighScore())));
        currentQuestionIndex = prefs.getState();
        Log.d("State", "onCreate: " + prefs.getState());

        questionList = new QuestionBank().getQuestions(new AnswerListAsyncResponse() {
            @Override
            public void processFinished(ArrayList<Question> questionArrayList) {
                questionTextview.setText(questionArrayList.get(currentQuestionIndex).getAnswer());
                questionCounterTextview.setText(MessageFormat.format("{0} / {1}", currentQuestionIndex, questionList.size()));
                //Log.d("inside", "processFinished: "+questionArrayList);
            }
        });
        //Log.d("Main", "onCreate: "+questionList);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.prev_button:
                if(currentQuestionIndex > 0) {
                    currentQuestionIndex = (currentQuestionIndex - 1) % questionList.size();
                    updateQuestion();
                }
                break;

            case R.id.next_button:
                goNextQue();
                break;

            case R.id.true_button:
                checkAnswer(true);
                updateQuestion();
                break;

            case R.id.false_button:
                checkAnswer(false);
                updateQuestion();
                break;

            case R.id.share_button:
                shareScore();
                break;
        }
    }

    private void shareScore() {

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hey I am Playing Trivia");
        intent.putExtra(Intent.EXTRA_TEXT, MessageFormat.format("Current Score:{0}\nHighest Score:{1}",
                score.getScore(), prefs.getHighScore()));
        startActivity(intent);
    }

    private void checkAnswer(boolean userChoice) {
        boolean answerIsTrue = questionList.get(currentQuestionIndex).isAnswerTrue();
        int toastMessageId = 0;

        if(userChoice == answerIsTrue){

            fadeView();
            addPoints();
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
                soundPool.play(sound1,1,1,0,0,1);
            }else {
                toastMessageId = R.string.correct_answer;
                Toast.makeText(MainActivity.this,toastMessageId,Toast.LENGTH_SHORT).show();
            }
        }else{

            shakeAnimation();
            deductPoints();
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
                soundPool.play(sound2,1,1,0,0,1);
            }else {
                toastMessageId = R.string.wrong_answer;
                Toast.makeText(MainActivity.this,toastMessageId,Toast.LENGTH_SHORT).show();
            }
        }


    }

    private void updateQuestion() {
        String question = questionList.get(currentQuestionIndex).getAnswer();
        questionTextview.setText(question);
        questionCounterTextview.setText(MessageFormat.format("{0} / {1}", currentQuestionIndex, questionList.size()));
    }

    private void shakeAnimation(){
        Animation shake = AnimationUtils.loadAnimation(MainActivity.this,
                             R.anim.shake_animation);

        final CardView cardView = findViewById(R.id.cardView);
        cardView.setAnimation(shake);
        shake.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                cardView.setCardBackgroundColor(Color.RED);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardView.setCardBackgroundColor(Color.WHITE);
                goNextQue();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void fadeView(){
        final CardView cardView = findViewById(R.id.cardView);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f,0.0f);
        alphaAnimation.setDuration(350);
        alphaAnimation.setRepeatCount(1);
        alphaAnimation.setRepeatMode(Animation.REVERSE);

        cardView.setAnimation(alphaAnimation);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                cardView.setCardBackgroundColor(Color.GREEN);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardView.setCardBackgroundColor(Color.WHITE);
                goNextQue();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private  void addPoints(){
        scoreCounter += 10;
        score.setScore(scoreCounter);
        scoreTextView.setText(MessageFormat.format("Current Score : {0}", String.valueOf(score.getScore())));
        Log.d("Score", "addPoints: "+score.getScore());
    }

    private  void deductPoints(){
        scoreCounter -= 10;
        if(scoreCounter > 0){
            score.setScore(scoreCounter);
            scoreTextView.setText(MessageFormat.format("Current Score : {0}", String.valueOf(score.getScore())));
            Log.d("Score", "deductPoints: " + score.getScore());
        }
        else{
            scoreCounter = 0;
            score.setScore(scoreCounter);
            scoreTextView.setText(MessageFormat.format("Current Score : {0}", String.valueOf(score.getScore())));
        }

    }

    @Override
    protected void onPause() {
        prefs.saveHighScore(score.getScore());
        prefs.setState(currentQuestionIndex);
        super.onPause();
    }

    private void goNextQue(){
        currentQuestionIndex = (currentQuestionIndex + 1) % questionList.size();
        updateQuestion();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            if(soundPool != null){
                soundPool.release();
                soundPool = null;
            }
        }

    }
}