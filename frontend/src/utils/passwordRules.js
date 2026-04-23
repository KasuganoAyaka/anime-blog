export const USERNAME_MIN_LENGTH = 3
export const USERNAME_MAX_LENGTH = 12
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 20
export const PASSWORD_ALLOWED_PATTERN = /^[A-Za-z\d!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~]*$/
export const PASSWORD_ALLOWED_CHAR_PATTERN = /[A-Za-z\d!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~]/

const getTranslator = (translator) => (
  typeof translator === 'function'
    ? translator
    : (key, params = {}) => {
        if (key === 'passwordRules.required') return '请输入密码'
        if (key === 'passwordRules.unsupported') return '密码仅支持英文、数字和常见符号，不支持中文或表情'
        if (key === 'passwordRules.ruleMessage') return `密码需为 ${params.min}-${params.max} 位，仅支持英文、数字和常见符号，且字母/数字/特殊字符至少满足两类`
        if (key === 'passwordRules.charset') return '仅支持英文、数字和常见符号，不支持中文或表情'
        if (key === 'passwordRules.length') return `长度 ${params.min}-${params.max} 位`
        if (key === 'passwordRules.letter') return '包含字母'
        if (key === 'passwordRules.digit') return '包含数字'
        if (key === 'passwordRules.special') return '包含特殊字符'
        if (key === 'passwordRules.categories') return '以上三类中至少满足两类'
        return key
      }
)

export const sanitizePasswordInput = (value = '') => Array.from(String(value))
  .filter(char => PASSWORD_ALLOWED_CHAR_PATTERN.test(char))
  .join('')

export const evaluatePasswordRules = (value = '') => {
  const hasLetter = /[A-Za-z]/.test(value)
  const hasDigit = /\d/.test(value)
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(value)
  const categoryCount = [hasLetter, hasDigit, hasSpecial].filter(Boolean).length
  const hasAllowedCharsOnly = PASSWORD_ALLOWED_PATTERN.test(value)

  return {
    hasLetter,
    hasDigit,
    hasSpecial,
    hasAllowedCharsOnly,
    hasValidLength: value.length >= PASSWORD_MIN_LENGTH && value.length <= PASSWORD_MAX_LENGTH,
    hasEnoughCategories: categoryCount >= 2,
    isValid: hasAllowedCharsOnly && value.length >= PASSWORD_MIN_LENGTH && value.length <= PASSWORD_MAX_LENGTH && categoryCount >= 2
  }
}

export const getPasswordRuleItems = (value = '', translator) => {
  const t = getTranslator(translator)
  const state = evaluatePasswordRules(value)

  return [
    { key: 'charset', label: t('passwordRules.charset'), met: state.hasAllowedCharsOnly },
    { key: 'length', label: t('passwordRules.length', { min: PASSWORD_MIN_LENGTH, max: PASSWORD_MAX_LENGTH }), met: state.hasValidLength },
    { key: 'letter', label: t('passwordRules.letter'), met: state.hasLetter },
    { key: 'digit', label: t('passwordRules.digit'), met: state.hasDigit },
    { key: 'special', label: t('passwordRules.special'), met: state.hasSpecial },
    { key: 'categories', label: t('passwordRules.categories'), met: state.hasEnoughCategories }
  ]
}

export const getPasswordValidationMessage = (value = '', translator) => {
  const t = getTranslator(translator)

  if (!value) {
    return t('passwordRules.required')
  }

  const state = evaluatePasswordRules(value)
  if (!state.hasAllowedCharsOnly) {
    return t('passwordRules.unsupported')
  }

  if (!state.isValid) {
    return t('passwordRules.ruleMessage', { min: PASSWORD_MIN_LENGTH, max: PASSWORD_MAX_LENGTH })
  }

  return ''
}
