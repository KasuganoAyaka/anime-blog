export const usePostCommentPresentation = ({ locale }) => {
  const clientRegionLabels = {
    local: '',
    'ipv6-network': '',
    'public-network': ''
  }

  const clientOsLabels = {
    windows: 'Windows',
    macos: 'macOS',
    ios: 'iOS',
    ipados: 'iPadOS',
    android: 'Android',
    harmonyos: 'HarmonyOS',
    linux: 'Linux',
    chromeos: 'ChromeOS',
    unix: 'Unix'
  }

  const clientBrowserLabels = {
    edge: 'Edge',
    chrome: 'Chrome',
    firefox: 'Firefox',
    safari: 'Safari',
    opera: 'Opera',
    'samsung-internet': 'Samsung Internet',
    'qq-browser': 'QQ Browser',
    wechat: 'WeChat',
    ie: 'Internet Explorer'
  }

  const clientTagIconMap = {
    region: [
      { tag: 'path', attrs: { d: 'M12 21s-5.5-4.6-5.5-9.1a5.5 5.5 0 1 1 11 0C17.5 16.4 12 21 12 21Z' } },
      { tag: 'circle', attrs: { cx: '12', cy: '11', r: '1.8' } }
    ],
    windows: [
      { tag: 'path', attrs: { d: 'M4 5.5 10.2 4v7H4Z' } },
      { tag: 'path', attrs: { d: 'M13 3.6 20 2.5v8.5h-7Z' } },
      { tag: 'path', attrs: { d: 'M4 12.5h6.2v7L4 18.5Z' } },
      { tag: 'path', attrs: { d: 'M13 12.5h7v8.9L13 20.4Z' } }
    ],
    macos: [
      { tag: 'rect', attrs: { x: '4', y: '5', width: '16', height: '11', rx: '2.5' } },
      { tag: 'path', attrs: { d: 'M9 19h6' } },
      { tag: 'path', attrs: { d: 'M12 16v3' } }
    ],
    ios: [
      { tag: 'rect', attrs: { x: '7.2', y: '3', width: '9.6', height: '18', rx: '2.5' } },
      { tag: 'circle', attrs: { cx: '12', cy: '17.2', r: '0.9' } }
    ],
    ipados: [
      { tag: 'rect', attrs: { x: '5', y: '3.5', width: '14', height: '17', rx: '2.5' } },
      { tag: 'circle', attrs: { cx: '12', cy: '17.4', r: '0.9' } }
    ],
    android: [
      { tag: 'path', attrs: { d: 'M8 9.2h8a2.8 2.8 0 0 1 2.8 2.8v4.5A2.5 2.5 0 0 1 16.3 19h-8.6A2.5 2.5 0 0 1 5.2 16.5V12A2.8 2.8 0 0 1 8 9.2Z' } },
      { tag: 'path', attrs: { d: 'M9.2 6.3 7.8 4.8' } },
      { tag: 'path', attrs: { d: 'M14.8 6.3 16.2 4.8' } },
      { tag: 'circle', attrs: { cx: '9.7', cy: '12.4', r: '0.7' } },
      { tag: 'circle', attrs: { cx: '14.3', cy: '12.4', r: '0.7' } }
    ],
    harmonyos: [
      { tag: 'path', attrs: { d: 'M12 4.2c2.8 0 5 2.2 5 5 0 1.4-.6 2.8-1.7 3.7L12 16.2l-3.3-3.3A5.1 5.1 0 0 1 7 9.2c0-2.8 2.2-5 5-5Z' } },
      { tag: 'path', attrs: { d: 'M12 16.2v3.6' } }
    ],
    linux: [
      { tag: 'rect', attrs: { x: '4', y: '5', width: '16', height: '14', rx: '2.5' } },
      { tag: 'path', attrs: { d: 'm8.2 10.2 2.3 2.1-2.3 2.1' } },
      { tag: 'path', attrs: { d: 'M12.4 14.6h3.4' } }
    ],
    chromeos: [
      { tag: 'rect', attrs: { x: '4', y: '6', width: '16', height: '10', rx: '2.2' } },
      { tag: 'path', attrs: { d: 'M2.8 18.5h18.4' } }
    ],
    unix: [
      { tag: 'rect', attrs: { x: '4', y: '5', width: '16', height: '14', rx: '2.5' } },
      { tag: 'path', attrs: { d: 'M7.8 10.4h8.4' } },
      { tag: 'path', attrs: { d: 'M7.8 13.6h5.8' } }
    ],
    edge: [
      { tag: 'path', attrs: { d: 'M19 15.4a6.9 6.9 0 1 1-12.8-3.6c1-1.8 3-3.1 5.3-3.1 2.8 0 5 1.8 5.8 4.3-1.1-.7-2.4-1-3.9-1-3.6 0-6 1.8-6 4.4 0 2.1 1.9 3.6 4.5 3.6 3 0 5.8-1.9 7.1-4.6Z' } }
    ],
    chrome: [
      { tag: 'circle', attrs: { cx: '12', cy: '12', r: '3.1' } },
      { tag: 'path', attrs: { d: 'M12 4.3h5.3l2.2 3.8-3.2 5.5H9.7' } },
      { tag: 'path', attrs: { d: 'M4.8 8.1 7.5 4.3H12l2.8 4.8-3.3 5.7' } },
      { tag: 'path', attrs: { d: 'M8.4 14.8H4.9 4.8l2.7 4.9H16' } }
    ],
    firefox: [
      { tag: 'circle', attrs: { cx: '12.2', cy: '12.4', r: '3.2' } },
      { tag: 'path', attrs: { d: 'M15.8 7.5c1.8 1 3 2.9 3 5.1 0 3.3-2.7 6-6 6-2.8 0-5.2-1.9-5.8-4.6.6.6 1.5 1 2.5 1 1.9 0 3.4-1.4 3.4-3.2 0-1.2-.7-2.3-1.7-2.9 1-.8 2.1-1.5 3.3-1.4.6 0 1 .3 1.3 1Z' } }
    ],
    safari: [
      { tag: 'circle', attrs: { cx: '12', cy: '12', r: '7.5' } },
      { tag: 'path', attrs: { d: 'm12 8.1 2.6 5.1L9.4 16z' } },
      { tag: 'path', attrs: { d: 'M12 5.2v1.6' } },
      { tag: 'path', attrs: { d: 'M18.8 12h-1.6' } },
      { tag: 'path', attrs: { d: 'M12 18.8v-1.6' } },
      { tag: 'path', attrs: { d: 'M5.2 12h1.6' } }
    ],
    opera: [
      { tag: 'circle', attrs: { cx: '12', cy: '12', r: '7.2' } },
      { tag: 'circle', attrs: { cx: '12', cy: '12', r: '3.3' } }
    ],
    'samsung-internet': [
      { tag: 'circle', attrs: { cx: '12', cy: '12', r: '5.5' } },
      { tag: 'path', attrs: { d: 'M4.5 14.5c1.3-2.6 4-4.3 7.1-4.3 3.7 0 6.9 2.3 8 5.6' } }
    ],
    'qq-browser': [
      { tag: 'circle', attrs: { cx: '11.5', cy: '11.5', r: '6.2' } },
      { tag: 'path', attrs: { d: 'M16.5 16.5 19.5 19.5' } }
    ],
    wechat: [
      { tag: 'path', attrs: { d: 'M10.2 6.2c-3.2 0-5.7 2-5.7 4.8 0 1.4.6 2.6 1.7 3.5L5.6 17l2.7-1.4c.6.1 1.2.2 1.9.2 3.2 0 5.7-2 5.7-4.8s-2.5-4.8-5.7-4.8Z' } },
      { tag: 'path', attrs: { d: 'M15.8 10.7c2.4 0 4.2 1.5 4.2 3.6 0 1-.4 1.9-1.2 2.6l.3 2-2-1.1a6 6 0 0 1-1.3.1c-2.4 0-4.2-1.5-4.2-3.6s1.8-3.6 4.2-3.6Z' } }
    ],
    ie: [
      { tag: 'path', attrs: { d: 'M7.2 12.4c.4-3.4 2.9-5.9 6.1-5.9 2.1 0 3.9 1 5 2.7' } },
      { tag: 'path', attrs: { d: 'M6.8 13.3h10.8c-.4 2.7-2.6 4.7-5.3 4.7-2.8 0-5-1.8-5.5-4.7Z' } },
      { tag: 'path', attrs: { d: 'M4.8 9.8c1.8-1.1 4.2-1.8 7-1.8 3 0 5.6.8 7.4 2.2' } }
    ],
    default: [
      { tag: 'circle', attrs: { cx: '12', cy: '12', r: '7.5' } }
    ]
  }

  const anonymousSurnamePool = '赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦许何吕张孔曹严华金魏陶姜戚谢邹喻柏水窦章云苏潘葛范彭郎鲁韦昌马苗凤花方俞任袁柳鲍史唐薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅皮卞齐康伍余元顾孟平黄穆萧尹'
  const anonymousGivenNamePool = '安白川初灯朵恩风归海栀纪景柠林洛梦南念宁沐清秋然若山时书棠晚希夏晓星言悠雨月枝知舟竹子霁岚棠澄遥栀语微青鸢霖昀宁冉听澈眠寻鹿漫暖舒念泠岫朔吟'

  const buildClientTag = (kind, key, label) => ({
    kind,
    key: String(key || kind),
    label: String(label || '')
  })

  const getClientTagIcon = (tag) => {
    if (!tag || typeof tag !== 'object') {
      return clientTagIconMap.default
    }
    if (tag.kind === 'region') {
      return clientTagIconMap.region
    }
    return clientTagIconMap[tag.key] || clientTagIconMap.default
  }

  const getCommentClientTags = (item) => {
    if (!item || typeof item !== 'object') {
      return []
    }

    const tags = []
    const regionKey = String(item.clientRegion || '')
    const region = regionKey && regionKey !== 'unknown'
      ? (Object.prototype.hasOwnProperty.call(clientRegionLabels, regionKey) ? clientRegionLabels[regionKey] : regionKey)
      : ''
    const os = item.clientOs && item.clientOs !== 'unknown'
      ? (clientOsLabels[item.clientOs] || item.clientOs)
      : ''
    const browser = item.clientBrowser && item.clientBrowser !== 'unknown'
      ? (clientBrowserLabels[item.clientBrowser] || item.clientBrowser)
      : ''

    if (region) tags.push(buildClientTag('region', regionKey || 'region', region))
    if (os) tags.push(buildClientTag('os', item.clientOs, os))
    if (browser) tags.push(buildClientTag('browser', item.clientBrowser, browser))
    return tags
  }

  const formatCommentParagraphs = (content) => String(content || '')
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)

  const formatCommentDate = (value) => {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return date.toLocaleString(locale.value === 'en' ? 'en-US' : locale.value, {
      month: locale.value === 'en' ? 'short' : '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const randomFromPool = (pool) => pool[Math.floor(Math.random() * pool.length)] || ''

  const createAnonymousAlias = () => {
    const surname = randomFromPool(anonymousSurnamePool)
    const givenLength = 2 + Math.floor(Math.random() * 3)
    let givenName = ''
    for (let index = 0; index < givenLength; index += 1) {
      givenName += randomFromPool(anonymousGivenNamePool)
    }
    return `${surname}${givenName}`.slice(0, 5)
  }

  return {
    createAnonymousAlias,
    formatCommentDate,
    formatCommentParagraphs,
    getClientTagIcon,
    getCommentClientTags
  }
}
