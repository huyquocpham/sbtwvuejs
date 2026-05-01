<template>
  <div class="product-manager">
    <!-- Login Form -->
    <div v-if="!isAuthenticated" class="login-section">
      <h2>Login Required</h2>
      <form @submit.prevent="login">
        <div class="form-group">
          <label for="username">Username:</label>
          <input 
            id="username" 
            v-model="loginForm.username" 
            type="text" 
            required 
            placeholder="Enter username"
          />
        </div>
        
        <div class="form-group">
          <label for="password">Password:</label>
          <input 
            id="password" 
            v-model="loginForm.password" 
            type="password" 
            required 
            placeholder="Enter password"
          />
        </div>
        
        <div class="form-buttons">
          <button type="submit" class="btn-primary">Login</button>
        </div>
        
        <p v-if="loginError" class="error">{{ loginError }}</p>
      </form>
      
      <p class="register-hint">
        Default users: admin/admin123 or user/user123
      </p>
    </div>
    
    <!-- Product Manager (Authenticated) -->
    <div v-else class="authenticated-content">
      <div class="header">
        <h2>Welcome, {{ currentUser }}!</h2>
        <button class="btn-logout" @click="logout">Logout</button>
      </div>
      
      <!-- Form for Add/Edit Product -->
      <div class="form-section">
        <h3>{{ isEditing ? 'Edit Product' : 'Add New Product' }}</h3>
        <form @submit.prevent="submitForm">
          <div class="form-group">
            <label for="name">Name:</label>
            <input 
              id="name" 
              v-model="form.name" 
              type="text" 
              required 
              placeholder="Enter product name"
            />
          </div>
          
          <div class="form-group">
            <label for="price">Price:</label>
            <input 
              id="price" 
              v-model.number="form.price" 
              type="number" 
              step="0.01" 
              required 
              placeholder="Enter product price"
            />
          </div>
          
          <div class="form-group">
            <label for="description">Description:</label>
            <textarea 
              id="description" 
              v-model="form.description" 
              required 
              placeholder="Enter product description"
              rows="3"
            ></textarea>
          </div>
          
          <div class="form-buttons">
            <button type="submit" class="btn-primary">
              {{ isEditing ? 'Update' : 'Add Product' }}
            </button>
            <button 
              v-if="isEditing" 
              type="button" 
              class="btn-secondary" 
              @click="cancelEdit"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
      
      <!-- Product List -->
      <div class="list-section">
        <h3>Products List</h3>
        <div v-if="loading" class="loading">Loading...</div>
        <div v-else-if="error" class="error">{{ error }}</div>
        <table v-else class="product-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Price</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in products" :key="product.id">
              <td>{{ product.id }}</td>
              <td>{{ product.name }}</td>
              <td>${{ product.price?.toFixed(2) }}</td>
              <td>{{ product.description }}</td>
              <td class="actions">
                <button class="btn-edit" @click="editProduct(product)">Edit</button>
                <button class="btn-delete" @click="deleteProduct(product.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!loading && products.length === 0" class="no-products">
          No products found. Add your first product!
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const API_URL = 'http://localhost:8080/api'
const AUTH_URL = `${API_URL}/auth`

// State
const isAuthenticated = ref(false)
const currentUser = ref('')
const token = ref(localStorage.getItem('token') || '')
const products = ref([])
const loading = ref(false)
const error = ref('')
const loginError = ref('')
const isEditing = ref(false)
const editingId = ref(null)

const loginForm = ref({
  username: '',
  password: ''
})

const form = ref({
  name: '',
  price: null,
  description: ''
})

// Check for existing token on mount
onMounted(() => {
  if (token.value) {
    isAuthenticated.value = true
    currentUser.value = localStorage.getItem('username') || 'User'
    fetchProducts()
  }
})

// Login
const login = async () => {
  loginError.value = ''
  try {
    const response = await axios.post(`${AUTH_URL}/login`, {
      username: loginForm.value.username,
      password: loginForm.value.password
    })
    
    token.value = response.data.token
    currentUser.value = response.data.username
    
    // Save to localStorage
    localStorage.setItem('token', token.value)
    localStorage.setItem('username', currentUser.value)
    
    isAuthenticated.value = true
    loginForm.value = { username: '', password: '' }
    
    // Fetch products after login
    fetchProducts()
  } catch (err) {
    loginError.value = 'Invalid username or password'
    console.error('Login error:', err)
  }
}

