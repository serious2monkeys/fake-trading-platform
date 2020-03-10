<template>
    <v-app>
        <v-content>
            <v-container>
                <Header/>
                <v-card>
                    <trading-vue :data="chart" :height="this.height" :width="this.width" style="margin-top: 56px"/>
                </v-card>
            </v-container>
        </v-content>
    </v-app>
</template>

<script>
    import TradingVue from 'trading-vue-js'
    import Header from "../components/Header.vue";

    export default {
        name: 'App',
        components: {
            TradingVue,
            Header
        },
        methods: {
            onResize() {
                this.width = window.innerWidth * 0.9;
                this.height = window.innerHeight * 0.9
            }
        },
        mounted() {
            window.addEventListener('resize', this.onResize)
        },
        beforeDestroy() {
            window.removeEventListener('resize', this.onResize)
        },
        data() {
            return {
                width: window.innerWidth * 0.9,
                height: window.innerHeight * 0.9,
                chart: {
                    ohlcv: [
                    ]
                },
                overlays: [{
                    "name": "Trades",
                    "type": "PerfectTrades",
                    "data": [
                        [
                            new Date().getMilliseconds- 50000, // timestamp (then trade occured)
                            1,             // state: 0 = idle, 1 = long
                            3973.0         // filled price
                        ],
                        [
                            new Date().getMilliseconds - 40000,
                            0,
                            4011.0
                        ],
                        [
                            new Date().getMilliseconds - 30000, // This is our buy
                            1,
                            4038.0
                        ],
                        [
                            new Date().getMilliseconds - 20000, // And this is obviously a sell
                            0,
                            4124.0
                        ]
                    ],
                    settings: {}
                }]
            }
        }
    }
</script>

<style>
</style>
