<template>
    <v-app-bar dense fixed>
        <v-app-bar-nav-icon/>
        <v-toolbar-title>
            Fake Crypto Trading Platform
        </v-toolbar-title>
        <v-spacer/>
        <v-btn href="/logout" icon>
            <v-icon>exit_to_app</v-icon>
        </v-btn>
    </v-app-bar>
</template>

<script>
    export default {
        name: "Header"
    }
</script>

<style scoped>

</style>