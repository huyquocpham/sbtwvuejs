<template>
  <div class="product-manager">
    <!-- Form for Add/Edit Product -->
    <div class="form-section">
      <h2>{{ isEditing ? 'Edit Product' : 'Add New Product' }}</h2>
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
      <h2>Products List</h2>
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const API_URL = 'http://localhost:8080/api/products'

// State
const products = ref([])
const loading = ref(false)
const error = ref('')
const isEditing = ref(false)
const editingId = ref(null)

const form = ref({
  name: '',
  price: null,
  description: ''
})

// Fetch all products (GET)
const fetchProducts = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get(API_URL)
    products.value = response.data
  } catch (err) {
    error.value = 'Failed to fetch products: ' + (err.response?.data?.message || err.message)
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
      await axios.put(`${API_URL}/${editingId.value}`, form.value)
    } else {
      // INSERT - POST request
      await axios.post(API_URL, form.value)
    }
    
    // Reset form and refresh list
    resetForm()
    await fetchProducts()
  } catch (err) {
    error.value = 'Failed to save product: ' + (err.response?.data?.message || err.message)
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
    await axios.delete(`${API_URL}/${id}`)
    await fetchProducts()
  } catch (err) {
    error.value = 'Failed to delete product: ' + (err.response?.data?.message || err.message)
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

// Load products on component mount
onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.product-manager {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 30px;
}

.form-section,
.list-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

h2 {
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
}

.btn-primary,
.btn-secondary,
.btn-edit,
.btn-delete {
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
}

@media (max-width: 768px) {
  .product-manager {
    grid-template-columns: 1fr;
  }
}
</style>
