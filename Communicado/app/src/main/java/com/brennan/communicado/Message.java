package com.brennan.communicado;

//container class for storing messages;
public class Message
{
    private Long timestamp;
    private String message;

    Message(Long timestamp, String message)
    {
        this.timestamp = timestamp;
        this.message = message;
    }

    Long getTimestamp()
    {
        return this.timestamp;
    }

    String getMessage()
    {
        return this.message;
    }


}