import { createRouter, createWebHistory } from 'vue-router'
import store from "../store/index.js"
import {notification} from "ant-design-vue";


const routes = [
  {
    path: '/login',
    name: 'login',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "about" */ '../views/the_login.vue')
  },
  {
    path: '/',
    name: 'main-center',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "about" */ '../views/main-center.vue'),
    meta:{
      loginRequired: true
    },
    children: [{
      path:'welcome',
      component: () => import('../views/main/welcome.vue'),
    },{
      path:'passenger',
      component: () => import('../views/main/passenger.vue'),
    }
    ]
  },
  {
    path:'',
    redirect:'/welcome'
  }

]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router

// 路由登录拦截
router.beforeEach((to, from, next) => {
  // 对所有路由进行判断，根据loginRequired判断是否需要拦截
  if (to.matched.some(function (item) {
    console.log((item, "是否需要登录校验：", item.meta.loginRequired || false));
    return item.meta.loginRequired;
  })) {
    // 需要校验
    const _member = store.state.member;
    console.log("登录校验开始：", _member);
    if (!_member.token) {
      // 校验失败
      console.log("未登录，跳转到登录页面");
      notification.error({description: "未登录或登录超时"});
      next("/login");
    } else {
      // 校验成功
      next();
    }
  } else {
    // 不需要校验
    next();
  }
});
