<template>
    <v-app>
        <v-content>
            <v-container>
                <Header/>
                <v-col cols="2" sm="3" style="margin-top: 32px; padding-bottom: 0">
                    <v-select
                            :items="['BTC/USD', 'BTC/EUR', 'ETH/USD', 'ETH/EUR']"
                            @change="onPairChange(this)"
                            label="Currency pair"
                            v-model="selectedPair"
                            value="BTC/USD"
                    />
                </v-col>
                <v-card>
                    <v-tabs fixed-tabs v-model="tab">
                        <v-tab>
                            <v-icon>multiline_chart</v-icon>
                            Coinbase
                        </v-tab>
                        <v-tab>
                            <v-icon>show_chart</v-icon>
                            x` x
                            Kraken
                        </v-tab>

                        <v-tab-item>
                            <div class="text-center ma-2">
                                <v-chip color="red" text-color="white">
                                    {{currentCoinbaseRate}}
                                </v-chip>
                            </div>
                            <ChartWrapper
                                    :colors="colors"
                                    :data-cube="coinbaseDataCube"
                                    :height="height"
                                    :width="width"
                                    ref="coinbaseChart"
                            />
                            <v-divider/>
                        </v-tab-item>
                        <v-tab-item>
                            <div class="text-center ma-2">
                                <v-chip color="red" text-color="white">
                                    {{currentKrakenRate}}
                                </v-chip>
                            </div>
                            <ChartWrapper
                                    :colors="colors"
                                    :data-cube="krakenDataCube"
                                    :height="height"
                                    :width="width"
                                    ref="krakenChart"
                            />
                            <v-divider/>
                        </v-tab-item>
                    </v-tabs>
                </v-card>
                <v-divider/>
                <v-card>
                    <v-data-table :headers="ratesTableData.headers"
                                  :items="ratesTableData.values" :items-per-page=5
                                  :sort-desc="true"
                                  class="elevation-1"
                                  dense
                                  item-key="timestamp"
                                  light
                                  sort-by="timestamp"/>
                </v-card>
            </v-container>
        </v-content>
    </v-app>
</template>

<script>
import Header from "../components/Header.vue"
import DataCube from "trading-vue-js/src/helpers/datacube";
import ChartWrapper from "../components/ChartWrapper.vue";

export default {
    name: 'App',
    components: {
        ChartWrapper,
        Header
    },
    methods: {
        onResize() {
            this.width = window.innerWidth * 0.8 - 20;
            this.height = window.innerHeight * 0.4
        },
        onPairChange() {
            console.log("Switched to " + this.selectedPair);
            this.coinbaseDataCube = this.chartsData["COINBASE"][this.selectedPair].dataCube;
            this.currentCoinbaseRate = this.chartsData["COINBASE"][this.selectedPair].currentRate;

            this.krakenDataCube = this.chartsData["KRAKEN"][this.selectedPair].dataCube;
            this.currentKrakenRate = this.chartsData["KRAKEN"][this.selectedPair].currentRate;

            this.$refs["coinbaseChart"]?.refresh();
            this.$refs["krakenChart"]?.refresh()
        },
        extractPairRepresentation(pairObj) {
            return pairObj.baseCurrency + '/' + pairObj.targetCurrency
        },
        readPriceFromHistory(priceMessage) {
            let priceValid = priceMessage.exchange && priceMessage.pair && priceMessage.rate && priceMessage.timestamp;
            if (priceValid) {
                let rate = priceMessage.rate;
                let timestamp = Math.round(new Date(priceMessage.timestamp).getTime() / 1000) * 1000;
                let targetExchangeData = this.chartsData[priceMessage.exchange];
                if (!targetExchangeData) {
                    targetExchangeData = this.chartsData[priceMessage.exchange] = {}
                }
                let pair = this.extractPairRepresentation(priceMessage.pair);
                this.ratesTableData.values.push({
                    timestamp: new Date(timestamp).toISOString(),
                    pair,
                    exchange: priceMessage.exchange,
                    rate
                });
                let pairData = targetExchangeData[pair];
                if (!pairData) {
                    pairData = targetExchangeData[pair] = {
                        initialRates: [],
                        currentRate: 0
                    };
                }
                pairData.initialRates.push([timestamp, rate, rate, rate, rate, 0]);
                pairData.currentRate = rate;
            }
        },
        appendPriceData(tick) {
            let tickValid = tick.exchange && tick.payload.pair && tick.payload.rate && tick.timestamp;
            if (tickValid) {
                let rate = tick.payload.rate;
                let timestamp = Math.round(tick.timestamp / 1000) * 1000;
                let targetExchangeData = this.chartsData[tick.exchange];
                if (!targetExchangeData) {
                    targetExchangeData = this.chartsData[tick.exchange] = {}
                }
                let pair = this.extractPairRepresentation(tick.payload.pair);
                this.ratesTableData.values.push({
                    timestamp: new Date(timestamp).toISOString(),
                    pair,
                    exchange: tick.exchange,
                    rate
                });
                let pairData = targetExchangeData[pair];
                if (!pairData) {
                    pairData = targetExchangeData[pair] = {
                        initialRates: [],
                        currentRate: 0
                    };
                }
                pairData.initialRates.push([timestamp, rate, rate, rate, rate, 0]);
                pairData.currentRate = rate;

                if (pairData.initialRates.length === 2) {
                    pairData.dataCube = new DataCube({
                        chart: {
                            type: "Spline",
                            data: pairData.initialRates,
                            settings: {
                                lineWidth: 1.5
                            }
                        },
                        onchart: [],
                        offchart: []
                    });
                } else if (pairData.initialRates.length > 2) {
                    pairData.dataCube.update({
                        candle: [timestamp, rate, rate, rate, rate, 0]
                    });
                }

                this.coinbaseDataCube = this.chartsData["COINBASE"][this.selectedPair].dataCube;
                this.currentCoinbaseRate = this.chartsData["COINBASE"][this.selectedPair].currentRate;

                this.krakenDataCube = this.chartsData["KRAKEN"][this.selectedPair].dataCube;
                    this.currentKrakenRate = this.chartsData["KRAKEN"][this.selectedPair].currentRate;
                }
            }
        },
        mounted() {
            window.addEventListener('resize', this.onResize);
            this.chartsData = {
                "KRAKEN": {
                    "BTC/USD": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    },
                    "BTC/EUR": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    },
                    "ETH/USD": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    },
                    "ETH/EUR": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    }
                },
                "COINBASE": {
                    "BTC/USD": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    },
                    "BTC/EUR": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    },
                    "ETH/USD": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    },
                    "ETH/EUR": {
                        "dataCube": new DataCube({
                            chart: {
                                type: "Spline",
                                data: []
                            }, onchart: [], offchart: []
                        }),
                        "currentRate": 0,
                        "initialRates": []
                    }
                }
            };
            this.coinbaseDataCube = this.chartsData["COINBASE"]["BTC/USD"].dataCube;
            this.krakenDataCube = this.chartsData["KRAKEN"]["BTC/USD"].dataCube;
            this.$resource("/candles").get().then(response => {
                if (response.ok) {
                    response.json().then(parsedResponse => {
                        parsedResponse.forEach(priceMessage => this.readPriceFromHistory(priceMessage))
                        for (let chartData of Object.keys(this.chartsData)) {
                            if (this.chartsData[chartData]) {
                                let chart = this.chartsData[chartData]
                                for (let pairName of Object.keys(chart)) {
                                    if (this.chartsData[chartData][pairName].initialRates) {
                                        this.chartsData[chartData][pairName].dataCube = new DataCube({
                                            chart: {
                                                type: "Spline",
                                                data: this.chartsData[chartData][pairName].initialRates,
                                                settings: {
                                                    lineWidth: 1.5
                                                }
                                            },
                                            onchart: [],
                                            offchart: []
                                        });
                                    }
                                }
                            }
                        }

                        this.coinbaseDataCube = this.chartsData["COINBASE"][this.selectedPair].dataCube;
                        this.currentCoinbaseRate = this.chartsData["COINBASE"][this.selectedPair].currentRate;

                        this.krakenDataCube = this.chartsData["KRAKEN"][this.selectedPair].dataCube;
                        this.currentKrakenRate = this.chartsData["KRAKEN"][this.selectedPair].currentRate;

                        this.socket = new WebSocket("ws://localhost:8080/streaming");
                        this.socket.onopen = () => {
                            this.socket.onmessage = ({data}) => {
                                let message = JSON.parse(data);
                                if ('PRICE_UPDATE' === message.type) {
                                    this.appendPriceData(message);
                                }
                            }
                        };
                    });
                }
            });
        },
        beforeDestroy() {
            window.removeEventListener('resize', this.onResize);
            this.socket.close()
        },
        data() {
            return {
                tab: null,
                selectedPair: "BTC/USD",
                currentCoinbaseRate: 0,
                currentKrakenRate: 0,
                chartsData: {},
                coinbaseDataCube: {},
                krakenDataCube: {},
                width: window.innerWidth * 0.8 - 20,
                height: window.innerHeight * 0.4,
                colors: {
                    colorBack: '#fff',
                    colorGrid: '#eee',
                    colorText: '#333',
                },
                ratesTableData: {
                    headers: [
                        {
                            text: 'Timestamp',
                            align: 'start',
                            sortable: true,
                            value: 'timestamp',
                        },
                        {
                            text: 'Currency Pair',
                            value: 'pair'
                        },
                        {
                            text: 'Exchange',
                            value: 'exchange'
                        },
                        {
                            text: 'Rate',
                            value: 'rate'
                        }
                    ],
                    values: []
                }
            }
        }
    }
</script>

<style>
</style>
