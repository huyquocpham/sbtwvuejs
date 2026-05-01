<template>
  <div id="app">
    <h1>Product Manager</h1>
    <ProductManager />
  </div>
</template>

<script setup>
import ProductManager from './components/ProductManager.vue'
</script>

<style>
#app {
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  color: #333;
  margin-bottom: 20px;
}
</style>
