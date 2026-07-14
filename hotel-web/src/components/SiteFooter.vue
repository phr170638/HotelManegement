<template>
  <footer class="site-footer">
    <div class="page-section footer-inner">
      <div>
        <p class="footer-title">华庭酒店预订中心</p>
        <p class="footer-text">
          聚焦城市旅宿、品质客房与顺滑预订体验，提供从搜索到下单的一站式服务。
        </p>
      </div>
      <div class="footer-grid">
        <div>
          <span class="footer-label">精选服务</span>
          <p>城市精选 / 会员礼遇 / 快速入住</p>
        </div>
        <div>
          <span class="footer-label">旅宿体验</span>
          <p>安静睡眠 / 东方茶饮 / 灵活出行</p>
        </div>
        <div>
          <span class="footer-label">服务支持</span>
          <p>在线预订 / 订单管理 / 客服协助</p>
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

.footer-label {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-secondary-strong);
  letter-spacing: 0.08em;
  text-transform: uppercase;
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
