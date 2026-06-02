// Customer id is set after onboarding via UI; the API stores it on the JWT as 'customerId' claim,
// but for simplicity here we cache it in localStorage. Real apps would decode the JWT.
const KEY = 'bank.customerId'
export const getCustomerId = () => localStorage.getItem(KEY) || ''
export const setCustomerId = (id) => (id ? localStorage.setItem(KEY, id) : localStorage.removeItem(KEY))
