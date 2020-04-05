<template>
    <v-app>
        <v-content>
            <v-container>
                <Header/>
                <v-card>
                    <v-tabs fixed-tabs style="margin-top: 56px" v-model="tab">
                        <v-tab>
                            <v-icon>multiline_chart</v-icon>
                            Coinbase
                        </v-tab>
                        <v-tab>
                            <v-icon>show_chart</v-icon>
                            Kraken
                        </v-tab>

                        <v-tab-item>
                            <trading-vue :color-back="colors.colorBack"
                                         :color-grid="colors.colorGrid"
                                         :color-text="colors.colorText"
                                         :data="dc"
                                         :height="this.height"
                                         :width="this.width"
                                         style="margin-top: 56px"/>

                        </v-tab-item>
                        <v-tab-item>

                        </v-tab-item>
                    </v-tabs>
                </v-card>
            </v-container>
        </v-content>
    </v-app>
</template>

<script>
    import TradingVue from 'trading-vue-js'
    import Header from "../components/Header.vue"
    import DataCube from "trading-vue-js/src/helpers/datacube";

    export default {
        name: 'App',
        components: {
            TradingVue,
            Header
        },
        methods: {
            onResize() {
                this.width = window.innerWidth * 0.8;
                this.height = window.innerHeight * 0.7
            }
        },
        mounted() {
            window.addEventListener('resize', this.onResize);
            let filterFun = (tickData) =>
                (tickData.payload.pair.baseCurrency == 'BTC'
                    && tickData.payload.pair.targetCurrency == 'USD');
            this.socket = new WebSocket("ws://localhost:8080/streaming");
            this.socket.onopen = () => {
                this.socket.onmessage = ({data}) => {
                    let tick = JSON.parse(data);

                    if (tick.exchange == 'COINBASE' && filterFun(tick)) {
                        this.messagesCount++;
                        let rate = tick.payload.rate;
                        let timestamp = Math.round(tick.timestamp / 1000) * 1000;
                        this.rateSum += rate;
                        if (this.messagesCount < 2) {
                            this.initialChart.push([timestamp, rate, rate, rate, rate, 0])
                        } else if (this.messagesCount == 2) {
                            console.log(this.initialChart);
                            this.dc = new DataCube({
                                chart: {
                                    type: "Spline",
                                    data: this.initialChart,
                                    settings: {
                                        lineWidth: 1.5
                                    }
                                }, onchart: [
                                    {
                                        name: "EMA, 5",
                                        type: "EMA",
                                        data: []
                                    }
                                ], offchart: []
                            })
                        } else {
                            if (this.messagesCount < 5) {
                                this.dc.update({
                                    candle: [timestamp, rate, rate, rate, rate, 0]
                                });
                            } else {
                                let emaValue = 0;
                                if (this.messagesCount == 5) {
                                    emaValue = this.rateSum / 5.0;
                                } else {
                                    emaValue = this.previousEma * 0.3 + rate * 0.7;
                                }
                                this.previousEma = emaValue;
                                this.dc.update({
                                    candle: [timestamp, rate, rate, rate, rate, 0],
                                    'EMA, 5': emaValue
                                });
                            }
                        }
                    }
                }
            }
        },
        beforeDestroy() {
            window.removeEventListener('resize', this.onResize);
            this.socket.close()
        },
        data() {
            return {
                tab: null,
                messagesCount: 0,
                initialChart: [],
                previousEma: 0,
                rateSum: 0,
                dc: new DataCube({
                    chart: {
                        type: "Spline",
                        data: []
                    }, onchart: [], offchart: []
                }),
                width: window.innerWidth * 0.8,
                height: window.innerHeight * 0.7,
                colors: {
                    colorBack: '#fff',
                    colorGrid: '#eee',
                    colorText: '#333',
                },
            }
        }
    }
</script>

<style>
</style>
