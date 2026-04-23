export const defaultLocale = 'zh-CN'
export const fallbackLocale = 'zh-CN'
export const supportedLocales = ['zh-CN', 'zh-TW', 'en']

export const messages = {
  'zh-CN': {
    common: {
      appName: 'AyakaのBlog',
      loading: '加载中...',
      guest: '游客',
      loggedIn: '已登录',
      admin: '管理员',
      user: '普通用户',
      language: '语言',
      back: '返回',
      save: '保存',
      cancel: '取消',
      submit: '提交',
      send: '发送',
      readMore: '阅读全文',
      noSummary: '暂无摘要',
      noData: '暂无数据',
      backHome: '返回博客首页',
      copyright: '© {year} AyakaのBlog. 保留所有权利。',
      languages: {
        'zh-CN': '简中',
        'zh-TW': '繁中',
        en: 'EN'
      }
    },
    theme: {
      system: '系统主题',
      dark: '深色主题',
      light: '浅色主题'
    },
    topHeader: {
      home: '首页',
      tags: '标签',
      categories: '分类',
      about: '关于',
      music: '音乐',
      backToTop: '返回顶部',
      backToTopHint: '回到顶部'
    },
    userMenu: {
      enterAdmin: '进入后台管理',
      enterWorkspace: '进入工作台',
      login: '登录',
      logout: '退出登录',
      confirmLogout: '确定要退出登录吗？',
      logoutTitle: '退出登录',
      logoutSuccess: '已退出登录'
    },
    home: {
      emptyTitle: '暂无文章',
      emptyDesc: '管理员快去后台发布第一篇文章吧',
      views: '{count} 次浏览',
      author: '创作者',
      authorBio: 'Hi friend 👋 这里是 AyakaのBlog',
      hotTags: '热门标签',
      noTags: '暂无标签',
      friendLinks: '友情链接',
      noLinks: '暂无友链'
    },
    login: {
      title: 'Ayaka-Blog',
      desc: '登录后即可进入后台与个人中心',
      usernamePlaceholder: '用户名或邮箱',
      passwordPlaceholder: '密码',
      turnstileHint: '登录失败次数较多时，需要先完成人机验证。',
      turnstileRequired: '请先完成人机验证',
      rememberMe: '7 天内免登录',
      submit: '登录',
      submitting: '登录中...',
      register: '注册账号',
      forgotPassword: '忘记密码',
      fillCredentials: '请填写用户名和密码',
      success: '登录成功',
      rememberSuccess: '登录成功，7 天内免登录'
    },
    register: {
      title: 'AyakaのBlog',
      desc: '创建你的站内账号，完成邮箱验证与人机验证后即可开始使用。',
      usernamePlaceholder: '用户名',
      usernameTip: '用户名长度需为 {min}-{max} 位',
      emailPlaceholder: '邮箱地址',
      sendCode: '发送验证码',
      emailCodePlaceholder: '邮箱验证码',
      passwordPlaceholder: '密码',
      confirmPasswordPlaceholder: '确认密码',
      turnstileHint: '发送注册邮箱验证码前，请先完成人机验证。',
      captchaPlaceholder: '图形验证码',
      captchaHint: '输入邮箱后获取',
      submit: '注册',
      backLogin: '返回登录',
      messages: {
        confirmPasswordRequired: '请确认密码',
        confirmPasswordMismatch: '两次密码输入不一致',
        usernameRequired: '请输入用户名',
        usernameLength: '用户名长度为 {min}-{max} 位',
        emailRequired: '请输入邮箱',
        emailInvalid: '邮箱格式不正确',
        emailCodeRequired: '请输入邮箱验证码',
        emailCodeLength: '验证码为 6 位',
        turnstileRequired: '请先完成人机验证',
        captchaRequired: '请输入图形验证码',
        captchaLength: '图形验证码为 4 位',
        invalidEmailFirst: '请先输入有效邮箱',
        captchaLoadFailed: '获取图形验证码失败',
        codeSending: '验证码发送中，请留意邮箱',
        codeSendFailed: '发送邮箱验证码失败',
        success: '注册成功，请登录',
        failed: '注册失败'
      }
    },
    passwordRules: {
      unsupported: '密码仅支持英文、数字和常见符号，不支持中文或表情。',
      ruleMessage: '密码需为 {min}-{max} 位，仅支持英文、数字和常见符号，且字母、数字、特殊字符至少满足两类。',
      charset: '仅支持英文、数字和常见符号',
      length: '长度 {min}-{max} 位',
      letter: '包含字母',
      digit: '包含数字',
      special: '包含特殊字符',
      categories: '以上三类中至少满足两类',
      required: '请输入密码'
    },
    forgotPassword: {
      title: '重置密码',
      desc: '按步骤完成邮箱验证后重置密码',
      emailPlaceholder: '请输入注册邮箱',
      sendCode: '发送验证码',
      codePlaceholder: '输入 6 位邮箱验证码',
      verifyCode: '验证验证码',
      resendCode: '重新发送验证码',
      resendCountdown: '{seconds}s 后可重发',
      changeEmail: '修改邮箱',
      newPasswordPlaceholder: '新密码',
      confirmNewPasswordPlaceholder: '确认新密码',
      resetPassword: '重置密码',
      successTitle: '密码已重置',
      successDesc: '现在可以使用新密码重新登录。',
      backLogin: '返回登录',
      messages: {
        invalidEmail: '请输入有效邮箱',
        codeSending: '验证码发送中，请留意邮箱',
        codeSendFailed: '发送验证码失败',
        codeRequired: '请输入 6 位验证码',
        codeVerified: '验证码校验通过',
        codeInvalid: '验证码错误',
        passwordIncomplete: '请完整填写新密码',
        passwordMismatch: '两次密码输入不一致',
        resetSuccess: '密码重置成功',
        resetFailed: '密码重置失败'
      }
    },
    about: {
      tabs: {
        contact: '联系我们',
        sites: '我的关联站点',
        links: '友情链接'
      },
      contact: {
        title: '联系我们',
        desc: '如果你对文章有想法、对网站建设有问题，或想讨论技术与创意的任何点子，都欢迎通过下面的方式与我取得联系。我会尽量在空闲时回复。',
        name: '姓名',
        namePlaceholder: '请输入你的名字',
        email: '邮箱',
        emailPlaceholder: '请输入你的邮箱',
        subject: '主题',
        subjectPlaceholder: '请输入邮件主题',
        message: '内容',
        messagePlaceholder: '请输入你的消息',
        submit: '发送消息',
        otherMethods: '其他联系方式',
        success: '消息已发送',
        failed: '消息发送失败'
      },
      sites: {
        title: '我的关联站点',
        desc: '这些是我运营的其他网站和服务，欢迎访问。',
        visit: '访问 ->',
        ayakaCloud: '个人网盘，分享文件和资源',
        bilibili: '分享动漫视频和精彩片段',
        music: '精选音乐合集',
        knowledge: '技术文档和笔记'
      },
      links: {
        title: '友情链接',
        desc: '这里汇集了我所信赖、欣赏并持续获得灵感的数字世界“地图”。',
        techPartners: '技术伙伴',
        empty: '暂无友情链接',
        submitTitle: '提交友情链接',
        submitDesc: '如果你也想与我交换友情链接，欢迎填写下面的表单。',
        siteName: '网站名称',
        siteNamePlaceholder: '你的网站名称',
        siteUrl: '网站 URL',
        siteDesc: '网站描述',
        siteDescPlaceholder: '简短描述你的网站',
        contactEmail: '联系邮箱',
        contactEmailPlaceholder: '你的邮箱',
        submit: '提交申请',
        success: '友链申请已提交',
        failed: '友链申请提交失败'
      },
      rules: {
        nameRequired: '请输入姓名',
        emailRequired: '请输入邮箱',
        emailInvalid: '邮箱格式不正确',
        subjectRequired: '请输入主题',
        messageRequired: '请输入内容',
        siteNameRequired: '请输入网站名称',
        siteUrlRequired: '请输入网站 URL',
        urlInvalid: 'URL 格式不正确',
        siteDescRequired: '请输入网站描述'
      }
    },
    tags: {
      title: '标签',
      desc: '点击标签查看相关文章，快速找到你感兴趣的内容。',
      emptyTitle: '暂无标签',
      emptyDesc: '文章发布后标签会自动显示',
      backToList: '返回标签列表',
      noArticles: '该标签下暂无文章',
      articleCount: '{count} 篇文章'
    },
    categories: {
      title: '分类',
      desc: '点击分类卡片查看相关文章，探索不同领域的内容。',
      emptyTitle: '暂无分类',
      emptyDesc: '文章发布后分类会自动显示',
      backToList: '返回分类列表',
      noArticles: '该分类下暂无文章',
      articleCount: '{count} 篇文章'
    },
    modules: {
      title: '功能模块',
      linksTitle: '友情链接',
      noFriends: '暂无友情链接',
      mainSite: '主站',
      mainSiteDesc: '博客首页，浏览最新文章和动态',
      drive: '网盘',
      driveDesc: '资源共享中心，各类资源下载',
      more: '更多功能',
      moreDesc: '持续更新中，敬请期待...',
      developing: '功能开发中...'
    },
    postDetail: {
      back: '返回',
      notFound: '文章不存在或未发布'
    },
    admin: {
      nav: {
        dashboard: '控制台',
        posts: '文章管理',
        reviews: '文章审核',
        music: '音乐管理',
        users: '用户管理',
        links: '友链管理',
        contacts: '联系消息',
        profile: '个人信息',
        workspace: '工作台',
        myPosts: '我的文章'
      },
      topbar: {
        front: '返回前台',
        logout: '退出登录'
      }
    }
  },
  'zh-TW': {
    common: {
      appName: 'AyakaのBlog',
      loading: '載入中...',
      guest: '遊客',
      loggedIn: '已登入',
      admin: '管理員',
      user: '普通使用者',
      language: '語言',
      back: '返回',
      save: '儲存',
      cancel: '取消',
      submit: '提交',
      send: '發送',
      readMore: '閱讀全文',
      noSummary: '暫無摘要',
      noData: '暫無資料',
      backHome: '返回部落格首頁',
      copyright: '© {year} AyakaのBlog. 保留所有權利。',
      languages: {
        'zh-CN': '簡中',
        'zh-TW': '繁中',
        en: 'EN'
      }
    },
    theme: {
      system: '系統主題',
      dark: '深色主題',
      light: '淺色主題'
    },
    topHeader: {
      home: '首頁',
      tags: '標籤',
      categories: '分類',
      about: '關於',
      music: '音樂',
      backToTop: '返回頂部',
      backToTopHint: '回到頂部'
    },
    userMenu: {
      enterAdmin: '進入後台管理',
      enterWorkspace: '進入工作台',
      login: '登入',
      logout: '退出登入',
      confirmLogout: '確定要退出登入嗎？',
      logoutTitle: '退出登入',
      logoutSuccess: '已退出登入'
    },
    home: {
      emptyTitle: '暫無文章',
      emptyDesc: '管理員快去後台發布第一篇文章吧',
      views: '{count} 次瀏覽',
      author: '創作者',
      authorBio: 'Hi friend 👋 這裡是 AyakaのBlog',
      hotTags: '熱門標籤',
      noTags: '暫無標籤',
      friendLinks: '友情連結',
      noLinks: '暫無友鏈'
    },
    login: {
      title: 'AyakaのBlog',
      desc: '登入後即可進入後台與個人中心',
      usernamePlaceholder: '使用者名稱或信箱',
      passwordPlaceholder: '密碼',
      turnstileHint: '登入失敗次數過多時，需要先完成人機驗證。',
      turnstileRequired: '請先完成人機驗證',
      rememberMe: '7 天內免登入',
      submit: '登入',
      submitting: '登入中...',
      register: '註冊帳號',
      forgotPassword: '忘記密碼',
      fillCredentials: '請填寫使用者名稱和密碼',
      success: '登入成功',
      rememberSuccess: '登入成功，7 天內免登入'
    },
    register: {
      title: 'AyakaのBlog',
      desc: '建立你的站內帳號，完成信箱驗證與人機驗證後即可開始使用。',
      usernamePlaceholder: '使用者名稱',
      usernameTip: '使用者名稱長度需為 {min}-{max} 位',
      emailPlaceholder: '信箱地址',
      sendCode: '發送驗證碼',
      emailCodePlaceholder: '信箱驗證碼',
      passwordPlaceholder: '密碼',
      confirmPasswordPlaceholder: '確認密碼',
      turnstileHint: '發送註冊信箱驗證碼前，請先完成人機驗證。',
      captchaPlaceholder: '圖形驗證碼',
      captchaHint: '輸入信箱後取得',
      submit: '註冊',
      backLogin: '返回登入',
      messages: {
        confirmPasswordRequired: '請確認密碼',
        confirmPasswordMismatch: '兩次密碼輸入不一致',
        usernameRequired: '請輸入使用者名稱',
        usernameLength: '使用者名稱長度為 {min}-{max} 位',
        emailRequired: '請輸入信箱',
        emailInvalid: '信箱格式不正確',
        emailCodeRequired: '請輸入信箱驗證碼',
        emailCodeLength: '驗證碼為 6 位',
        turnstileRequired: '請先完成人機驗證',
        captchaRequired: '請輸入圖形驗證碼',
        captchaLength: '圖形驗證碼為 4 位',
        invalidEmailFirst: '請先輸入有效信箱',
        captchaLoadFailed: '取得圖形驗證碼失敗',
        codeSending: '驗證碼發送中，請留意信箱',
        codeSendFailed: '發送信箱驗證碼失敗',
        success: '註冊成功，請登入',
        failed: '註冊失敗'
      }
    },
    passwordRules: {
      unsupported: '密碼僅支援英文、數字和常見符號，不支援中文或表情。',
      ruleMessage: '密碼需為 {min}-{max} 位，僅支援英文、數字和常見符號，且字母、數字、特殊字元至少滿足兩類。',
      charset: '僅支援英文、數字和常見符號',
      length: '長度 {min}-{max} 位',
      letter: '包含字母',
      digit: '包含數字',
      special: '包含特殊字元',
      categories: '以上三類中至少滿足兩類',
      required: '請輸入密碼'
    },
    forgotPassword: {
      title: '重設密碼',
      desc: '按步驟完成信箱驗證後重設密碼',
      emailPlaceholder: '請輸入註冊信箱',
      sendCode: '發送驗證碼',
      codePlaceholder: '輸入 6 位信箱驗證碼',
      verifyCode: '驗證驗證碼',
      resendCode: '重新發送驗證碼',
      resendCountdown: '{seconds}s 後可重發',
      changeEmail: '修改信箱',
      newPasswordPlaceholder: '新密碼',
      confirmNewPasswordPlaceholder: '確認新密碼',
      resetPassword: '重設密碼',
      successTitle: '密碼已重設',
      successDesc: '現在可以使用新密碼重新登入。',
      backLogin: '返回登入',
      messages: {
        invalidEmail: '請輸入有效信箱',
        codeSending: '驗證碼發送中，請留意信箱',
        codeSendFailed: '發送驗證碼失敗',
        codeRequired: '請輸入 6 位驗證碼',
        codeVerified: '驗證碼校驗通過',
        codeInvalid: '驗證碼錯誤',
        passwordIncomplete: '請完整填寫新密碼',
        passwordMismatch: '兩次密碼輸入不一致',
        resetSuccess: '密碼重設成功',
        resetFailed: '密碼重設失敗'
      }
    },
    about: {
      tabs: {
        contact: '聯絡我們',
        sites: '我的關聯站點',
        links: '友情連結'
      },
      contact: {
        title: '聯絡我們',
        desc: '如果你對文章有想法、對網站建設有問題，或想討論技術與創意的任何點子，都歡迎透過下面的方式與我聯絡。我會盡量在空閒時回覆。',
        name: '姓名',
        namePlaceholder: '請輸入你的名字',
        email: '信箱',
        emailPlaceholder: '請輸入你的信箱',
        subject: '主題',
        subjectPlaceholder: '請輸入郵件主題',
        message: '內容',
        messagePlaceholder: '請輸入你的訊息',
        submit: '發送訊息',
        otherMethods: '其他聯絡方式',
        success: '訊息已發送',
        failed: '訊息發送失敗'
      },
      sites: {
        title: '我的關聯站點',
        desc: '這些是我營運的其他網站和服務，歡迎造訪。',
        visit: '造訪 ->',
        ayakaCloud: '個人網盤，分享檔案和資源',
        bilibili: '分享動漫影片和精彩片段',
        music: '精選音樂合集',
        knowledge: '技術文件和筆記'
      },
      links: {
        title: '友情連結',
        desc: '這裡彙集了我所信賴、欣賞並持續獲得靈感的數位世界「地圖」。',
        techPartners: '技術夥伴',
        empty: '暫無友情連結',
        submitTitle: '提交友情連結',
        submitDesc: '如果你也想與我交換友情連結，歡迎填寫下面的表單。',
        siteName: '網站名稱',
        siteNamePlaceholder: '你的網站名稱',
        siteUrl: '網站 URL',
        siteDesc: '網站描述',
        siteDescPlaceholder: '簡短描述你的網站',
        contactEmail: '聯絡信箱',
        contactEmailPlaceholder: '你的信箱',
        submit: '提交申請',
        success: '友鏈申請已提交',
        failed: '友鏈申請提交失敗'
      },
      rules: {
        nameRequired: '請輸入姓名',
        emailRequired: '請輸入信箱',
        emailInvalid: '信箱格式不正確',
        subjectRequired: '請輸入主題',
        messageRequired: '請輸入內容',
        siteNameRequired: '請輸入網站名稱',
        siteUrlRequired: '請輸入網站 URL',
        urlInvalid: 'URL 格式不正確',
        siteDescRequired: '請輸入網站描述'
      }
    },
    tags: {
      title: '標籤',
      desc: '點擊標籤查看相關文章，快速找到你感興趣的內容。',
      emptyTitle: '暫無標籤',
      emptyDesc: '文章發布後標籤會自動顯示',
      backToList: '返回標籤列表',
      noArticles: '該標籤下暫無文章',
      articleCount: '{count} 篇文章'
    },
    categories: {
      title: '分類',
      desc: '點擊分類卡片查看相關文章，探索不同領域的內容。',
      emptyTitle: '暫無分類',
      emptyDesc: '文章發布後分類會自動顯示',
      backToList: '返回分類列表',
      noArticles: '該分類下暫無文章',
      articleCount: '{count} 篇文章'
    },
    modules: {
      title: '功能模組',
      linksTitle: '友情連結',
      noFriends: '暫無友情連結',
      mainSite: '主站',
      mainSiteDesc: '部落格首頁，瀏覽最新文章和動態',
      drive: '網盤',
      driveDesc: '資源共享中心，各類資源下載',
      more: '更多功能',
      moreDesc: '持續更新中，敬請期待...',
      developing: '功能開發中...'
    },
    postDetail: {
      back: '返回',
      notFound: '文章不存在或未發布'
    },
    admin: {
      nav: {
        dashboard: '控制台',
        posts: '文章管理',
        reviews: '文章審核',
        music: '音樂管理',
        users: '用戶管理',
        links: '友鏈管理',
        contacts: '聯絡訊息',
        profile: '個人資訊',
        workspace: '工作台',
        myPosts: '我的文章'
      },
      topbar: {
        front: '返回前台',
        logout: '退出登入'
      }
    }
  },
  en: {
    common: {
      appName: 'Ayaka Blog',
      loading: 'Loading...',
      guest: 'Guest',
      loggedIn: 'Signed in',
      admin: 'Admin',
      user: 'User',
      language: 'Language',
      back: 'Back',
      save: 'Save',
      cancel: 'Cancel',
      submit: 'Submit',
      send: 'Send',
      readMore: 'Read more',
      noSummary: 'No summary yet',
      noData: 'No data',
      backHome: 'Back to blog home',
      copyright: '© {year} Ayaka Blog. All rights reserved.',
      languages: {
        'zh-CN': 'SC',
        'zh-TW': 'TC',
        en: 'EN'
      }
    },
    theme: {
      system: 'System theme',
      dark: 'Dark theme',
      light: 'Light theme'
    },
    topHeader: {
      home: 'Home',
      tags: 'Tags',
      categories: 'Categories',
      about: 'About',
      music: 'Music',
      backToTop: 'Back to top',
      backToTopHint: 'Back to top'
    },
    userMenu: {
      enterAdmin: 'Open admin panel',
      enterWorkspace: 'Open workspace',
      login: 'Sign in',
      logout: 'Sign out',
      confirmLogout: 'Are you sure you want to sign out?',
      logoutTitle: 'Sign out',
      logoutSuccess: 'Signed out'
    },
    home: {
      emptyTitle: 'No posts yet',
      emptyDesc: 'Publish the first post from the admin panel.',
      views: '{count} views',
      author: 'Author',
      authorBio: 'Hi friend 👋 welcome to Ayaka Blog',
      hotTags: 'Popular tags',
      noTags: 'No tags yet',
      friendLinks: 'Friend links',
      noLinks: 'No links yet'
    },
    login: {
      title: 'Ayaka Blog',
      desc: 'Sign in to access the admin panel and your workspace.',
      usernamePlaceholder: 'Username or email',
      passwordPlaceholder: 'Password',
      turnstileHint: 'Human verification is only required after repeated failed sign-in attempts.',
      turnstileRequired: 'Please complete the human verification first',
      rememberMe: 'Keep me signed in for 7 days',
      submit: 'Sign in',
      submitting: 'Signing in...',
      register: 'Create account',
      forgotPassword: 'Forgot password',
      fillCredentials: 'Please enter your username and password',
      success: 'Signed in successfully',
      rememberSuccess: 'Signed in successfully for 7 days'
    },
    register: {
      title: 'Ayaka Blog',
      desc: 'Create your account and finish email plus human verification to get started.',
      usernamePlaceholder: 'Username',
      usernameTip: 'Username length must be {min}-{max} characters',
      emailPlaceholder: 'Email address',
      sendCode: 'Send code',
      emailCodePlaceholder: 'Email verification code',
      passwordPlaceholder: 'Password',
      confirmPasswordPlaceholder: 'Confirm password',
      turnstileHint: 'Complete the human verification before sending the registration email code.',
      captchaPlaceholder: 'Captcha',
      captchaHint: 'Enter your email first',
      submit: 'Register',
      backLogin: 'Back to sign in',
      messages: {
        confirmPasswordRequired: 'Please confirm your password',
        confirmPasswordMismatch: 'Passwords do not match',
        usernameRequired: 'Please enter a username',
        usernameLength: 'Username must be {min}-{max} characters',
        emailRequired: 'Please enter an email address',
        emailInvalid: 'Invalid email address',
        emailCodeRequired: 'Please enter the email code',
        emailCodeLength: 'The code must be 6 digits',
        turnstileRequired: 'Please complete the human verification first',
        captchaRequired: 'Please enter the captcha',
        captchaLength: 'The captcha must be 4 characters',
        invalidEmailFirst: 'Enter a valid email address first',
        captchaLoadFailed: 'Failed to load captcha',
        codeSending: 'Sending code, please check your inbox',
        codeSendFailed: 'Failed to send the email code',
        success: 'Registration successful. Please sign in.',
        failed: 'Registration failed'
      }
    },
    passwordRules: {
      unsupported: 'Only English letters, numbers, and common symbols are supported. Chinese characters and emoji are not allowed.',
      ruleMessage: 'Password must be {min}-{max} characters and include at least two of: letters, numbers, and symbols.',
      charset: 'Only letters, numbers, and common symbols are allowed',
      length: 'Length {min}-{max}',
      letter: 'Contains letters',
      digit: 'Contains numbers',
      special: 'Contains symbols',
      categories: 'At least two of the three categories above',
      required: 'Please enter a password'
    },
    forgotPassword: {
      title: 'Reset password',
      desc: 'Verify your email step by step, then reset your password.',
      emailPlaceholder: 'Enter your registered email',
      sendCode: 'Send code',
      codePlaceholder: 'Enter the 6-digit email code',
      verifyCode: 'Verify code',
      resendCode: 'Resend code',
      resendCountdown: 'Resend in {seconds}s',
      changeEmail: 'Change email',
      newPasswordPlaceholder: 'New password',
      confirmNewPasswordPlaceholder: 'Confirm new password',
      resetPassword: 'Reset password',
      successTitle: 'Password reset complete',
      successDesc: 'You can now sign in with your new password.',
      backLogin: 'Back to sign in',
      messages: {
        invalidEmail: 'Please enter a valid email address',
        codeSending: 'Sending code, please check your inbox',
        codeSendFailed: 'Failed to send the code',
        codeRequired: 'Please enter the 6-digit code',
        codeVerified: 'Code verified',
        codeInvalid: 'Invalid verification code',
        passwordIncomplete: 'Please complete the new password fields',
        passwordMismatch: 'Passwords do not match',
        resetSuccess: 'Password reset successfully',
        resetFailed: 'Failed to reset password'
      }
    },
    about: {
      tabs: {
        contact: 'Contact',
        sites: 'My sites',
        links: 'Friend links'
      },
      contact: {
        title: 'Contact us',
        desc: 'If you have thoughts about the posts, questions about the site, or ideas you want to discuss, feel free to reach out using the form below. I will reply when I can.',
        name: 'Name',
        namePlaceholder: 'Your name',
        email: 'Email',
        emailPlaceholder: 'Your email',
        subject: 'Subject',
        subjectPlaceholder: 'Subject',
        message: 'Message',
        messagePlaceholder: 'Your message',
        submit: 'Send message',
        otherMethods: 'Other ways to reach me',
        success: 'Message sent',
        failed: 'Failed to send the message'
      },
      sites: {
        title: 'My sites',
        desc: 'These are the other websites and services I run.',
        visit: 'Visit ->',
        ayakaCloud: 'Personal cloud drive for files and resources',
        bilibili: 'Anime videos and highlights',
        music: 'A curated music collection',
        knowledge: 'Technical docs and notes'
      },
      links: {
        title: 'Friend links',
        desc: 'A small map of the digital places I trust, admire, and keep learning from.',
        techPartners: 'Tech partners',
        empty: 'No friend links yet',
        submitTitle: 'Submit a friend link',
        submitDesc: 'Want to exchange links with me? Fill out the form below.',
        siteName: 'Site name',
        siteNamePlaceholder: 'Your site name',
        siteUrl: 'Site URL',
        siteDesc: 'Site description',
        siteDescPlaceholder: 'A short description of your site',
        contactEmail: 'Contact email',
        contactEmailPlaceholder: 'Your email',
        submit: 'Submit request',
        success: 'Friend link request submitted',
        failed: 'Failed to submit the request'
      },
      rules: {
        nameRequired: 'Please enter your name',
        emailRequired: 'Please enter your email',
        emailInvalid: 'Invalid email address',
        subjectRequired: 'Please enter a subject',
        messageRequired: 'Please enter a message',
        siteNameRequired: 'Please enter the site name',
        siteUrlRequired: 'Please enter the site URL',
        urlInvalid: 'Invalid URL',
        siteDescRequired: 'Please enter a site description'
      }
    },
    tags: {
      title: 'Tags',
      desc: 'Click a tag to see related posts and quickly find what interests you.',
      emptyTitle: 'No tags yet',
      emptyDesc: 'Tags will appear automatically after posts are published.',
      backToList: 'Back to tag list',
      noArticles: 'No posts under this tag',
      articleCount: '{count} posts'
    },
    categories: {
      title: 'Categories',
      desc: 'Click a category card to explore related posts.',
      emptyTitle: 'No categories yet',
      emptyDesc: 'Categories will appear automatically after posts are published.',
      backToList: 'Back to category list',
      noArticles: 'No posts in this category',
      articleCount: '{count} posts'
    },
    modules: {
      title: 'Modules',
      linksTitle: 'Friend links',
      noFriends: 'No friend links yet',
      mainSite: 'Main site',
      mainSiteDesc: 'Blog homepage with the latest posts and updates',
      drive: 'Cloud drive',
      driveDesc: 'Resource hub for downloads and sharing',
      more: 'More features',
      moreDesc: 'More features are on the way...',
      developing: 'This feature is under development...'
    },
    postDetail: {
      back: 'Back',
      notFound: 'This post does not exist or is not published yet'
    },
    admin: {
      nav: {
        dashboard: 'Dashboard',
        posts: 'Posts',
        reviews: 'Reviews',
        music: 'Music',
        users: 'Users',
        links: 'Links',
        contacts: 'Messages',
        profile: 'Profile',
        workspace: 'Workspace',
        myPosts: 'My posts'
      },
      topbar: {
        front: 'Back to site',
        logout: 'Sign out'
      }
    }
  }
}
