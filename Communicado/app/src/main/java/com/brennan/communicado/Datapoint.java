package com.brennan.communicado;

//container class for storing data points;
public class Datapoint
{
    private Long timestamp;
    private float datapoint;

    Datapoint(Long timestamp, float datapoint)
    {
        this.timestamp = timestamp;
        this.datapoint = datapoint;
    }

    Long getTimestamp()
    {
        return this.timestamp;
    }

    float getDatapoint()
    {
        return this.datapoint;
    }


}