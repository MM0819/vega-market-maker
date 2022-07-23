package com.vega.protocol.ws;

import com.vega.protocol.model.ReferencePrice;
import com.vega.protocol.store.ReferencePriceStore;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;

@Slf4j
public class BinanceWebSocketClient extends WebSocketClient {

    private final String symbol;
    private final ReferencePriceStore referencePriceStore;

    public BinanceWebSocketClient(URI uri,
                                  String symbol,
                                  ReferencePriceStore referencePriceStore) {
        super(uri);
        this.symbol = symbol;
        this.referencePriceStore = referencePriceStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        try {
            JSONObject sub = new JSONObject()
                    .put("method", "SUBSCRIBE")
                    .put("params", new JSONArray().put(String.format("%s@ticker", symbol.toLowerCase(Locale.ROOT))))
                    .put("id", 1);
            this.send(sub.toString());
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
                BigDecimal askPrice = BigDecimal.valueOf(data.getDouble("a"));
                BigDecimal bidPrice = BigDecimal.valueOf(data.getDouble("b"));
                BigDecimal askSize = BigDecimal.valueOf(data.getDouble("A"));
                BigDecimal bidSize = BigDecimal.valueOf(data.getDouble("B"));
                BigDecimal midPrice = askPrice.add(bidPrice).multiply(BigDecimal.valueOf(0.5));
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
