<template>
  <a-layout id="components-layout-demo-top-side-2">
    <The_header></The_header>
    <a-layout>
      <The_sider_view></The_sider_view>
      <a-layout style="padding: 0 24px 24px">
        <a-layout-content
            :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px' }"
        >
          <router-view/>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>
<script>
import {defineComponent} from 'vue';
import The_header from "@/components/the_header";
import The_sider_view from "@/components/the_sider";

export default defineComponent({
  components: {
    The_sider_view,
    The_header,
  },
  setup() {
  },
});
</script>
<style>

</style>