// Logout
const logout = () => {
  token.value = ''
  currentUser.value = ''
  isAuthenticated.value = false
  products.value = []
  localStorage.removeItem('token')
  localStorage.removeItem('username')
}

// Get auth headers
const getAuthHeaders = () => ({
  'Authorization': `Bearer ${token.value}`,
  'Content-Type': 'application/json'
})

// Fetch all products (GET)
const fetchProducts = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get(`${API_URL}/products`, {
      headers: getAuthHeaders()
    })
    products.value = response.data
  } catch (err) {
    if (err.response?.status === 401) {
      logout()
      error.value = 'Session expired. Please login again.'
    } else {
      error.value = 'Failed to fetch products: ' + (err.response?.data?.message || err.message)
    }
    console.error('Error fetching products:', err)
  } finally {
    loading.value = false
  }
}

// Submit form (INSERT or UPDATE)
const submitForm = async () => {
  error.value = ''
  try {
    if (isEditing.value) {
      // UPDATE - PUT request
      await axios.put(`${API_URL}/products/${editingId.value}`, form.value, {
        headers: getAuthHeaders()
      })
    } else {
      // INSERT - POST request
      await axios.post(`${API_URL}/products`, form.value, {
        headers: getAuthHeaders()
      })
    }
    
    // Reset form and refresh list
    resetForm()
    await fetchProducts()
  } catch (err) {
    if (err.response?.status === 401) {
      logout()
      error.value = 'Session expired. Please login again.'
    } else {
      error.value = 'Failed to save product: ' + (err.response?.data?.message || err.message)
    }
    console.error('Error saving product:', err)
  }
}

// Edit product
const editProduct = (product) => {
  isEditing.value = true
  editingId.value = product.id
  form.value = {
    name: product.name,
    price: product.price,
    description: product.description
  }
}

// Cancel edit
const cancelEdit = () => {
  resetForm()
}

// Delete product
const deleteProduct = async (id) => {
  if (!confirm('Are you sure you want to delete this product?')) {
    return
  }
  
  error.value = ''
  try {
    await axios.delete(`${API_URL}/products/${id}`, {
      headers: getAuthHeaders()
    })
    await fetchProducts()
  } catch (err) {
    if (err.response?.status === 401) {
      logout()
      error.value = 'Session expired. Please login again.'
    } else {
      error.value = 'Failed to delete product: ' + (err.response?.data?.message || err.message)
    }
    console.error('Error deleting product:', err)
  }
}

// Reset form
const resetForm = () => {
  isEditing.value = false
  editingId.value = null
  form.value = {
    name: '',
    price: null,
    description: ''
  }
}
</script>

<style scoped>
.product-manager {
  max-width: 1000px;
  margin: 0 auto;
}

.login-section {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  max-width: 400px;
  margin: 50px auto;
}

.login-section h2,
.form-section h3,
.list-section h3 {
  color: #333;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #555;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.form-buttons {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.btn-primary,
.btn-secondary,
.btn-edit,
.btn-delete,
.btn-logout {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.btn-primary {
  background-color: #4CAF50;
  color: white;
}

.btn-primary:hover {
  background-color: #45a049;
}

.btn-secondary {
  background-color: #9e9e9e;
  color: white;
}

.btn-secondary:hover {
  background-color: #757575;
}

.btn-logout {
  background-color: #f44336;
  color: white;
}

.btn-logout:hover {
  background-color: #d32f2f;
}

.authenticated-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header h2 {
  margin: 0;
  color: #333;
}

.form-section,
.list-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.product-table {
  width: 100%;
  border-collapse: collapse;
}

.product-table th,
.product-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.product-table th {
  background-color: #f5f5f5;
  font-weight: 600;
  color: #333;
}

.product-table tr:hover {
  background-color: #f9f9f9;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-edit {
  background-color: #2196F3;
  color: white;
}

.btn-edit:hover {
  background-color: #1976D2;
}

.btn-delete {
  background-color: #f44336;
  color: white;
}

.btn-delete:hover {
  background-color: #d32f2f;
}

.loading,
.error,
.no-products {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error {
  color: #f44336;
  margin-top: 10px;
}

.login-error {
  color: #f44336;
  margin-top: 10px;
}

.register-hint {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-top: 15px;
}
</style>
