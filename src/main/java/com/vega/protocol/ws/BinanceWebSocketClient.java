package com.vega.protocol.ws;

import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;

@Slf4j
public class BinanceWebSocketClient extends WebSocketClient {

    private final ReferencePriceStore referencePriceStore;

    public BinanceWebSocketClient(URI uri,
                                  ReferencePriceStore referencePriceStore) {
        super(uri);
        this.referencePriceStore = referencePriceStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        try {
            // TODO - fix this (sub to all symbols??)
//            JSONObject sub = new JSONObject()
//                    .put("method", "SUBSCRIBE")
//                    .put("params", new JSONArray().put(String.format("%s@ticker", symbol.toLowerCase(Locale.ROOT))))
//                    .put("id", 1);
//            this.send(sub.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            if(jsonObject.has("stream")) {
                JSONObject data = jsonObject.getJSONObject("data");
                double askPrice = data.getDouble("a");
                double bidPrice = data.getDouble("b");
                double askSize = data.getDouble("A");
                double bidSize = data.getDouble("B");
                double midPrice = (askPrice + bidPrice) / 2.0;
                ReferencePrice referencePrice = new ReferencePrice()
                        .setAskPrice(askPrice)
                        .setBidPrice(bidPrice)
                        .setAskSize(askSize)
                        .setBidSize(bidSize)
                        .setMidPrice(midPrice);
                referencePriceStore.update(referencePrice);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.error(reason);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(Exception e) {
        log.error(e.getMessage(), e);
    }
}
