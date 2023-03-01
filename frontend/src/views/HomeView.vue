<template>
  <div class="home">
    <HomeAuth/>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import HomeAuth from "@/components/HomeAuth.vue";

@Options({
  components: {
    HomeAuth
  },
})
export default class HomeView extends Vue {}
</script>
