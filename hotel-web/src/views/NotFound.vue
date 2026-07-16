<template>
  <div class="not-found-page page-container narrow">
    <section class="not-found-card soft-card">
      <div class="gold-chip">Page Missing</div>
      <h1>页面不存在</h1>
      <p>当前链接可能已失效，或者页面还没有开放。你可以返回首页、继续搜索酒店，或回到后台入口。</p>

      <div class="action-grid">
        <el-button type="primary" class="primary-btn" @click="$router.push('/')">返回首页</el-button>
        <el-button @click="$router.push('/search')">搜索酒店</el-button>
        <el-button @click="$router.push('/admin/hotels')">后台首页</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.not-found-page {
  padding: 48px 0 0;
}

.not-found-card {
  padding: 36px 32px;
  background: rgba(255, 252, 247, 0.92);
}

.not-found-card h1 {
  margin: 18px 0 10px;
  font-size: clamp(30px, 4vw, 44px);
}

.not-found-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.action-grid {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

@media (max-width: 700px) {
  .action-grid {
    flex-direction: column;
  }
}
</style>
