import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge.vue'

describe('StatusBadge', () => {
  it('renders status label from status code', () => {
    const wrapper = mount(StatusBadge, {
      props: {
        status: 1
      }
    })

    expect(wrapper.text()).toContain('已支付')
  })

  it('allows overriding label text', () => {
    const wrapper = mount(StatusBadge, {
      props: {
        status: 1,
        text: '自定义状态'
      }
    })

    expect(wrapper.text()).toContain('自定义状态')
  })
})
