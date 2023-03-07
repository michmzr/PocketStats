<template>
  <div class="home">
    <HomeAuth/>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import TopAuth from "@/components/TopAuth.vue";

@Options({
  components: {
    HomeAuth: TopAuth
  },
})
export default class HomeView extends Vue {}
</script>
