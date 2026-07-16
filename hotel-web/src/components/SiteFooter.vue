<template>
  <footer class="site-footer">
    <div class="page-section footer-inner">
      <div class="footer-brand">
        <p class="footer-title">华庭酒店预订中心</p>
        <p class="footer-text">
          聚焦城市旅宿、品质客房与顺滑预订体验，提供从搜索到下单的一站式服务。
        </p>
        <div class="footer-contact">
          <span>客服热线：400-820-1888</span>
          <span>服务时间：7 x 24 小时</span>
        </div>
      </div>
      <div class="footer-grid">
        <div>
          <span class="footer-label">快捷入口</span>
          <div class="footer-links">
            <router-link to="/">首页</router-link>
            <router-link to="/search">搜索酒店</router-link>
            <router-link to="/user/orders">我的订单</router-link>
          </div>
        </div>
        <div>
          <span class="footer-label">账户服务</span>
          <div class="footer-links">
            <router-link to="/login">登录</router-link>
            <router-link to="/register">注册</router-link>
            <router-link to="/user/center">个人中心</router-link>
          </div>
        </div>
        <div>
          <span class="footer-label">管理入口</span>
          <div class="footer-links">
            <router-link to="/admin/hotels">酒店管理</router-link>
            <router-link to="/admin/orders">订单管理</router-link>
            <router-link to="/admin/users">用户管理</router-link>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  margin-top: 48px;
  padding: 28px 0 40px;
  border-top: 1px solid rgba(109, 90, 75, 0.08);
  background: rgba(248, 242, 233, 0.75);
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.footer-brand {
  max-width: 360px;
}

.footer-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--brand-primary);
}

.footer-text,
.footer-grid p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.footer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(140px, 1fr));
  gap: 20px;
}

.footer-contact {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--text-muted);
  font-size: 13px;
}

.footer-label {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-secondary-strong);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.footer-links {
  display: grid;
  gap: 10px;
}

.footer-links a {
  color: var(--text-secondary);
}

.footer-links a:hover {
  color: var(--brand-primary);
}

@media (max-width: 900px) {
  .footer-inner {
    flex-direction: column;
  }

  .footer-grid {
    width: 100%;
    grid-template-columns: 1fr;
  }
}
</style>
