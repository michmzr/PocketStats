<template>
  <div class="container text-center">
    <div class="col">
      <nav class="navbar navbar-expand-lg navbar-light bg-light">
        <a class="navbar-brand" href="#">PocketStats</a>

        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav">
            <li class="nav-item">
              <router-link to="/" class="nav-link">Home</router-link>
            </li>
          </ul>
        </div>

        <TopAuth></TopAuth>
      </nav>
    </div>
  </div>
  <main role="main" class="container">
    <div class="container text-center">
      <router-view/>
    </div>
  </main>
</template>

<script>
import {defineComponent} from "vue";
import TopAuth from "@/components/TopAuth.vue";

export default defineComponent({
  components: {
    TopAuth
  },
});
</script>
