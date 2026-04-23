import { profileApi } from '@/api'

export const syncCurrentProfile = async (userStore) => {
  try {
    const res = await profileApi.getProfile()
    if (res.data) {
      userStore.updateUser(res.data)
      return res.data
    }
  } catch (error) {
    console.error('同步个人资料失败', error)
  }

  return null
}
