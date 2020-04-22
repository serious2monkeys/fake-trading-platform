import Vue from 'vue'
import App from './pages/App.vue'
import VueResource from 'vue-resource'
import 'vuetify/dist/vuetify.min.css'
import Vuetify from "vuetify"
import Login from "./pages/Login.vue";

Vue.use(Vuetify);
Vue.use(VueResource);

Vue.config.productionTip = false;

new Vue({
    el: '#app',
    vuetify: new Vuetify({}),
    render: a => a(App)
});

new Vue({
    el: '#login',
    vuetify: new Vuetify({}),
    render: a => a(Login)
});