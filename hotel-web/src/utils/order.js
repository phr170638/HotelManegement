const statusMap = {
  0: {
    label: '待支付',
    tone: 'warning',
    description: '订单已创建，等待完成支付。'
  },
  1: {
    label: '已支付',
    tone: 'success',
    description: '订单已支付成功，等待入住。'
  },
  2: {
    label: '已取消',
    tone: 'info',
    description: '订单已取消，不再占用房量。'
  },
  3: {
    label: '已入住',
    tone: 'success',
    description: '客人已办理入住。'
  },
  4: {
    label: '退房申请中',
    tone: 'warning',
    description: '已提交退房申请，等待处理。'
  },
  5: {
    label: '已退房',
    tone: 'info',
    description: '订单已完成退房处理。'
  },
  6: {
    label: '已完成',
    tone: 'success',
    description: '订单已全部完成。'
  }
}

export function getOrderStatusMeta(status) {
  if (typeof status !== 'number') {
    return {
      label: '未知状态',
      tone: 'danger',
      description: '当前状态未识别，请稍后刷新重试。'
    }
  }

  return statusMap[status] || {
    label: '未知状态',
    tone: 'danger',
    description: '当前状态未识别，请稍后刷新重试。'
  }
}
