<template>
  <a-select v-model:value="name" show-search allowClear
            :filterOption="filterNameOption"
            @change="onChange" placeholder="请选择站名"
            :style="'width: ' + localWidth">
    <a-select-option v-for="item in stations" :key="item.name" :value="item.name" :label="item.name + item.namePinyin + item.namePy">
      {{item.name}} {{item.namePinyin}} ~ {{item.namePy}}
    </a-select-option>
  </a-select>
</template>

<script>

import {defineComponent, onMounted, ref, watch} from 'vue';
import axios from "axios";
import {notification} from "ant-design-vue";

export default defineComponent({
  name: "station-select-view",
  props: ["modelValue", "width"],
  emits: ['update:modelValue', 'change'],
  setup(props, {emit}) {
    const name = ref();
    const stations = ref([]);
    const localWidth = ref(props.width);

    // let loading = ref(false);

    if (Tool.isEmpty(props.width)) {
      localWidth.value = "100%";
    }

    // 利用watch，动态获取父组件的值，如果放在onMounted或其它方法里，则只有第一次有效
    watch(() => props.modelValue, ()=>{
      console.log("props.modelValue", props.modelValue);
      name.value = props.modelValue;
    }, {immediate: true});

    /**
     * 查询所有的站名，用于站名下拉框
     */
    /*const queryAllTrain = () => {
      let list = SessionStorage.get(SESSION_ALL_TRAIN);
      if (Tool.isNotEmpty(list)) {
        console.log("queryAllTrain 读取缓存");
        stations.value = list;
      } else {
        axios.get("/business/admin/station/query-all").then((response) => {
          let data = response.data;
          if (data.success) {
            stations.value = data.content;
            console.log("queryAllTrain 保存缓存");
            SessionStorage.set(SESSION_ALL_TRAIN, stations.value);
          } else {
            notification.error({description: data.message});
          }
        });
      }
    };*/
    const queryName = () => {
      axios.get("/business/admin/station/query-all",).then((response) => {
        // loading.value = false;
        let data = response.data;
        if (data.success) {
          stations.value = data.content;
        } else {
          notification.error({description: data.message});
        }
      });
    };

    /**
     * 站名下拉框筛选
     */
    const filterNameOption = (input, option) => {
      console.log(input, option);
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
    };

    /**
     * 将当前组件的值响应给父组件
     * @param value
     */
    const onChange = (value) => {
      emit('update:modelValue', value);
      // let station = stations.value.filter(item => item.code === value)[0];
      // if (Tool.isEmpty(station)) {
      //   station = {};
      // }
      // emit('change', station);
    };

    onMounted(() => {
      // queryAllTrain();
      queryName();
    });

    return {
      name,
      stations,
      filterNameOption,
      onChange,
      localWidth,
      queryName,
      // loading
    };
  },
});
</script>
