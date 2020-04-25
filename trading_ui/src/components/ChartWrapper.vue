<template>
    <trading-vue :color-back="colors.colorBack"
                 :color-grid="colors.colorGrid"
                 :color-text="colors.colorText"
                 :data="dataCube"
                 :height="height"
                 :width="width"
                 ref="chart"
                 style="margin: 6px 10px 10px; padding-left: 20px"/>
</template>

<script>
    import DataCube from "trading-vue-js/src/helpers/datacube";
    import TradingVue from 'trading-vue-js'

    export default {
        name: "ChartWrapper",
        components: {TradingVue},
        methods: {
            refresh() {
                this.$refs["chart"].resetChart()
            }
        },
        props: {
            dataCube: DataCube,
            width: Number,
            height: Number,
            colors: Object
        }
    }
</script>

<style scoped>

</style>