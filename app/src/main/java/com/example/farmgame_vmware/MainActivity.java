package com.example.farmgame_vmware;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    //View Components
    TextView tv_money;
    TextView tv_apple_up;
    TextView tv_mandarin_up;
    TextView tv_avocado_up;
    TextView tv_speed_up;

    ImageView iv_background;
    ImageView iv_apple;
    ImageView iv_mandarin;
    ImageView iv_avocado;
    ImageView iv_shovel;
    ImageView iv_speed_up;

    ImageButton ib_apple;
    ImageButton ib_mandarin;
    ImageButton ib_avocado;
    ImageButton ib_up;

    ImageView[] iv_array = new ImageView[9]; // 과일 심을 칸, TAG로 empty인지 구분
    ProgressBar[] pb_array = new ProgressBar[9]; // 각 칸의 프로그레스바
    Thread[] th_array = new Thread[9]; // 각 칸의 쓰레드
    Runnable[] run_array = new Runnable[9];
    String[] fruit_array = new String[9]; // 각 칸에 심어진 과일 이름
    int[] step_array = new int[9]; // 각 칸의 과일의 성장 단계 1, 2, 3 단계

    // 명령 값
    public final int APPLE = 1000;
    public final int MANDARIN = 2000;
    public final int AVOCADO = 3000;
    public final int SHOVEL = 4000;
    public final int UP = 10;
    public final int SPEED = 20;

    // 핸들러에 보낼 메세지의 ID값
    public final int SEND_APPLE = 6000;
    public final int SEND_MANDARIN = 7000;
    public final int SEND_AVOCADO = 8000;
    public final int SEND_SHOVEL_FIRST = 9000;
    public final int SEND_REMOVE = 10000;
    public final int SEND_UP_APPLE = 11000;
    public final int SEND_UP_MANDARIN = 12000;
    public final int SEND_UP_AVOCADO = 13000;

    // 돈 UI 핸들러,
    Handler moneyHandler;
    Handler fruitHandler_1;

    // 앱 종료 시 돈 저장하기 위한 SharedPreference (로컬 저장소)
    SharedPreferences appData;
    SharedPreferences.Editor editor;

    // 각 상황에 맞는 다이얼로그
    AlertDialog.Builder alert_not_cultivate;
    AlertDialog.Builder alert_not_enogh_money;
    AlertDialog.Builder alert_already;
    AlertDialog.Builder alert_remove_tree;
    AlertDialog.Builder alert_plant;
    AlertDialog.Builder alert_3step;
    AlertDialog.Builder alert_max_speed;

    Context context = this;

    // 9칸에 대한 이미지뷰 아이디, 프로그래스바
    int[] imageID = {R.id.iv_1, R.id.iv_2, R.id.iv_3, R.id.iv_4, R.id.iv_5, R.id.iv_6, R.id.iv_7, R.id.iv_8, R.id.iv_9};
    int[] pbID = {R.id.pb_1, R.id.pb_2, R.id.pb_3, R.id.pb_4, R.id.pb_5, R.id.pb_6, R.id.pb_7, R.id.pb_8, R.id.pb_9};
    int[] speedPrice = {100000, 200000, 300000};

    // 각 칸의 태그 값
    final String EMPTY = "empty";
    final String NOTEMPTY = "not_empty";
    final String NOTHING = "nothing";

    int apple_cnt;
    int mandarin_cnt;
    int avocado_cnt;

    int harvest_time = 1000;
    public int total_money = 0;
    int menu_clicked;
    int time_step;

    // 돈 3자리씩 끊어서 보여주는 format
    DecimalFormat myFormatter = new DecimalFormat("###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.handlerSetting();
        this.initialize();
        this.menuSetting();
        this.setImageListener();

    }

    @Override
    protected void onPause() {
        super.onPause();
// 액티비티 생명주기에서 onPause 시에 현재 돈 저장
        editor.putInt("money", total_money);
        editor.apply();

    }

    // View Components 초기화,
    public void initialize() {

        tv_money = findViewById(R.id.tv_money);
        tv_apple_up = findViewById(R.id.tv_apple_up);
        tv_mandarin_up = findViewById(R.id.tv_mandarin_up);
        tv_avocado_up = findViewById(R.id.tv_avocado_up);
        tv_speed_up = findViewById(R.id.tv_speed_up);

        ib_apple = findViewById(R.id.ib_apple);
        ib_mandarin = findViewById(R.id.ib_mandarin);
        ib_avocado = findViewById(R.id.ib_avocado);
        ib_up = findViewById(R.id.fruit_up);

        for (int i = 0; i < iv_array.length; i++) {
            iv_array[i] = findViewById(imageID[i]);
            iv_array[i].setImageResource(R.drawable.not_cultivate);
            iv_array[i].setTag(EMPTY);
        }

        for (int i = 0; i < pb_array.length; i++) {
            pb_array[i] = findViewById(pbID[i]);
        }

        for (int i = 0; i < fruit_array.length; i++) {
            fruit_array[i] = "";
        }

        for (int i = 0; i < step_array.length; i++) {
            step_array[i] = 1;
        }

        iv_background = findViewById(R.id.iv_background);
        iv_apple = findViewById(R.id.iv_apple);
        iv_mandarin = findViewById(R.id.iv_mandarin);
        iv_avocado = findViewById(R.id.iv_avocado);
        iv_shovel = findViewById(R.id.iv_shovel);
        iv_speed_up = findViewById(R.id.iv_speed_up);

// 화면 배경 이미지 크기 적정하게 적용
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmapImage = BitmapFactory.decodeResource(getResources(), R.drawable.background, options);
        iv_background.setImageBitmap(bitmapImage);

        appData = getSharedPreferences("appData", MODE_PRIVATE);
        editor = appData.edit();

//각 상태의 다이얼로그 세팅 함수들
        notCultivateDialog();
        notEnoughMoneyDialog();
        alreadyPlantedDialog();
        removeTreeDIalog();
        plantSomethingDialog();
        already_3stepDialog();
        already_MaxSpeedDialog();

// 이전에 나갈때 저장한 돈 가져오기
        total_money = appData.getInt("money", 1000000);
// 돈 바로 적용
        moneyHandler.sendEmptyMessage(0);

//메뉴 초기화
        menu_clicked = 0;

// speed up 횟수
        time_step = 0;

//각 과일 업글 횟수
        apple_cnt = appData.getInt("apple_cnt", 0);
        mandarin_cnt = appData.getInt("mandarin_cnt", 0);
        avocado_cnt = appData.getInt("avocado_cnt", 0);


    } //initialize finish

    // 메뉴 클릭 시 토스트 메세지, 과일 업그레이드 기능 적용
    public void menuSetting() {

        iv_apple.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                menu_clicked = APPLE;
                Toast.makeText(getApplicationContext(), "Apple Selected", Toast.LENGTH_SHORT).show();
            }
        });

        iv_mandarin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                menu_clicked = MANDARIN;
                Toast.makeText(getApplicationContext(), "Mandarin Selected", Toast.LENGTH_SHORT).show();
            }
        });

        iv_avocado.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                menu_clicked = AVOCADO;
                Toast.makeText(getApplicationContext(), "Avocado Selected", Toast.LENGTH_SHORT).show();
            }
        });

        iv_shovel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                menu_clicked = SHOVEL;
                Toast.makeText(getApplicationContext(), "Shovel Selected", Toast.LENGTH_SHORT).show();
            }
        });

        iv_speed_up.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (time_step == 0) { // 처음 Speed up
                    if (total_money >= 100000) {
                        total_money -= 100000;
// 돈 UI 적용
                        moneyHandler.sendEmptyMessage(0);
// Speed Up Text 변경
                        tv_speed_up.setText("20a");
// 수확 시간 1.5배 빠르게
                        harvest_time = 750;
                        time_step += 1;
                        Toast.makeText(context, "스피드 업", Toast.LENGTH_SHORT).show();
                    } else {
// 돈 부족 다이얼로그, 메뉴 초기화
                        alert_not_enogh_money.show();
                        menu_clicked = 0;
                    }
                } else if (time_step == 1) { // 2번째 Speed up
                    if (total_money >= 200000) {
                        total_money -= 200000;
                        moneyHandler.sendEmptyMessage(0);
                        tv_speed_up.setText("30a");
// 수확 시간 2배 빠르게
                        harvest_time = 500;
                        time_step += 1;
                        Toast.makeText(context, "스피드 업", Toast.LENGTH_SHORT).show();
                    } else {
                        alert_not_enogh_money.show();
                        menu_clicked = 0;
                    }
                } else if (time_step == 2) { // 3번째 Speed up
                    if (total_money >= 300000) {
                        total_money -= 300000;
                        moneyHandler.sendEmptyMessage(0);
                        tv_speed_up.setText("Max");
// 수확 시간 4배 빠르게
                        harvest_time = 250;
                        time_step += 1;
                        Toast.makeText(context, "스피드 업", Toast.LENGTH_SHORT).show();
                    } else {
                        alert_not_enogh_money.show();
                        menu_clicked = 0;
                    }
                } else if (time_step >= 3) {
                    alert_max_speed.show();
                }

            }
        });

        ib_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu_clicked = UP;
                Toast.makeText(getApplicationContext(), "UP Selected", Toast.LENGTH_SHORT).show();
            }
        });

        ib_apple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Apple Enhance Selected", Toast.LENGTH_SHORT).show();
                if (total_money >= 5000) {
                    Log.d("로그", "사과 업");
                    total_money -= 5000;
                    moneyHandler.sendEmptyMessage(0);
                    apple_cnt += 1;
                } else {
                    alert_not_enogh_money.show();
                }
            }
        });

        ib_mandarin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Mandarin Enhance Selected", Toast.LENGTH_SHORT).show();
                if (total_money >= 50000) {
                    Log.d("로그", "귤 업");
                    total_money -= 50000;
                    moneyHandler.sendEmptyMessage(0);
                    mandarin_cnt += 1;
                } else {
                    alert_not_enogh_money.show();
                }
            }
        });

        ib_avocado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Avocado Enhance Selected", Toast.LENGTH_SHORT).show();
                if (total_money >= 500000) {
                    Log.d("로그", "사과 업");
                    total_money -= 500000;
                    moneyHandler.sendEmptyMessage(0);
                    avocado_cnt += 1;
                } else {
                    alert_not_enogh_money.show();
                }
            }
        });

    }//menuSetiing finish

    @SuppressLint("HandlerLeak")
    public void handlerSetting() {

// 돈 UI 접근 핸들러
        moneyHandler = new Handler() {

            @Override
            public void handleMessage(@NonNull Message msg) {

                switch (msg.what) {
                    case 0:
                        String money = myFormatter.format(total_money);
                        String brif = String.format("%.1f", total_money / 10000.0);
                        Log.d("로그", money + "(" + brif + "a)");
                        tv_money.setText(money + "(" + brif + "a)");
                        break;
                }
            }
        };

// 과일 핸들러
        fruitHandler_1 = new Handler() {

            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case SEND_SHOVEL_FIRST:
// 땅 갈기
                        iv_array[msg.arg1].setImageResource(R.drawable.nothing);
                        break;
                    case SEND_APPLE:
// 사과 심기
                        iv_array[msg.arg1].setImageResource(R.drawable.apple_1);
                        pb_array[msg.arg1].setVisibility(View.VISIBLE);
                        fruit_array[msg.arg1] = "apple";
                        run_array[msg.arg1] = new apple(msg.arg2, msg.arg1);
                        th_array[msg.arg1] = new Thread(run_array[msg.arg1]);
                        Log.d("로그", msg.arg1 + "번째 쓰레드 시작");
                        th_array[msg.arg1].start();
                        break;
                    case SEND_MANDARIN:
// 귤 심기
                        iv_array[msg.arg1].setImageResource(R.drawable.mandarin_1);
                        pb_array[msg.arg1].setVisibility(View.VISIBLE);
                        fruit_array[msg.arg1] = "mandarin";
                        mandarin mandarinrun = new mandarin(msg.arg2, msg.arg1);
                        th_array[msg.arg1] = new Thread(mandarinrun);
                        Log.d("로그", msg.arg1 + "번째 쓰레드 시작");
                        th_array[msg.arg1].start();
                        break;
                    case SEND_AVOCADO:
// 아보카도 심기
                        iv_array[msg.arg1].setImageResource(R.drawable.avocado_1);
                        pb_array[msg.arg1].setVisibility(View.VISIBLE);
                        fruit_array[msg.arg1] = "avocado";
                        avocado avocadorun = new avocado(msg.arg2, msg.arg1);
                        th_array[msg.arg1] = new Thread(avocadorun);
                        Log.d("로그", msg.arg1 + "번째 쓰레드 시작");
                        th_array[msg.arg1].start();
                        break;
                    case SEND_REMOVE:
// 과일 제거
                        Log.d("로그", msg.arg1 + "번째 쓰레드 중지");
                        th_array[msg.arg1].interrupt();
                        iv_array[msg.arg1].setImageResource(R.drawable.nothing);
                        pb_array[msg.arg1].setVisibility(View.GONE);
                        fruit_array[msg.arg1] = "";
                        step_array[msg.arg1] = 1;
                        break;
                    case SEND_UP_APPLE:
// 사과 단계 업
                        Log.d("로그", "사과 step up");
                        if (step_array[msg.arg1] == 1) { // 현재 1단계일 경우
                            th_array[msg.arg1].interrupt(); // 쓰레드 중지
                            iv_array[msg.arg1].setImageResource(R.drawable.apple_2); // 사과 2단계 이미지 적용
                            apple applerun = new apple(2, msg.arg1);
                            th_array[msg.arg1] = new Thread(applerun); // 새로운 사과 2단계 쓰레드 생성
                            Log.d("로그", msg.arg1 + "번째 쓰레드 2단계 시작");
                            th_array[msg.arg1].start(); // 쓰레드 시작
                            step_array[msg.arg1] = 2; // 단계 저장
                            break;
                        } else if (step_array[msg.arg1] == 2) { // 현재 2단계일 경우
                            th_array[msg.arg1].interrupt();
                            iv_array[msg.arg1].setImageResource(R.drawable.apple_3);
                            apple applerun = new apple(3, msg.arg1);
                            th_array[msg.arg1] = new Thread(applerun);
                            Log.d("로그", msg.arg1 + "번째 쓰레드 3단계 시작");
                            th_array[msg.arg1].start();
                            step_array[msg.arg1] = 3;
                            break;
                        }
                    case SEND_UP_MANDARIN:
// 귤 단계 업
                        Log.d("로그", "귤 step up");
                        if (step_array[msg.arg1] == 1) { // 현재 1단계일 경우
                            th_array[msg.arg1].interrupt();
                            iv_array[msg.arg1].setImageResource(R.drawable.mandarin_2);
                            mandarin man2 = new mandarin(2, msg.arg1);
                            th_array[msg.arg1] = new Thread(man2);
                            Log.d("로그", msg.arg1 + "번째 쓰레드 2단계 시작");
                            th_array[msg.arg1].start();
                            step_array[msg.arg1] = 2;
                            break;
                        } else if (step_array[msg.arg1] == 2) { // 현재 2단계일 경우
                            th_array[msg.arg1].interrupt();
                            iv_array[msg.arg1].setImageResource(R.drawable.mandarin_3);
                            mandarin man3 = new mandarin(3, msg.arg1);
                            th_array[msg.arg1] = new Thread(man3);
                            Log.d("로그", msg.arg1 + "번째 쓰레드 3단계 시작");
                            th_array[msg.arg1].start();
                            step_array[msg.arg1] = 3;
                            break;
                        }
                    case SEND_UP_AVOCADO:
                        Log.d("로그", "아보카도 step up");
                        if (step_array[msg.arg1] == 1) {
                            th_array[msg.arg1].interrupt();
                            iv_array[msg.arg1].setImageResource(R.drawable.avocado_2);
                            avocado avo2 = new avocado(2, msg.arg1);
                            th_array[msg.arg1] = new Thread(avo2);
                            Log.d("로그", msg.arg1 + "번째 쓰레드 2단계 시작");
                            th_array[msg.arg1].start();
                            step_array[msg.arg1] = 2;
                            break;
                        } else if (step_array[msg.arg1] == 2) {
                            th_array[msg.arg1].interrupt();
                            iv_array[msg.arg1].setImageResource(R.drawable.avocado_3);
                            avocado avo3 = new avocado(3, msg.arg1);
                            th_array[msg.arg1] = new Thread(avo3);
                            Log.d("로그", msg.arg1 + "번째 쓰레드 3단계 시작");
                            th_array[msg.arg1].start();
                            step_array[msg.arg1] = 3;
                            break;
                        }
                    default:
                        break;
                }
            }
        };
    }
