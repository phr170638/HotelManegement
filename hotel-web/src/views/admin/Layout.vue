<template>
  <el-container class="admin-layout">
    <el-aside width="220px">
      <div class="logo">酒店管理系统</div>
      <el-menu :default-active="route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/admin/hotels">
          <el-icon><OfficeBuilding /></el-icon>
          <span>酒店管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <el-button @click="$router.push('/')">返回首页</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.el-aside { background-color: #304156; }
.logo { color: #fff; font-size: 18px; padding: 20px; text-align: center; border-bottom: 1px solid #4a5a6e; }
.el-header { background: #fff; display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #e6e6e6; }
.el-main { background: #f5f7fa; }
</style>
