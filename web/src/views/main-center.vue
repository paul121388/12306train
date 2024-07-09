<template>
  <a-layout id="components-layout-demo-top-side-2">
    <The_header></The_header>
    <a-layout>
      <The_sider_view></The_sider_view>
      <a-layout style="padding: 0 24px 24px">
        <a-breadcrumb style="margin: 16px 0">
          <a-breadcrumb-item>Home</a-breadcrumb-item>
          <a-breadcrumb-item>List</a-breadcrumb-item>
          <a-breadcrumb-item>App</a-breadcrumb-item>
        </a-breadcrumb>
        <a-layout-content
            :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px' }"
        >
          所有会员总数：{{count}}
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>
<script>
import {defineComponent, ref} from 'vue';
import The_header from "@/components/the_header";
import The_sider_view from "@/components/the_sider";
import axios from "axios";
import {notification} from "ant-design-vue";

export default defineComponent({
  components: {
    The_sider_view,
    The_header,
  },
  setup() {
    const count = ref(0);
    axios.get("/member/member/count").then((response) => {
      let data = response.data;
      if (data.success) {
        count.value = data.content;
      } else {
        notification.error({ description: data.message });
      }
    })
    return {
      count,
      collapsed: ref(false),
    };
  },
});
</script>
<style>
#components-layout-demo-top-side-2 .logo {
  float: left;
  width: 120px;
  height: 31px;
  margin: 16px 24px 16px 0;
  background: rgba(255, 255, 255, 0.3);
}

.ant-row-rtl #components-layout-demo-top-side-2 .logo {
  float: right;
  margin: 16px 0 16px 24px;
}

.site-layout-background {
  background: #fff;
}
</style>