//handler setting finish

    // 각 칸 눌렀을 때 메뉴에 따라 다른 기능 구현
    public void setImageListener() {

        for (int i = 0; i < iv_array.length; i++) {

            final int j = i;
            iv_array[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("로그", j + "번째 clicked");
                    String tag = iv_array[j].getTag().toString(); //클릭한 이미지의 태그 값

//클릭 한 메뉴에 따라 기능 구현
                    switch (menu_clicked) {

                        case APPLE:
// 경작이 안되어 있다면, dialog 띄움
                            if (tag.equals(EMPTY)) {
                                Log.d("로그", "땅부터 파");
                                alert_not_cultivate.show();
                                menu_clicked = 0;
                                break;
                            } else if (tag.equals(NOTHING)) { // 경작 된 상태
                                Log.d("로그", "사과를 심자!");
                                if (getTotal_money() >= 1000) {
                                    iv_array[j].setTag(NOTEMPTY);
                                    total_money -= 1000;
                                    moneyHandler.sendEmptyMessage(0); // 돈 적용
                                    Message msg = fruitHandler_1.obtainMessage();
// 메세지에 정보 담기
                                    msg.what = SEND_APPLE; // 사과
                                    msg.arg1 = j; // 몇 번째 칸 인지
                                    msg.arg2 = step_array[j]; // 현재 과일 단계
                                    fruitHandler_1.sendMessage(msg); // 보내기
                                    menu_clicked = 0; // 클릭한 메뉴 초기화
                                    break;
                                } else {
// 돈이 부족할 경우 돈 부족 다이얼로그 보여주고 메뉴 초기화
                                    alert_not_enogh_money.show();
                                    menu_clicked = 0;
                                    break;
                                }
                            } else if (tag.equals(NOTEMPTY)) {
//이미 과일이 심어져 있는 경우, 다이얼로그 보여주고 메뉴 초기화
                                alert_already.show();
                                menu_clicked = 0;
                                break;
                            }
                        case MANDARIN:
// 경작이 안되어 있다면, dialog 띄움
                            if (tag.equals(EMPTY)) {
                                Log.d("로그", "땅부터 파");
                                alert_not_cultivate.show();
                                menu_clicked = 0;
                                break;
//
                            } else if (tag.equals(NOTHING)) {
                                if (getTotal_money() >= 10000) {
                                    Log.d("로그", "귤을 심자!");
                                    iv_array[j].setTag(NOTEMPTY);
                                    total_money -= 10000;
                                    moneyHandler.sendEmptyMessage(0);
                                    Message msg = fruitHandler_1.obtainMessage();
                                    msg.what = SEND_MANDARIN;
                                    msg.arg1 = j;
                                    msg.arg2 = step_array[j];
                                    fruitHandler_1.sendMessage(msg);
                                    menu_clicked = 0;
                                    break;
                                } else {
                                    alert_not_enogh_money.show();
                                    menu_clicked = 0;
                                    break;
                                }
                            } else if (tag.equals(NOTEMPTY)) {
                                alert_already.show();
                                menu_clicked = 0;
                                break;
                            }
                        case AVOCADO:
// 경작이 안되어 있다면, dialog 띄움
                            if (tag.equals(EMPTY)) {
                                Log.d("로그", "땅부터 파");
                                alert_not_cultivate.show();
                                menu_clicked = 0;
                                break;
//
                            } else if (tag.equals(NOTHING)) {
                                if (getTotal_money() >= 100000) {
                                    Log.d("로그", "아보카도를 심자!");
                                    iv_array[j].setTag(NOTEMPTY);
                                    total_money -= 100000;
                                    moneyHandler.sendEmptyMessage(0);
                                    Message msg = fruitHandler_1.obtainMessage();
                                    msg.what = SEND_AVOCADO;
                                    msg.arg1 = j;
                                    msg.arg2 = step_array[j];
                                    fruitHandler_1.sendMessage(msg);
                                    menu_clicked = 0;
                                    break;
                                } else {
                                    alert_not_enogh_money.show();
                                    menu_clicked = 0;
                                    break;
                                }
                            } else if (tag.equals(NOTEMPTY)) {
                                alert_already.show();
                                menu_clicked = 0;
                                break;
                            }
                        case SHOVEL:
// 삽 메뉴 ( 경작 또는 제거)
                            if (tag.equals(EMPTY)) { // 땅을 갈아야 하는 상태
                                if (getTotal_money() >= 5000) { // 충분한 돈이 있는지 체크
                                    if (iv_array[j].getTag().equals(EMPTY)) {
                                        Log.d("로그", "삽질");
                                        getTags(); // 이미지 태그 확인
                                        iv_array[j].setTag(NOTHING); // 경작 상태로 태그 변경
                                        Message msg = fruitHandler_1.obtainMessage();
// 메세지에 정보 담기
                                        msg.what = SEND_SHOVEL_FIRST;
                                        msg.arg1 = j;
                                        fruitHandler_1.sendMessage(msg);
                                        total_money -= 5000;
                                        moneyHandler.sendEmptyMessage(0);
                                        menu_clicked = 0;
                                        break;
                                    }
                                } else {
// 돈 부족한 경우, 다이얼로그 보여주고 메뉴 초기화
                                    alert_not_enogh_money.show();
                                    menu_clicked = 0;
                                    break;
                                }
                            } else if (iv_array[j].getTag().equals(NOTEMPTY)) { //이미 작물이 있다
//작물 없앨지 묻는 다이얼로그와 yes 일시 없애고 경작된 땅으로 바꾸는 기능 구현하기
                                editor.putInt("index", j);
                                editor.apply();
                                alert_remove_tree.show(); // 과일 제거 다이얼로그 띄우기
                                menu_clicked = 0; // 메뉴 초기화
                                break;
                            } else if (iv_array[j].getTag().equals(NOTHING)) { // 땅은 갈려있지만, 식물이 없음
                                Log.d("로그", "과일이나 심으라");
                                menu_clicked = 0;
                                break;
                            }
                        case UP:
// 과일 나무 단계 업
                            if (tag.equals(EMPTY)) {
                                alert_not_cultivate.show();
                                menu_clicked = 0;
                                break;
                            } else if (tag.equals(NOTHING)) {
                                alert_plant.show();
                                menu_clicked = 0;
                                break;
                            } else if (tag.equals(NOTEMPTY)) {
                                if (fruit_array[j].equals("apple")) {
                                    if (step_array[j] == 1) { // 현재 1단계인 경우
                                        if (total_money >= 2000) { // 사과 2단계 업글 비용 2000
                                            total_money -= 2000;
                                            moneyHandler.sendEmptyMessage(0);
                                            Message msg = fruitHandler_1.obtainMessage();
// 메세지에 정보 담기
                                            msg.what = SEND_UP_APPLE;
                                            msg.arg1 = j;
                                            fruitHandler_1.sendMessage(msg);
                                            menu_clicked = 0;
                                            break;
                                        } else {
                                            alert_not_enogh_money.show();
                                            menu_clicked = 0;
                                            break;
                                        }
                                    } else if (step_array[j] == 2) { // 현재 2단계인 경우
                                        if (total_money >= 3000) { // 사과 3단계 업글 비용 3000
                                            total_money -= 3000;
                                            moneyHandler.sendEmptyMessage(0);
                                            Message msg = fruitHandler_1.obtainMessage();
                                            msg.what = SEND_UP_APPLE;
                                            msg.arg1 = j;
                                            fruitHandler_1.sendMessage(msg);
                                            menu_clicked = 0;
                                            break;
                                        } else {
                                            alert_not_enogh_money.show();
                                            menu_clicked = 0;
                                            break;
                                        }
                                    }
                                } else if (fruit_array[j].equals("mandarin")) { // 귤
                                    if (step_array[j] == 1) {
                                        if (total_money >= 20000) {
                                            total_money -= 20000;
                                            moneyHandler.sendEmptyMessage(0);
                                            Message msg = fruitHandler_1.obtainMessage();
                                            msg.what = SEND_UP_MANDARIN;
                                            msg.arg1 = j;
                                            fruitHandler_1.sendMessage(msg);
                                            menu_clicked = 0;
                                            break;
                                        } else {
                                            alert_not_enogh_money.show();
                                            menu_clicked = 0;
                                            break;
                                        }
                                    } else if (step_array[j] == 2) {
                                        if (total_money >= 30000) {
                                            total_money -= 30000;
                                            moneyHandler.sendEmptyMessage(0);
                                            Message msg = fruitHandler_1.obtainMessage();
                                            msg.what = SEND_UP_MANDARIN;
                                            msg.arg1 = j;
                                            fruitHandler_1.sendMessage(msg);
                                            menu_clicked = 0;
                                            break;
                                        } else {
                                            alert_not_enogh_money.show();
                                            menu_clicked = 0;
                                            break;
                                        }
                                    }
                                } else if (fruit_array[j].equals("avocado")) { // 아보카도
                                    if (step_array[j] == 1) {
                                        if (total_money >= 200000) {
                                            total_money -= 200000;
                                            moneyHandler.sendEmptyMessage(0);
                                            Message msg = fruitHandler_1.obtainMessage();
                                            msg.what = SEND_UP_AVOCADO;
                                            msg.arg1 = j;
                                            fruitHandler_1.sendMessage(msg);
                                            menu_clicked = 0;
                                            break;
                                        } else {
                                            alert_not_enogh_money.show();
                                            menu_clicked = 0;
                                            break;
                                        }
                                    } else if (step_array[j] == 2) {
                                        if (total_money >= 300000) {
                                            total_money -= 300000;
                                            moneyHandler.sendEmptyMessage(0);
                                            Message msg = fruitHandler_1.obtainMessage();
                                            msg.what = SEND_UP_AVOCADO;
                                            msg.arg1 = j;
                                            fruitHandler_1.sendMessage(msg);
                                            menu_clicked = 0;
                                            break;
                                        } else {
                                            alert_not_enogh_money.show();
                                            menu_clicked = 0;
                                            break;
                                        }
                                    }
                                }
                            }
                        default:
                            break;
                    }
                }//onclick finish
            });
        }//for finish
    }//setImageListener finish

    // 소지금 가져오기
    public int getTotal_money() {
        return total_money;
    }

    // 경작 안됬음을 알려주는 다이얼로그
    public void notCultivateDialog() {

// Alert Dialog 생성
        alert_not_cultivate = new AlertDialog.Builder(context);
// 다이얼로그 Title 적용
        alert_not_cultivate.setTitle("Plowing First!");
// 내용
        alert_not_cultivate.setMessage("땅을 먼저 갈아야 합니다.");

    }

    // 돈 부족 다이얼로그
    public void notEnoughMoneyDialog() {

        alert_not_enogh_money = new AlertDialog.Builder(context);
        alert_not_enogh_money.setTitle("Not enough money!");
        alert_not_enogh_money.setMessage("돈이 부족합니다.");

    }

    // 이미 과일 심어진 경우 다이얼로그
    public void alreadyPlantedDialog() {

        alert_already = new AlertDialog.Builder(context);
        alert_already.setTitle("Already Planted!");
        alert_already.setMessage("이미 과일이 심어져 있습니다.");

    }

    // 과일이 안심어진 경우 단계 업 할 시
    public void plantSomethingDialog() {

        alert_plant = new AlertDialog.Builder(context);
        alert_plant.setTitle("Plant Something!");
        alert_plant.setMessage("과일을 먼저 심으세요.");

    }

    // 과일 최종 단계에서 업을 하려 할 경우
    public void already_3stepDialog() {

        alert_3step = new AlertDialog.Builder(context);
        alert_3step.setTitle("This plant cannot grow any more");
        alert_3step.setMessage("이 식물은 이미 최종 단계입니다.");
    }

    // Speed Up 최종일 때
    public void already_MaxSpeedDialog() {

        alert_max_speed = new AlertDialog.Builder(context);
        alert_max_speed.setTitle("Already Max Speed");
        alert_max_speed.setMessage("더 이상 수확 속도를 빠르게 할 수 없습니다.");

    }

    // 과일 제거 다이얼로그 - 예 , 아니요
    public void removeTreeDIalog() {

        alert_remove_tree = new AlertDialog.Builder(context);
        alert_remove_tree.setTitle("Are you sure?");
        alert_remove_tree.setMessage("심어진 식물을 제거하시겠습니까?").setCancelable(false).setNegativeButton("아니요", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "현실에 만족하시나 봐요?", Toast.LENGTH_SHORT).show();
            }
        }).setPositiveButton("네", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int index = appData.getInt("index", 0);
                iv_array[index].setTag(NOTHING);
                step_array[index] = 1;
                Log.d("로그", "index: " + index + " Tag: " + iv_array[index].getTag());
                Message msg = fruitHandler_1.obtainMessage();
                msg.what = SEND_REMOVE;
                msg.arg1 = index;
                fruitHandler_1.sendMessage(msg);
            }
        });
    }

    // 이미지뷰의 태그 확인용
    public void getTags() {
        for (ImageView iv : iv_array) {
            Log.d("로그", iv.getTag() + "");
        }
    }

    // 사과 쓰레드용 클래스
    class apple implements Runnable {

        int step;
        int index;
        public int progress_percent;

        public apple(int step, int index) {
            this.step = step;
            this.index = index;
            progress_percent = 0;
        }

        //실행 부
        @Override
        public void run() {

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(harvest_time);
                    progress_percent += 20; // 20% 씩 오르게
                    pb_array[index].setProgress(progress_percent);
//Log.d("로그", "progress : " + progress_percent);

// 단계에 따라 돈 값 차등 적용, 100 넘으면 0으로 초기화
                    if (progress_percent >= 100) {
                        progress_percent = 0;
                        if (step_array[index] == 1) {
                            total_money += 50;
                        } else if (step_array[index] == 2) {
                            total_money += 75;
                        } else if (step_array[index] == 3) {
                            total_money += 100;
                        }

// 사과 가격 업그레이드 시 마다 100원씩 추가로 돈 들어옴
                        total_money += (apple_cnt * 100);
//돈 UI 핸들러 접근해서 바꿔주기
                        moneyHandler.sendEmptyMessage(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pb_array[index].setProgress(0);
                Log.d("로그", index + "번째 사과 쓰레드 종료");
            }
        }//run finish
    }//class apple finish

    class mandarin implements Runnable {

        int step;
        int index;
        private int progress_percent;

        public mandarin(int step, int index) {
            this.step = step;
            this.index = index;
            progress_percent = 0;
        }

        @Override
        public void run() {
            try {

                while (!Thread.currentThread().isInterrupted()) {

                    Thread.sleep(harvest_time);
                    progress_percent += 10;
                    pb_array[index].setProgress(progress_percent);
//Log.d("로그", "progress : " + progress_percent);

                    if (progress_percent >= 100) {
                        progress_percent = 0;
                        if (step_array[index] == 1) {
                            total_money += 500;
                        } else if (step_array[index] == 2) {
                            total_money += 750;
                        } else if (step_array[index] == 3) {
                            total_money += 1000;
                        }
                        total_money += (mandarin_cnt * 1000);
                        moneyHandler.sendEmptyMessage(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pb_array[index].setProgress(0);
                Log.d("로그", index + "번째 귤 쓰레드 종료");
            }
        }//run finish
    }//class mandarin finish

    class avocado implements Runnable {

        int step;
        int index;
        private int progress_percent;

        public avocado(int step, int index) {
            this.step = step;
            this.index = index;
            progress_percent = 0;
        }

        @Override
        public void run() {

            try {
                while (!Thread.currentThread().isInterrupted()) {

                    Thread.sleep(harvest_time);
                    progress_percent += 5;
                    pb_array[index].setProgress(progress_percent);
//Log.d("로그", "progress : " + progress_percent);

                    if (progress_percent >= 100) {
                        progress_percent = 0;
                        if (step_array[index] == 1) {
                            total_money += 5000;
                        } else if (step_array[index] == 2) {
                            total_money += 7500;
                        } else if (step_array[index] == 3) {
                            total_money += 10000;
                        }
                        total_money += (avocado_cnt * 10000);
                        moneyHandler.sendEmptyMessage(0);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pb_array[index].setProgress(0);
                Log.d("로그", index + "번째 아보카도 쓰레드 종료");
            }
        }//run finish
    }//class avocado finish

}//main finish