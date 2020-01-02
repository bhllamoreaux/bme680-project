package com.brennan.communicado;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.history.PNHistoryItemResult;
import com.pubnub.api.models.consumer.history.PNHistoryResult;

import java.util.LinkedList;


/* Code pulled from pubnub documentation to get old messages/retrieve the past data */
public class PubNubRecursiveHistoryFetcher {

    private PubNub pubnub;
    boolean finished = false;

    public static abstract class CallbackSkeleton {
        public abstract void handleResponse(PNHistoryResult result);
    }

    PubNubRecursiveHistoryFetcher() {
        // NOTICE: for demo/demo pub/sub keys Storage & Playback is disabled,
        // so use your pub/sub keys instead
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey("sub-c-");
        pubnub = new PubNub(pnConfiguration);
    }

    public static void main(String[] args) {
        PubNubRecursiveHistoryFetcher fetcher = new PubNubRecursiveHistoryFetcher();
        fetcher.getAllMessages("communicado", null, 100, new CallbackSkeleton() {
            @Override
            public void handleResponse(PNHistoryResult result) {
                for (PNHistoryItemResult message : result.getMessages()) {
                    //do whatever
                }
            }
        });
    }

    /**
     * Fetches channel history in a recursive manner, in chunks of specified size, starting from the most recent,
     * with every subset (with predefined size) sorted by the timestamp the messages were published.
     *
     * @param channel  The channel where to fetch history from
     * @param start    The timetoken which the fetching starts from
     * @param count    Chunk size
     * @param callback Callback which fires when a chunk is fetched
     */
    void getAllMessages(final String channel, Long start, final int count, final CallbackSkeleton callback) {
        pubnub.history()
                .channel(channel)
                .count(count)
                .start(start)
                .includeTimetoken(true)
                .async(new PNCallback<PNHistoryResult>() {
                    @Override
                    public void onResponse(PNHistoryResult result, PNStatus status) {
                        if (!status.isError() && !result.getMessages().isEmpty()) {
                            callback.handleResponse(result);
                            getAllMessages(channel, result.getMessages().get(0).getTimetoken(), count, callback);
                            if(result.getMessages().size() < 100)
                                finished = true;
                        }
                    }
                });
    }

    public boolean isFinished()
    {
        return this.finished;
    }

}
