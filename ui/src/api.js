import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

const TOKEN_KEY = 'bank.token'

export function setToken(t) {
  if (t) localStorage.setItem(TOKEN_KEY, t)
  else localStorage.removeItem(TOKEN_KEY)
}
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

api.interceptors.request.use((cfg) => {
  const tok = getToken()
  if (tok) cfg.headers.Authorization = `Bearer ${tok}`
  return cfg
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      setToken(null)
      window.location.assign('/login')
    }
    return Promise.reject(err)
  },
)

export const auth = {
  login: (body) => api.post('/auth/login', body).then((r) => r.data.data),
  register: (body) => api.post('/auth/register', body).then((r) => r.data),
}

export const customers = {
  create: (body) => api.post('/customers', body).then((r) => r.data.data),
  get: (id) => api.get(`/customers/${id}`).then((r) => r.data.data),
  setKyc: (id, status) =>
    api.post(`/customers/${id}/kyc/${status}`).then((r) => r.data.data),
}

export const accounts = {
  open: (body) => api.post('/accounts', body).then((r) => r.data.data),
  byCustomer: (customerId) =>
    api.get(`/accounts/customer/${customerId}`).then((r) => r.data.data),
  get: (id) => api.get(`/accounts/${id}`).then((r) => r.data.data),
  deposit: (id, amount, reason) =>
    api.post(`/accounts/${id}/deposit`, { amount, reason }).then((r) => r.data.data),
  withdraw: (id, amount, reason) =>
    api.post(`/accounts/${id}/withdraw`, { amount, reason }).then((r) => r.data.data),
}

export const workflows = {
  startLoan: (body) => api.post('/workflows/loans', body).then((r) => r.data.data),
  listTasks: () => api.get('/workflows/tasks').then((r) => r.data.data),
  claim: (taskId) => api.post(`/workflows/tasks/${taskId}/claim`).then((r) => r.data),
  complete: (taskId, variables) =>
    api.post(`/workflows/tasks/${taskId}/complete`, { variables }).then((r) => r.data),
}

export const transfers = {
  initiate: (body, idempotencyKey) =>
    api
      .post('/transfers', body, { headers: { 'Idempotency-Key': idempotencyKey } })
      .then((r) => r.data.data),
  get: (id) => api.get(`/transfers/${id}`).then((r) => r.data.data),
}
