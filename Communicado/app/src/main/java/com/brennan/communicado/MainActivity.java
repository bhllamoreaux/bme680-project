package com.brennan.communicado;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.JsonObject;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.history.PNHistoryItemResult;
import com.pubnub.api.models.consumer.history.PNHistoryResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import com.pubnub.api.models.consumer.pubsub.PNSignalResult;
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class MainActivity extends AppCompatActivity {

    //variables
    PNConfiguration pnConfiguration;
    PubNub pubnub;
    TextView bme_temp;
    TextView bme_humidity;
    TextView temp_time;
    TextView temp_low;
    TextView temp_high;
    TextView humid_high, humid_low, humid_time;
    TextView press_high, press_low, press_time, bme_press;
    TextView gas_high, gas_time, bme_gas, gas_low;
    TextView x_value,y_value;
    LinkedList<Message> messages;
    LinkedList<Datapoint> temps, humids, pressures, gases;
    LinearLayout tempLayout, humidLayout, pressureLayout, gasLayout;
    LineChart chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Internet not granted", Toast.LENGTH_LONG).show();
        }

        init();

        //get all of the data from the last 3 days
        //this is all stored in pubnub's history/on their servers
        //puts it in the messages linked list
        //also calls extractData();
        getHistory();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.light:
                //send the message to toggle the light
                pubnub.publish()
                        .message("button pressed")
                        .channel("communicado")
                        .async(new PNCallback<PNPublishResult>() {
                            @Override
                            public void onResponse(PNPublishResult result, PNStatus status) {
                                //handle errors
                            }
                        });
                return true;
                

            case R.id.refresh:
                //refresh the data
                getHistory();
                Toast.makeText(MainActivity.this,"Refreshing...", Toast.LENGTH_LONG).show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

