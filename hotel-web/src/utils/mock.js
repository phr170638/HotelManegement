import { getCityImage, getHotelGallery } from './hotel'

export const mockCities = [
  { id: 1, nameCn: '西安', nameEn: "Xi'an", image: getCityImage('Xi an') },
  { id: 2, nameCn: '北京', nameEn: 'Beijing', image: getCityImage('Beijing') },
  { id: 3, nameCn: '上海', nameEn: 'Shanghai', image: getCityImage('Shanghai') },
  { id: 4, nameCn: '成都', nameEn: 'Chengdu', image: getCityImage('Chengdu') }
]

export const mockHotels = [
  {
    id: 101,
    cityId: 1,
    cityName: '西安',
    nameCn: '云栖御庭酒店',
    nameEn: 'Yunqi Court Hotel',
    starLevel: 5,
    score: 4.8,
    address: '西安市雁塔区曲江新区雁南一路 88 号',
    brand: '云栖精选',
    minPrice: 698,
    reviewCount: 328,
    facilities: ['免费 WiFi', '泳池', '早餐', '停车场']
  },
  {
    id: 102,
    cityId: 2,
    cityName: '北京',
    nameCn: '观澜国际酒店',
    nameEn: 'Grandview International',
    starLevel: 4,
    score: 4.6,
    address: '北京市朝阳区建国路 66 号',
    brand: '观澜',
    minPrice: 568,
    reviewCount: 251,
    facilities: ['商务中心', '健身房', '洗衣服务', '快速入住']
  },
  {
    id: 103,
    cityId: 3,
    cityName: '上海',
    nameCn: '泊岸设计酒店',
    nameEn: 'Harbor Atelier Hotel',
    starLevel: 4,
    score: 4.7,
    address: '上海市黄浦区中山东二路 18 号',
    brand: '泊岸',
    minPrice: 788,
    reviewCount: 402,
    facilities: ['江景房', '餐厅', '酒吧', '会议室']
  }
]

export const mockHotelDetail = {
  ...mockHotels[0],
  description:
    '酒店以现代东方美学为灵感，提供面向商务与休闲旅客的高品质住宿体验，步行可达商圈与景点，适合差旅与城市度假等多种出行场景。',
  images: getHotelGallery(mockHotels[0]).map((url, index) => ({
    id: index + 1,
    url,
    type: index === 0 ? 1 : 2,
    sortOrder: index + 1
  })),
  rooms: [
    {
      id: 9001,
      name: '高级大床房',
      bedType: '大床',
      breakfast: '双早',
      maxGuests: 2,
      area: '38m²',
      floor: '12-18层',
      price: 698,
      cancelable: 1,
      cancelPenalty: 80,
      facilities: ['浴缸', '落地窗', '办公桌', '智能电视'],
      images: getHotelGallery({ nameCn: '高级大床房' })
    },
    {
      id: 9002,
      name: '行政双床房',
      bedType: '双床',
      breakfast: '双早',
      maxGuests: 2,
      area: '42m²',
      floor: '19-22层',
      price: 828,
      cancelable: 1,
      cancelPenalty: 100,
      facilities: ['行政礼遇', '咖啡机', '观景窗', '浴袍'],
      images: getHotelGallery({ nameCn: '行政双床房' })
    }
  ]
}

export const mockReviews = [
  {
    id: 1,
    score: 5,
    content: '房间很新，前台办理入住很快，整体流程清晰顺畅，入住体验很舒服。',
    createTime: '2026-07-12 15:20:00',
    user: { nickname: '旅行者小周' }
  },
  {
    id: 2,
    score: 4,
    content: '地理位置不错，早餐品类丰富，房间安静，适合商务出行和周末短住。',
    createTime: '2026-07-10 09:10:00',
    user: { nickname: '木子小姐' }
  }
]

export const mockOrders = [
  {
    id: 6001,
    orderNo: 'HT202607140001',
    hotelName: '云栖御庭酒店',
    hotelImage: getHotelGallery(mockHotels[0])[0],
    checkInDate: '2026-07-20',
    checkOutDate: '2026-07-22',
    totalAmount: 1396,
    status: 0,
    statusText: '待支付',
    createTime: '2026-07-14 10:00:00'
  },
  {
    id: 6002,
    orderNo: 'HT202607120015',
    hotelName: '观澜国际酒店',
    hotelImage: getHotelGallery(mockHotels[1])[0],
    checkInDate: '2026-07-18',
    checkOutDate: '2026-07-19',
    totalAmount: 568,
    status: 1,
    statusText: '已支付',
    createTime: '2026-07-12 21:00:00'
  }
]

export const mockAdminOrders = [
  {
    id: 7001,
    orderNo: 'ADM202607140031',
    guestName: '王小雨',
    totalAmount: 968,
    status: 1,
    createTime: '2026-07-14 09:40:00'
  },
  {
    id: 7002,
    orderNo: 'ADM202607130087',
    guestName: '李晨',
    totalAmount: 1280,
    status: 4,
    createTime: '2026-07-13 16:20:00'
  }
]

export const mockAdminUsers = [
  {
    id: 1,
    phone: '13800000000',
    email: 'admin@hotel.com',
    nickname: '系统管理员',
    status: 1,
    createTime: '2026-07-13 08:00:00'
  },
  {
    id: 2,
    phone: '13911112222',
    email: 'demo@hotel.com',
    nickname: '李安然',
    status: 1,
    createTime: '2026-07-13 11:30:00'
  }
]

export const mockUserInfo = {
  id: 2,
  phone: '13911112222',
  email: 'demo@hotel.com',
  nickname: '李安然',
  avatar: '',
  roles: ['user'],
  permissions: [],
  createTime: '2026-07-13 11:30:00'
}
