
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import OrderManager from "./components/OrderManager"

import MyPage from "./components/MyPage"
import OrdermgmtManager from "./components/OrdermgmtManager"

import DeliveryManager from "./components/DeliveryManager"

import PaymentManager from "./components/PaymentManager"

export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/Order',
                name: 'OrderManager',
                component: OrderManager
            },

            {
                path: '/MyPage',
                name: 'MyPage',
                component: MyPage
            },
            {
                path: '/Ordermgmt',
                name: 'OrdermgmtManager',
                component: OrdermgmtManager
            },

            {
                path: '/Delivery',
                name: 'DeliveryManager',
                component: DeliveryManager
            },

            {
                path: '/Payment',
                name: 'PaymentManager',
                component: PaymentManager
            },



    ]
})