// handle



    //this function just initializes all of the various views and variables
    public void init()
    {
        //configure pubnub, initialize and set listeners to receive data
        pnConfiguration = new PNConfiguration();
        pubnub = initPubnub(pnConfiguration);
        setListener(pubnub);

        //initialize linked lists
        messages = new LinkedList<>();
        temps = new LinkedList<>();
        humids = new LinkedList<>();
        pressures = new LinkedList<>();
        gases = new LinkedList<>();

        //initialize textviews
        bme_temp = findViewById(R.id.bme_temp);
        bme_humidity = findViewById(R.id.bme_humid);
        temp_time = findViewById(R.id.temp_time);
        temp_low = findViewById(R.id.temp_low);
        temp_high = findViewById(R.id.temp_high);

        humid_high = findViewById(R.id.humid_high);
        humid_low = findViewById(R.id.humid_low);
        humid_time = findViewById(R.id.humid_time);

        press_high = findViewById(R.id.press_high);
        press_low = findViewById(R.id.press_low);
        press_time = findViewById(R.id.press_time);
        bme_press = findViewById(R.id.bme_press);

        gas_high = findViewById(R.id.gas_high);
        gas_low = findViewById(R.id.gas_low);
        gas_time = findViewById(R.id.gas_time);
        bme_gas = findViewById(R.id.bme_gas);

        //initialize panels
        tempLayout = findViewById(R.id.temp_layout);
        humidLayout = findViewById(R.id.humid_layout);
        pressureLayout = findViewById(R.id.pressure_layout);
        gasLayout = findViewById(R.id.gas_layout);

        //initialize the chart views
        chart = findViewById(R.id.chart);
        x_value = findViewById(R.id.x_value);
        y_value = findViewById(R.id.y_value);
    }

    public PubNub initPubnub(PNConfiguration pnConfiguration) {

        //set the subscribe/publish keys
        pnConfiguration.setSubscribeKey("");
        pnConfiguration.setPublishKey("");
        pnConfiguration.setSecure(true);

        PubNub pubnub = new PubNub(pnConfiguration);

        //subscribes to the channel that the raspberry pi is broadcasting on
        pubnub.subscribe()
                .channels(Arrays.asList("communicado"))
                .execute();

        return pubnub;
    }

    public void setListener(PubNub pubnub) {


        pubnub.addListener(new SubscribeCallback() {

            @Override
            public void status(PubNub pubnub, PNStatus pnStatus) {
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult pnMessageResult) {
                // print basic info about newly received messages
                System.out.println("Message channel: " + pnMessageResult.getChannel());
                System.out.println("Message publisher: " + pnMessageResult.getPublisher());
                System.out.println("Message content: " + pnMessageResult.getMessage());

            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult pnPresenceEventResult) {
                // print basic info about newly received presence events
                System.out.println("Presence channel: " + pnPresenceEventResult.getChannel());
                System.out.println("Presence event: " + pnPresenceEventResult.getEvent());
                System.out.println("Presence uuid: " + pnPresenceEventResult.getUuid());
            }

            @Override
            public void signal(PubNub pubnub, PNSignalResult pnSignalResult) {
            }

            @Override
            public void user(PubNub pubnub, PNUserResult pnUserResult) {
            }

            @Override
            public void space(PubNub pubnub, PNSpaceResult pnSpaceResult) {
            }

            @Override
            public void membership(PubNub pubnub, PNMembershipResult pnMembershipResult) {
            }

            @Override
            public void messageAction(PubNub pubnub, PNMessageActionResult pnMessageActionResult) {

            }
        });

        pubnub.subscribe()
                .channels(Arrays.asList("communicado"))
                .withPresence() // to receive presence events
                .execute();

        JsonObject message = new JsonObject();
        message.addProperty("sender", pnConfiguration.getUuid());
        message.addProperty("text", "Hello From Java SDK");

        pubnub.publish()
                .message(message)
                .channel("communicado")
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        if (!status.isError()) {
                            System.out.println("Message timetoken: " + result.getTimetoken());
                        } else {
                            status.getErrorData().getThrowable().printStackTrace();
                        }
                    }
                });

        pubnub.history()
                .channel("communicado")
                .count(10)
                .includeTimetoken(true)
                .async(new PNCallback<PNHistoryResult>() {
                    @Override
                    public void onResponse(PNHistoryResult result, PNStatus status) {
                        if (!status.isError()) {
                            for (PNHistoryItemResult historyItem : result.getMessages()) {
                                Log.d("fuck", "Message content: " + historyItem.getEntry());
                            }
                            System.out.println("Start timetoken: " + result.getStartTimetoken());
                            System.out.println("End timetoken: " + result.getEndTimetoken());
                        } else {
                            status.getErrorData().getThrowable().printStackTrace();
                        }
                    }
                });
    }

    //pubnub keys only hold data for 3 days at the moment, so might as well just get all of it
    //unfortunately, this may result in getting some useless info as well
    public void getHistory() {
        //gets all of the messages from the last few days
        PubNubRecursiveHistoryFetcher fetcher = new PubNubRecursiveHistoryFetcher();
        fetcher.getAllMessages("communicado", null, 100, new PubNubRecursiveHistoryFetcher.CallbackSkeleton() {
            @Override
            public void handleResponse(PNHistoryResult result) {
                for (PNHistoryItemResult message : result.getMessages()) {
                    Message msg = new Message(message.getTimetoken(), message.getEntry().toString());
                    messages.add(msg);
                }

                //if there is no more data, sort through it
                if (result.getMessages().size() < 100) {

                    //sort all of the messages to be in chronological order,
                    // since they weren't in order before
                    Collections.sort(messages, new Comparator<Message>() {
                        @Override
                        public int compare(Message msg1, Message msg2) {
                            return msg1.getTimestamp().compareTo(msg2.getTimestamp());
                        }
                    });

                    //organize the data from the raw messages
                    extractData();

                    //setup the different clickable elements
                    initClickable();

                    //initialize the graph
                    displayChart(temps, "Temperatures");

                    //change the elevations so theres a visual of which graph you are using
                    tempLayout.setElevation(20);
                    humidLayout.setElevation(10);
                    pressureLayout.setElevation(10);
                    gasLayout.setElevation(10);
                }
            }
        });

    }


    //function to separate up and manipulate the data, preparing for setup
    public void extractData() {
        Message item;
        ListIterator<Message> iterator = messages.listIterator(0);
        while (iterator.hasNext()) {
            item = iterator.next();

            if (item.getMessage().contains("bme_temp")) {
                //add the temperature to the storage, but first convert to fahrenheit
                float fahrenheit =  Float.parseFloat(item.getMessage().substring(9, item.getMessage().length() - 1));
                fahrenheit *= 1.8;
                fahrenheit += 32;

                Datapoint datapoint = new Datapoint(item.getTimestamp(), fahrenheit);
                temps.add(datapoint);
            } else if (item.getMessage().contains("bme_humidity")) {
                //add the humidity to the storage
                Datapoint datapoint = new Datapoint(item.getTimestamp(),
                        Float.parseFloat(item.getMessage().substring(13, item.getMessage().length() - 1)));
                humids.add(datapoint);
            } else if (item.getMessage().contains("bme_pressure")) {
                //add the pressure to the storage
                Datapoint datapoint = new Datapoint(item.getTimestamp(),
                        Float.parseFloat(item.getMessage().substring(13, item.getMessage().length() - 1)));
                pressures.add(datapoint);
            } else if (item.getMessage().contains("bme_gas")) {
                //add the pressure to the storage
                Datapoint datapoint = new Datapoint(item.getTimestamp(),
                        Float.parseFloat(item.getMessage().substring(8, item.getMessage().length() - 1)));
                gases.add(datapoint);
            }
        }

        initTextViews();

    }

    public void initClickable() {
        tempLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change the chart to temperatures
                displayChart(temps,"Temperatures");

                //change the elevations so theres a visual of which graph you are using
                tempLayout.setElevation(20);
                humidLayout.setElevation(10);
                pressureLayout.setElevation(10);
                gasLayout.setElevation(10);
            }
        });


        humidLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change the chart to humidity
                displayChart(humids,"Humidity");
                humidLayout.setElevation(20);
                tempLayout.setElevation(10);
                pressureLayout.setElevation(10);
                gasLayout.setElevation(10);
            }
        });

        pressureLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change the chart to pressure
                displayChart(pressures,"Pressure");
                humidLayout.setElevation(10);
                tempLayout.setElevation(10);
                pressureLayout.setElevation(20);
                gasLayout.setElevation(10);
            }
        });

        gasLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change the chart to gas resistance level
                displayChart(gases,"Gas Resistance");

                humidLayout.setElevation(10);
                tempLayout.setElevation(10);
                pressureLayout.setElevation(10);
                gasLayout.setElevation(20);
            }
        });
    }


    //this is a simple function to convert a linkedlist of Floats to a float array
    public float[] convertFloat(LinkedList<Datapoint> list) {
        float[] converted = new float[list.size()];
        ListIterator<Datapoint> iterator = list.listIterator(0);
        int i = 0;

        while (iterator.hasNext()) {
            converted[i] = iterator.next().getDatapoint();
            i++;
        }

        return converted;
    }

    //this is a simple function to convert a linkedlist of datapoints to a long array of timestamps
    public long[] timestampToArray(LinkedList<Datapoint> list) {
        long[] converted = new long[list.size()];
        ListIterator<Datapoint> iterator = list.listIterator(0);
        int i = 0;

        while (iterator.hasNext()) {
            converted[i] = iterator.next().getTimestamp();
            i++;
        }

        return converted;
    }

    //sets the initial text values of the textviews
    public void initTextViews() {
        //set the textview to be the last reading
        bme_temp.setText("Last reading: " + temps.getLast().getDatapoint() + "°F");
        temp_time.setText(new Date(temps.getLast().getTimestamp() / 10000).toString());
        temp_low.setText("3 day low: " + getLow(temps) + "°F");
        temp_high.setText("3 day high: " + getHigh(temps) + "°F");

        bme_humidity.setText("Last reading: " + humids.getLast().getDatapoint() + "%");
        humid_time.setText(new Date(humids.getLast().getTimestamp() / 10000).toString());
        humid_low.setText("3 day low: " + getLow(humids) + "%");
        humid_high.setText("3 day high: " + getHigh(humids) + "%");

        bme_press.setText("Last reading: " + pressures.getLast().getDatapoint() + " hPa");
        press_time.setText(new Date(pressures.getLast().getTimestamp() / 10000).toString());
        press_low.setText("3 day low: " + getLow(pressures) + " hPa");
        press_high.setText("3 day high: " + getHigh(pressures) + " hPa");

        bme_gas.setText("Last reading: " + gases.getLast().getDatapoint() + " Ω");
        gas_time.setText(new Date(gases.getLast().getTimestamp() / 10000).toString());
        gas_low.setText("3 day low: " + getLow(gases) + " Ω");
        gas_high.setText("3 day high: " + getHigh(gases) + " Ω");
    }

    //function to get the highest value from the linked list
    public float getHigh(LinkedList<Datapoint> dp) {
        float max = Float.MIN_VALUE;
        for (Datapoint datapoint : dp) {
            if (max < datapoint.getDatapoint())
                max = datapoint.getDatapoint();
        }

        return max;
    }

    //function to get the highest value from the linked list
    public float getLow(LinkedList<Datapoint> dp) {
        float min = Float.MAX_VALUE;
        for (Datapoint datapoint : dp) {
            if (min > datapoint.getDatapoint())
                min = datapoint.getDatapoint();
        }

        return min;
    }

    //sets up the graph for the specified data
    public void displayChart(LinkedList<Datapoint> data, String label) {

        List<Entry> entries = new ArrayList<Entry>();
        LineDataSet dataSet;
        LineData lineData;


        //convert the datapoints to the correct format
        for (Datapoint d : data) {
            entries.add(new Entry(d.getTimestamp(), d.getDatapoint()));
        }


        dataSet = new LineDataSet(entries, label); // add entries to dataset
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.GREEN); //set colors

        lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh

        //get rid of the axis labels
        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawLabels(false);

        //allow scaling
        chart.setScaleEnabled(true);

        //allow scrubbing and show values
        chart.setHighlightPerDragEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                y_value.setText(String.valueOf(e.getY()));
                x_value.setText(new Date((long) e.getX() / 10000).toString());
            }

            @Override
            public void onNothingSelected() {
                //dont care
            }
        });



    }

}




