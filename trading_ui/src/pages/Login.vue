<template>
    <v-app>
        <v-content>
            <v-container class="fill-height" fluid>
                <v-row align="center" justify="center">
                    <v-col cols="12" md="3" sm="8">
                        <v-card class="elevation-12">
                            <v-toolbar color="primary" dark flat>
                                <v-toolbar-title>Enter your username and password</v-toolbar-title>
                            </v-toolbar>
                            <v-spacer/>
                            <v-card-text>
                                <v-form action="/login" method="post">
                                    <v-text-field label="Username" name="username" prepend-icon="person" type="text"/>
                                    <v-text-field label="Password" name="password" prepend-icon="lock" type="password"/>
                                    <div class="errorText" style="visibility: hidden">Invalid username or password</div>
                                    <v-spacer/>
                                    <v-btn color="primary" type="submit">Login</v-btn>
                                </v-form>
                            </v-card-text>
                        </v-card>
                    </v-col>
                </v-row>
            </v-container>
        </v-content>
    </v-app>
</template>

<script>
    export default {
        name: "Login",
        created: function () {
            window.onload = function () {
                let location = document.location.href;
                if (location.indexOf('?error') !== -1) {
                    document.querySelector('.errorText').style.visibility = '';
                }
                let loginInput = document.querySelector('input[name="login"]');
                if (loginInput) {
                    loginInput.focus()
                }
            }
        }
    }
</script>

<style scoped>
</style>