# Vega Protocol - Market Maker

## :construction: :construction_worker_man: :construction:

**This is a new project and under active development. Some of the contents of this README might describe functionality that has not been implemented yet. When the first version is available for use, it will be published to the releases section of this repository as well as to Docker Hub.**

## Overview

This application implements an active market maker for Vega Protocol. The market maker can be configured to trade on a single market by tracking an external reference price and posting quotes on Vega around that price. The market maker updates its quotes once every 15 seconds.

You can track external prices from two sources:

* Crypto prices are taken from [Binance](https://binance.com)
* Non-crypto prices (e.g. equities, FX and commodities) are taken from [Polygon](https://polygon.io)

*Note: Support for Uniswap v3 will be added in the near future.*

### Pricing Strategy

The trading strategy implements a dynamic AMM curve, which is adapted based on the trader's open volume. The objective of the market maker is to quote prices with a spread around the external reference price while remaining neutral over a large sample trades. In order to achieve this goal, the algorithm skews its orders depending on whether it accumulates long or short exposure. If the trader accumulates long exposure, then bids will be quoted further away from the mid-price than asks, and if it accumulates short exposure, then asks will be quoted further away than bids.

By implementing an AMM curve, which distributes inventory between a price of 0 and infinity, we're able to ensure that (in theory) the market maker can never become distressed and is always able to quote prices on the given market. In practise, however, because positions on Vega are leveraged, it might happen that a trader becomes over exposed and in that scenario they should look to reduce their position size manually. 

Positions can be reduced by executing market orders on Vega against the orders of other market makers to reduce open volume, or, in the scenario where you are the dominate liquidity provider of a market, you would need to use an external market to hedge your position on Vega (e.g. if you are accumulating long BTCUSDT exposure on Vega, you could hedge on Binance with an equal-sized short position).

To better understand how the market maker quotes prices, you should review the [PricingUtils](https://github.com/MM0819/vega-market-maker/blob/main/src/main/java/com/vega/protocol/utils/PricingUtils.java) class, which implements the AMM curve. In the future, these docs will be updated with some graphical examples demonstrating how the pricing strategy works.

### Trading Configuration

You're able to override a variety of configuration parameters to control the behaviour of your market making strategy. This might typically be something you'd want to do if your market maker is accumulating too much unwanted exposure in a given direction, for example. 

The table below provides an extensive explanation of the purpose of each configuration parameter:

`[TODO - insert table]`

You can edit your market maker's configuration by visiting [http://localhost:7777](http://localhost:7777) in your web browser (if you are running the strategy on your local machine).

### Application Configuration

The following configuration parameters control the operation of your market maker:

`[TODO - insert table]`

The default values can be found in `{project_dir}/src/main/resources/application.properties`. To override these values, simply set an environment variable with the corresponding name. For example, to override `vega.ws.enabled=true`, you should define the environment variable `VEGA_WS_ENABLED=false`.

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

You can access the code coverage report at:

`{project_dir}/target/site/jacoco/index.html`

### Running the Application

First you need to configure your secret environment variables (see [.env.sample](https://github.com/MM0819/vega-market-maker/blob/main/.env.sample)).

Setup your environment: 

`./setup-env.sh`

Start the app: 

`java -jar target/simple-market-maker-1.0-SNAPSHOT.jar`
