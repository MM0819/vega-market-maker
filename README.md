# Vega Protocol - Market Maker

## Overview

This application implements an active market maker for Vega Protocol. The market maker can be configured to trade on a single market by tracking an external reference price and posting quotes on Vega around that price. The market maker updates its quotes once every 15 seconds.

You can track external prices from two sources:

* Crypto prices are taken from [Binance](https://binance.com)
* Non-crypto prices (e.g. equities, FX and commodities) are taken from [Polygon](https://polygon.io)

*Note: Support will be added for Uniswap v3 in the near future.*

### Pricing Strategy

The trading strategy implements a dynamic AMM curve, which is adapted based on the trader's open volume. The objective of the market maker is to quote prices with a spread around the external reference price while remaining neutral over a large sample trades. In order to achieve this goal, the algorithm skews its orders depending on whether it accumulates long or short exposure. If the trader accumulates long exposure, then bids will be quoted further away from the mid-price than asks, and if it accumulates short exposure, then asks will be quoted further away than bids.

By implementing an AMM curve, which distributes inventory between a price of 0 and infinity, we're able to ensure that (in theory) the market maker can never become distressed and is always able to quote prices on the given market. In practise, however, because positions on Vega are leveraged, it might happen that a trader becomes over exposed and in that scenario they should look to reduce their position size manually. Positions can be reduced by execute market orders on Vega against the orders of other market makers to reduce open volume, or, in the scenario where you are the dominate liquidity provider of a market, you would need to use an external market to hedge your position on Vega (e.g. if you are accumulating long BTCUSDT exposure on Vega, you could hedge on Binance with an equal-sized short position).

To better understand how the market maker quotes prices, you should review the [PricingUtils](https://github.com/MM0819/vega-market-maker/blob/main/src/main/java/com/vega/protocol/utils/PricingUtils.java) class, which implements the AMM curve. In future, these docs will be updated with some graphical examples demonstrating how the pricing strategy works.

### Configuration

You're able to override a variety of configuration parameters to control the behaviour of your market making strategy. This might typically be something you'd want to do if your market maker is accumulating too much unwanted exposure in a given direction, for example. 

The table below provides an extensive explanation of the purpose of each configuration parameter:

`[TODO - insert table]`

You can edit your market maker's configuration by visiting [http://localhost:7777](http://localhost:7777) in your web browser (if you are running the strategy on your local machine).

### Running the Market Maker

The easiest way to run the market maker is to pull the latest version from Docker Hub. You will need [Docker](https://www.docker.com) installed on your machine to do so.

Execute the command below to start the market maker with default confgiuration:

`TBC`

## Development

This application is built using [Java 16](https://www.oracle.com/java/technologies/javase/jdk16-archive-downloads.html), [Maven](https://maven.apache.org) and [Spring Boot](https://spring.io/projects/spring-boot) (`version 2.4.x`). The following instructions explain how to build the application for the purpose of running a market making strategy or contributing to the code base. Before you're able to run the application, you will also need to install and configure a [Vega wallet](https://github.com/vegaprotocol/vegawallet).

### Build

To build the application:

`mvn clean install -DskipTests`

### Test

To run the tests:

`mvn clean install`

You can access the code coverage report at :`{project_dir}/target/site/jacoco/index.html`

### Running the Application

First you need to configure your secret environment variables (see [.env.sample](https://github.com/MM0819/vega-market-maker/blob/main/.env.sample)).

Setup your environment: 

`./setup.sh`

Start the app: 

`java -jar target/simple-market-maker-1.0-SNAPSHOT.jar`
