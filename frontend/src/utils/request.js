export const getRequestErrorMessage = (error, fallback = 'Request failed') => {
  const responseMessage = error?.response?.data?.message || error?.response?.data?.error

  if (error?.code === 'ECONNABORTED' || /timeout/i.test(error?.message || '')) {
    return '请求超时，请稍后重试'
  }

  if (error?.response?.status === 413) {
    return responseMessage || '上传文件大小超出限制'
  }

  if (/network error/i.test(error?.message || '')) {
    return responseMessage || '网络连接异常，若上传大文件请检查服务器上传大小限制'
  }

  return responseMessage || error?.message || fallback
}
