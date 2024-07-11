<template>
  <div>
    <p>
      <a-button type="primary" @click="showModal">新增</a-button>
    </p>
    <a-table :columns="columns" :data-source="passengers" :pagination="pagination"></a-table>
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
          <a-input v-model:value="passenger.name"/>
        </a-form-item>

        <a-form-item
            label="身份证"
            name="idCard"
            :rules="[{ required: true, message: '请输入身份证号!' }]"
        >
          <a-input v-model:value="passenger.idCard"/>
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

import {defineComponent, ref, reactive, onMounted} from 'vue';
import axios from "axios";
import {notification} from "ant-design-vue";

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
    const passengers = ref([]);
    const columns = [
      {
        title: '姓名',
        dataIndex: 'name',
        key: 'name',
      },
      {
        title: '身份证',
        dataIndex: 'idCard',
        key: 'idCard',
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
      },
    ];

    const showModal = () => {
      open.value = true;
    };

    const handleOk = e => {
      axios.post('/member/passenger/save', passenger).then(response => {
        let data = response.data;
        if (data.success) {
          notification.success({description: "保存成功！"});
          open.value = false;
        } else {
          notification.error({description: data.message});
        }
      })
      console.log(e);
      open.value = false;
    };

    const handleQuery = (param) =>{
      axios.get('/member/passenger/query-list',{
        params:{
          page: param.page,
          size: param.size
        }
      }).then((response) => {
        let data = response.data;
        if (data.success) {
          passengers.value = data.content.list;
          pagination.total = data.content.total;
        }
        else {
          notification.error({description: data.message});
        }
      });
    }

    const pagination = reactive({
      total: 0,
      current: 1,
      pageSize: 2,
    });

    onMounted(() => {
      handleQuery({page: 1, size: 2});
    });

    return {
      open,
      showModal,
      handleOk,
      passengers,
      columns,
      pagination
    };
  }
});
</script>