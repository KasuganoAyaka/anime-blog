export const POST_CATEGORIES = ['闲聊', '软件', '经验分享', '项目分享', '数码前线', '前端', '后端', '生活', '技术', '资源优选', '游戏', '职场与成长']

export const createEmptyPostForm = () => ({
  title: '',
  category: '',
  excerpt: '',
  content: '',
  tags: '',
  coverImage: '',
  status: 'draft'
})
