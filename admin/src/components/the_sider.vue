<template>
  <a-layout-sider width="200" style="background: #fff">
    <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        :style="{ height: '100%', borderRight: 0 }"
    >
      <a-menu-item key="/welcome">
        <router-link to="/welcome">
          <coffee-outlined /> &nbsp; 欢迎
        </router-link>
      </a-menu-item>
      <a-menu-item key="/about">
        <router-link to="/about">
          <user-outlined /> &nbsp; &nbsp; 关于
        </router-link>
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
</template>

<script>
import { defineComponent, ref, watch } from 'vue';
import { UserOutlined } from '@ant-design/icons-vue';
// import store from "@/store";
import router from "@/router";
// import { UserOutlined, LaptopOutlined, NotificationOutlined } from '@ant-design/icons-vue';
export default defineComponent({
  name: "the_sider_view",
  components: {
    UserOutlined,
    // LaptopOutlined,
    // NotificationOutlined,
  },
  setup() {
    const selectedKeys = ref([]);
    watch(() => router.currentRoute.value.path, (newValue) => {
      console.log('watch', newValue);
      selectedKeys.value = [];
      selectedKeys.value.push(newValue);
    }, {immediate: true});

    return {
      selectedKeys,
    };
  },
});
</script>

<style scoped>

</style>