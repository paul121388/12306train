<template>
  <div>
    <p>
      <a-space>
        <a-button type="primary" @click="onAdd">新增</a-button>
        <a-button type="primary" @click="handleQuery()">刷新</a-button>
      </a-space>
    </p>
    <a-table :columns="columns"
             :data-source="passengers"
             :pagination="pagination"
             @change="handlePageChange"
             :loading="loading">
      <template #bodyCell="{column, record}">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-popconfirm
                title="删除后不可恢复，确认删除?"
                @confirm="onDelete(record)"
                ok-text="确认" cancel-text="取消">
              <a style="color: red">删除</a>
            </a-popconfirm>
            <a @click="onEdit(record)">编辑</a>
          </a-space>
        </template>
        <template v-else-if="column.dataIndex === 'type'">
        <span v-for="item in PASSENGER_TYPE_ARRAY" :key="item.key">
          <span v-if="item.key === record.type">
            {{item.value}}
          </span>
        </span>
        </template>
      </template>
    </a-table>
    <a-modal v-model:visible="open" title="乘车人" @ok="handleOk"
             ok-text="确认" cancel-text="取消">
      <a-form
          :model="passenger"
          name="basic"
          :label-col="{ span: 8 }"
          :wrapper-col="{ span: 16 }"
          autocomplete="off"
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
            <a-select-option v-for="item in PASSENGER_TYPE_ARRAY" :key="item.key" :value="item.value">
              {{item.value}}
            </a-select-option>
          </a-select>
        </a-form-item>


      </a-form>
    </a-modal>
  </div>
</template>
<script>

import {defineComponent, ref, onMounted} from 'vue';
import axios from "axios";
import {notification} from "ant-design-vue";

export default defineComponent({
  name: "passenger-view",
  setup() {
    const open = ref(false);
    const PASSENGER_TYPE_ARRAY = window.PASSENGER_TYPE;
    const pagination = ref({
      total: 0,
      current: 1,
      pageSize: 2
    });
    let passenger = ref({
      id: undefined,
      memberId: undefined,
      name: undefined,
      idCard: undefined,
      type: undefined,
      createTime: undefined,
      updateTime: undefined,

    });
    const passengers = ref([]);
    let loading = ref(false);
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
      }, {
        title: '操作',
        dataIndex: 'operation'
      }
    ];

    const onAdd = () => {
      passenger.value={};
      open.value = true;
    };

    const onEdit = (record) => {
      passenger.value = window.Tool.copy(record);
      open.value = true;
    };

    const onDelete = (record) => {
      axios.delete("/member/passenger/delete/" + record.id).then((response) => {
        const data = response.data;
        if (data.success) {
          notification.success({description: "删除成功！"});
          handleQuery({
            page: pagination.value.current,
            size: pagination.value.pageSize,
          });
        } else {
          notification.error({description: data.message});
        }
      });
    };


    const handleOk = e => {
      axios.post('/member/passenger/save', passenger.value).then(response => {
        let data = response.data;
        if (data.success) {
          notification.success({description: "保存成功！"});
          open.value = false;
          handleQuery({
            page: pagination.value.current,
            size: pagination.value.pageSize
          });
        } else {
          notification.error({description: data.message});
        }
      })
      console.log(e);
      open.value = false;
    };

    const handleQuery = (param) => {
      if (!param) {
        param = {
          page: 1,
          size: pagination.value.pageSize
        };
      }
      loading.value = true;
      axios.get('/member/passenger/query-list', {
        params: {
          page: param.page,
          size: param.size
        }
      }).then((response) => {
        loading.value = false;
        let data = response.data;
        if (data.success) {
          passengers.value = data.content.list;
          pagination.value.current = param.page;
          pagination.value.total = data.content.total;
        } else {
          notification.error({description: data.message});
        }
      });
    }

    const handlePageChange = (pagination) => {
      handleQuery({
        page: pagination.current,
        size: pagination.pageSize
      })
    }

    onMounted(() => {
      handleQuery({page: 1, size: pagination.value.pageSize});
    });

    return {
      open,
      onAdd,
      handleOk,
      passengers,
      passenger,
      columns,
      pagination,
      handlePageChange,
      handleQuery,
      loading,
      onEdit,
      onDelete,
      PASSENGER_TYPE_ARRAY
    };
  }
});
</script>