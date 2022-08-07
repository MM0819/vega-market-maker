package com.vega.protocol.service;

import com.vega.protocol.constant.MarketSide;
import com.vega.protocol.model.LiquidityCommitment;
import com.vega.protocol.model.LiquidityCommitmentOffset;
import com.vega.protocol.model.Market;
import com.vega.protocol.utils.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Slf4j
public class OrderServiceTest {

    private OrderService orderService;
    private DecimalUtils decimalUtils;

    @BeforeEach
    public void setup() {
        decimalUtils = Mockito.mock(DecimalUtils.class);
        orderService = new OrderService(decimalUtils);
    }

    @Test
    public void testGetOtherSide() {
        Assertions.assertEquals(MarketSide.SELL, orderService.getOtherSide(MarketSide.BUY));
        Assertions.assertEquals(MarketSide.BUY, orderService.getOtherSide(MarketSide.SELL));
    }

    @Test
    public void testParseLiquidityOrdersFromGraphQL() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("vega-orders-ws.json")) {
            JSONObject ordersJson = new JSONObject(
                    IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8));
            JSONArray ordersArray = ordersJson
                    .getJSONObject("payload")
                    .getJSONObject("data")
                    .getJSONArray("orders")
                    .getJSONObject(0)
                    .getJSONObject("liquidityProvision")
                    .getJSONArray("buys");
            List<LiquidityCommitmentOffset> liquidityOrders =
                    orderService.parseLiquidityOrders(ordersArray, 5, true);
            Assertions.assertEquals(4, liquidityOrders.size());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assertions.fail();
        }
    }

    @Test
    public void testParseLiquidityOrdersFromREST() {
        try(InputStream is = getClass().getClassLoader()
                .getResourceAsStream("vega-liquidity-provisions-rest.json")) {
            JSONObject ordersJson = new JSONObject(
                    IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8));
            JSONArray ordersArray = ordersJson
                    .getJSONArray("liquidityProvisions")
                    .getJSONObject(0)
                    .getJSONArray("buys");
            List<LiquidityCommitmentOffset> liquidityOrders =
                    orderService.parseLiquidityOrders(ordersArray, 5, false);
            Assertions.assertEquals(2, liquidityOrders.size());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assertions.fail();
        }
    }
}