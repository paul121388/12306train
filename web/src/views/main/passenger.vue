<template>
  <div>
    <a-button type="primary" @click="showModal">新增</a-button>
    <a-modal v-model:visible="open" title="乘车人" @ok="handleOk"
              ok-text="确认" cancel-text="取消">
      <a-form
          :model="passenger"
          name="basic"
          :label-col="{ span: 8 }"
          :wrapper-col="{ span: 16 }"
          autocomplete="off"
          @finish="onFinish"
          @finishFailed="onFinishFailed"
      >
        <a-form-item
            label="姓名"
            name="name"
            :rules="[{ required: true, message: '请输入姓名!' }]"
        >
          <a-input v-model:value="passenger.name" />
        </a-form-item>

        <a-form-item
            label="身份证"
            name="idCard"
            :rules="[{ required: true, message: '请输入身份证号!' }]"
        >
          <a-input v-model:value="passenger.idCard" />
        </a-form-item>

        <a-form-item
            label="类型"
            name="type"
            :rules="[{ required: true, message: 'Please input your password!' }]"
        >
          <a-select
              ref="select"
              v-model:value="passenger.type"
              style="width: 120px"
          >
            <a-select-option value="1">成人</a-select-option>
            <a-select-option value="2">儿童</a-select-option>
            <a-select-option value="3">学生</a-select-option>
          </a-select>
        </a-form-item>


      </a-form>
    </a-modal>
  </div>
</template>
<script>

import {defineComponent, ref, reactive} from 'vue';

export default defineComponent({
  name: "passenger-view",
  setup() {
    const open = ref(false);
    const passenger = reactive({
      id: undefined,
      memberId: undefined,
      name: undefined,
      idCard: undefined,
      type: undefined,
      createTime: undefined,
      updateTime: undefined,

    });
    const showModal = () => {
      open.value = true;
    };

    const handleOk = e => {
      console.log(e);
      open.value = false;
    };

    const onFinish = values => {
      console.log('Success:', values);
    };

    const onFinishFailed = errorInfo => {
      console.log('Failed:', errorInfo);
    };

    return {
      open,
      showModal,
      handleOk,
      onFinish,
      onFinishFailed,
      passenger
    };
  }
});
</script>