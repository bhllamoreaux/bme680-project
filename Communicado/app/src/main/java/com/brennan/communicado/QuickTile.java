package com.brennan.communicado;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;

import java.util.Arrays;

public class QuickTile extends TileService {

    private PubNub pubnub;
    private PNConfiguration pnConfiguration;
    public QuickTile() {
    }

    @Override
    public void onClick() {
        super.onClick();

        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }

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
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }

        //prepare in case the user clicks the tile
        pnConfiguration = new PNConfiguration();

        //set the subscribe/publish keys
        pnConfiguration.setSubscribeKey("");
        pnConfiguration.setPublishKey("");
        pnConfiguration.setSecure(true);

        pubnub = new PubNub(pnConfiguration);

        //subscribes to the channel that the raspberry pi is broadcasting on
        pubnub.subscribe()
                .channels(Arrays.asList("communicado"))
                .execute();

    }
}
